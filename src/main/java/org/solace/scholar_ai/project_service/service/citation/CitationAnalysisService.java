package org.solace.scholar_ai.project_service.service.citation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckRequestDto;
import org.solace.scholar_ai.project_service.model.citation.CitationCheck;
import org.solace.scholar_ai.project_service.model.citation.CitationEvidence;
import org.solace.scholar_ai.project_service.model.citation.CitationIssue;
import org.solace.scholar_ai.project_service.model.extraction.ExtractedParagraph;
import org.solace.scholar_ai.project_service.model.extraction.ExtractedReference;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.paper.PaperAuthor;
import org.solace.scholar_ai.project_service.repository.extraction.PaperExtractionRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperAuthorRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.service.ai.GeminiGeneralService;
import org.springframework.stereotype.Service;

/**
 * Service for performing real AI-powered citation analysis on LaTeX content
 * Implements two-stage verification: local papers first, then web sources
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CitationAnalysisService {

    private final GeminiGeneralService geminiGeneralService;
    private final PaperExtractionRepository paperExtractionRepository;
    private final PaperRepository paperRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ObjectMapper objectMapper;

    // Patterns for LaTeX parsing
    private static final Pattern CITE_PATTERN = Pattern.compile("\\\\cite\\{([^}]+)\\}");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+\\s+|$");
    private static final Pattern CLAIM_PATTERN = Pattern.compile(
            "\\b(shows?|demonstrates?|proves?|indicates?|suggests?|confirms?|reveals?|establishes?)\\b");

    /**
     * Analyze LaTeX content for citation issues using selected papers as local context
     */
    public List<CitationIssue> analyzeDocument(
            CitationCheck check, String latexContent, List<String> selectedPaperIds, boolean runWebCheck) {
        // Use default options
        return analyzeDocument(check, latexContent, selectedPaperIds, runWebCheck, null);
    }

    /**
     * Analyze LaTeX content for citation issues using selected papers as local context with configurable options
     */
    public List<CitationIssue> analyzeDocument(
            CitationCheck check,
            String latexContent,
            List<String> selectedPaperIds,
            boolean runWebCheck,
            CitationCheckRequestDto.Options options) {
        log.info(
                "Starting citation analysis for document {} with {} selected papers",
                check.getDocumentId(),
                selectedPaperIds.size());

        // Extract configurable thresholds from options
        double similarityThreshold = 0.85; // Default
        double plagiarismThreshold = 0.92; // Default

        if (options != null) {
            if (options.getSimilarityThreshold() != null) {
                similarityThreshold = options.getSimilarityThreshold();
            }
            if (options.getPlagiarismThreshold() != null) {
                plagiarismThreshold = options.getPlagiarismThreshold();
            }
            log.info(
                    "Using configurable thresholds: similarity={}, plagiarism={}",
                    similarityThreshold,
                    plagiarismThreshold);
        }

        List<CitationIssue> issues = new ArrayList<>();

        try {
            // Step 1: Parse LaTeX content into sentences
            List<LatexSentence> sentences = parseLatexIntoSentences(latexContent);
            log.info("Parsed {} sentences from LaTeX content", sentences.size());

            // Step 2: Load local corpus from selected papers
            Map<String, List<ExtractedParagraph>> localCorpus = loadLocalCorpus(selectedPaperIds);
            Map<String, List<ExtractedReference>> localReferences = loadLocalReferences(selectedPaperIds);
            log.info(
                    "Loaded local corpus: {} papers, {} paragraphs, {} references",
                    localCorpus.size(),
                    localCorpus.values().stream().mapToInt(List::size).sum(),
                    localReferences.values().stream().mapToInt(List::size).sum());

            // Step 3: Analyze each sentence for citation issues
            for (LatexSentence sentence : sentences) {
                if (shouldAnalyzeSentence(sentence)) {
                    issues.addAll(analyzeSentence(check, sentence, localCorpus, localReferences, runWebCheck));
                }
            }

            // Step 4: Comprehensive citation validation
            log.info("Starting comprehensive citation validation...");
            List<CitationIssue> orphanIssues = findOrphanReferences(check, latexContent, sentences);
            log.info("Found {} orphan reference issues", orphanIssues.size());
            issues.addAll(orphanIssues);

            List<CitationIssue> danglingIssues = findDanglingCitations(check, latexContent, sentences);
            log.info("Found {} dangling citation issues", danglingIssues.size());
            issues.addAll(danglingIssues);

            List<CitationIssue> metadataIssues = validateMetadata(check, latexContent, localReferences);
            log.info("Found {} metadata validation issues", metadataIssues.size());
            issues.addAll(metadataIssues);

            List<CitationIssue> plagiarismIssues = detectPotentialPlagiarism(check, sentences, localCorpus);
            log.info("Found {} potential plagiarism issues", plagiarismIssues.size());
            issues.addAll(plagiarismIssues);

            log.info("Citation analysis completed with {} issues found", issues.size());

        } catch (Exception e) {
            log.error("Error during citation analysis", e);
            // Create error issue
            CitationIssue errorIssue = CitationIssue.builder()
                    .citationCheck(check)
                    .projectId(check.getProjectId())
                    .documentId(check.getDocumentId())
                    .type(CitationIssue.IssueType.MISSING_CITATION)
                    .severity(CitationIssue.Severity.LOW)
                    .fromPos(0)
                    .toPos(50)
                    .lineStart(1)
                    .lineEnd(1)
                    .snippet("Error analyzing document: " + e.getMessage())
                    .citedKeys(new String[] {})
                    .suggestions(new ArrayList<>())
                    .build();
            issues.add(errorIssue);
        }

        return issues;
    }

    /**
     * Parse LaTeX content into sentences with position tracking
     */
    private List<LatexSentence> parseLatexIntoSentences(String latexContent) {
        List<LatexSentence> sentences = new ArrayList<>();

        // Split original content into lines to preserve line numbers
        String[] originalLines = latexContent.split("\n");
        int currentPos = 0;

        // Process each line individually to maintain correct line numbers
        for (int lineNumber = 0; lineNumber < originalLines.length; lineNumber++) {
            String originalLine = originalLines[lineNumber];

            // Clean this line individually
            String cleanLine = originalLine
                    .replaceAll("\\\\[a-zA-Z]+\\*?\\{[^}]*\\}", " ") // Remove simple commands
                    .replaceAll("\\\\[a-zA-Z]+\\*?", " ") // Remove commands without braces
                    .replaceAll("\\$[^$]*\\$", " [MATH] ") // Replace inline math
                    .replaceAll("\\$\\$[^$]*\\$\\$", " [DISPLAY_MATH] ") // Replace display math
                    .replaceAll("\\\\\\[[^\\]]*\\\\\\]", " [DISPLAY_MATH] ") // Replace display math
                    .replaceAll("\\%.*", "") // Remove comments
                    .replaceAll("\\s+", " ") // Normalize whitespace
                    .trim();

            if (cleanLine.isEmpty()) {
                currentPos += originalLine.length() + 1; // Account for newline character
                continue;
            }

            // Split line into sentences
            String[] sentenceParts = cleanLine.split("\\. ");
            int linePos = 0;

            for (int i = 0; i < sentenceParts.length; i++) {
                String sentenceText = sentenceParts[i].trim();
                if (sentenceText.length() > 10) { // Minimum sentence length
                    int sentenceStart = currentPos + linePos;
                    int sentenceEnd = sentenceStart + sentenceText.length();

                    // Extract cited keys from ORIGINAL line (before cleaning) to preserve \cite{} commands
                    Set<String> citedKeys = extractCitedKeys(originalLine);

                    LatexSentence sentence = new LatexSentence(
                            sentenceText,
                            sentenceStart,
                            sentenceEnd,
                            lineNumber + 1, // Convert 0-based to 1-based line numbering
                            lineNumber + 1, // For now, assume sentence doesn't span multiple lines
                            citedKeys);
                    sentences.add(sentence);

                    log.debug(
                            "Created sentence at line {}: '{}' with cited keys: {}",
                            lineNumber + 1,
                            sentenceText.substring(0, Math.min(50, sentenceText.length())),
                            citedKeys);
                }
                linePos += sentenceText.length() + 2; // Account for ". " separator
            }

            currentPos += originalLine.length() + 1; // Account for newline character
        }

        log.info(
                "Parsed {} sentences from LaTeX content with line numbers ranging from {} to {}",
                sentences.size(),
                sentences.isEmpty()
                        ? 0
                        : sentences.stream()
                                .mapToInt(LatexSentence::getLineStart)
                                .min()
                                .orElse(0),
                sentences.isEmpty()
                        ? 0
                        : sentences.stream()
                                .mapToInt(LatexSentence::getLineStart)
                                .max()
                                .orElse(0));

        return sentences;
    }

    /**
     * Extract cited keys from a sentence
     */
    private Set<String> extractCitedKeys(String text) {
        Set<String> keys = new HashSet<>();
        Matcher matcher = CITE_PATTERN.matcher(text);
        while (matcher.find()) {
            String citeContent = matcher.group(1);
            // Split multiple keys in same cite command
            Arrays.stream(citeContent.split(",")).map(String::trim).forEach(keys::add);
        }
        return keys;
    }

    /**
     * Load local corpus from selected papers
     */
    private Map<String, List<ExtractedParagraph>> loadLocalCorpus(List<String> selectedPaperIds) {
        Map<String, List<ExtractedParagraph>> corpus = new HashMap<>();

        for (String paperId : selectedPaperIds) {
            try {
                UUID paperUuid = UUID.fromString(paperId);
                List<ExtractedParagraph> paragraphs = Collections.emptyList();

                // Load extraction with sections first
                Optional<PaperExtraction> extraction = paperExtractionRepository.findByPaperIdWithSections(paperUuid);
                if (extraction.isPresent()) {
                    // Get all paragraphs from sections (paragraphs will be lazy loaded)
                    paragraphs = extraction.get().getSections().stream()
                            .flatMap(section -> section.getParagraphs().stream())
                            .collect(Collectors.toList());
                }
                corpus.put(paperId, paragraphs);
                log.debug("Loaded {} paragraphs for paper {}", paragraphs.size(), paperId);
            } catch (Exception e) {
                log.warn("Failed to load paragraphs for paper {}: {}", paperId, e.getMessage());
            }
        }

        return corpus;
    }

    /**
     * Load local references from selected papers
     */
    private Map<String, List<ExtractedReference>> loadLocalReferences(List<String> selectedPaperIds) {
        Map<String, List<ExtractedReference>> references = new HashMap<>();

        for (String paperId : selectedPaperIds) {
            try {
                UUID paperUuid = UUID.fromString(paperId);
                List<ExtractedReference> refs = Collections.emptyList();
                Optional<PaperExtraction> extraction = paperExtractionRepository.findByPaperIdWithReferences(paperUuid);
                if (extraction.isPresent()) {
                    refs = extraction.get().getReferences();
                }
                references.put(paperId, refs);
                log.debug("Loaded {} references for paper {}", refs.size(), paperId);
            } catch (Exception e) {
                log.warn("Failed to load references for paper {}: {}", paperId, e.getMessage());
            }
        }

        return references;
    }

    /**
     * Determine if a sentence should be analyzed for citations
     */
    private boolean shouldAnalyzeSentence(LatexSentence sentence) {
        String text = sentence.getText().toLowerCase();

        // Skip very short sentences
        if (text.length() < 20) return false;

        // Skip sentences that are likely headers or captions
        if (text.startsWith("figure") || text.startsWith("table") || text.startsWith("section")) {
            return false;
        }

        // Focus on sentences with factual claims
        return CLAIM_PATTERN.matcher(text).find()
                || text.contains("research")
                || text.contains("study")
                || text.contains("analysis")
                || text.contains("result")
                || text.contains("finding")
                || text.contains("data")
                || text.contains("method")
                || text.contains("algorithm")
                || text.contains("approach")
                || text.length() > 100; // Long sentences likely contain substantive claims
    }

    /**
     * Analyze a single sentence for citation issues
     */
    private List<CitationIssue> analyzeSentence(
            CitationCheck check,
            LatexSentence sentence,
            Map<String, List<ExtractedParagraph>> localCorpus,
            Map<String, List<ExtractedReference>> localReferences,
            boolean runWebCheck) {

        List<CitationIssue> issues = new ArrayList<>();

        try {
            // Step 1: Local verification against selected papers
            LocalVerificationResult localResult = performLocalVerification(sentence, localCorpus);

            // Step 2: Determine if citation is needed and if current citations are adequate
            boolean needsCitation = needsCitation(sentence, localResult);
            boolean hasAdequateCitation = hasAdequateCitation(sentence, localResult);

            if (needsCitation && !hasAdequateCitation) {
                CitationIssue.IssueType issueType = sentence.getCitedKeys().isEmpty()
                        ? CitationIssue.IssueType.MISSING_CITATION
                        : CitationIssue.IssueType.WEAK_CITATION;

                CitationIssue issue = CitationIssue.builder()
                        .citationCheck(check)
                        .projectId(check.getProjectId())
                        .documentId(check.getDocumentId())
                        .type(issueType)
                        .severity(determineSeverity(localResult))
                        .fromPos(sentence.getStartPos())
                        .toPos(sentence.getEndPos())
                        .lineStart(sentence.getLineStart())
                        .lineEnd(sentence.getLineEnd())
                        .snippet(sentence.getText())
                        .citedKeys(sentence.getCitedKeys().toArray(new String[0]))
                        .suggestions(createSuggestions(localResult))
                        .build();

                log.info(
                        "Created citation issue at line {}: '{}'",
                        sentence.getLineStart(),
                        sentence.getText()
                                .substring(0, Math.min(50, sentence.getText().length())));

                // Add evidence
                List<CitationEvidence> evidence = createEvidence(issue, localResult);
                issue.setEvidence(evidence);

                issues.add(issue);
            }

        } catch (Exception e) {
            log.error("Error analyzing sentence: {}", sentence.getText(), e);
        }

        return issues;
    }

    /**
     * Perform local verification against selected papers using AI
     */
    private LocalVerificationResult performLocalVerification(
            LatexSentence sentence, Map<String, List<ExtractedParagraph>> localCorpus) {

        List<EvidenceCandidate> candidates = new ArrayList<>();

        // Find top matching paragraphs from local corpus
        for (Map.Entry<String, List<ExtractedParagraph>> entry : localCorpus.entrySet()) {
            String paperId = entry.getKey();
            List<ExtractedParagraph> paragraphs = entry.getValue();

            for (ExtractedParagraph paragraph : paragraphs) {
                if (paragraph.getText() != null && paragraph.getText().length() > 50) {
                    double similarity = calculateTextSimilarity(sentence.getText(), paragraph.getText());
                    if (similarity > 0.3) { // Threshold for considering as candidate
                        candidates.add(new EvidenceCandidate(paperId, paragraph, similarity));
                    }
                }
            }
        }

        // Sort by similarity and take top candidates
        candidates.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        List<EvidenceCandidate> topCandidates = candidates.stream().limit(5).collect(Collectors.toList());

        // Use AI to verify each candidate
        List<VerifiedEvidence> verifiedEvidence = new ArrayList<>();
        for (EvidenceCandidate candidate : topCandidates) {
            VerificationDecision decision =
                    verifyWithAI(sentence.getText(), candidate.getParagraph().getText());
            if (decision.getDecision().equals("supports") && decision.getConfidence() > 0.6) {
                verifiedEvidence.add(new VerifiedEvidence(candidate, decision));
            }
        }

        return new LocalVerificationResult(topCandidates, verifiedEvidence);
    }

    /**
     * Use Gemini AI to verify if evidence supports the claim
     */
    private VerificationDecision verifyWithAI(String claim, String evidence) {
        try {
            String prompt = buildVerificationPrompt(claim, evidence);
            String response = geminiGeneralService.generateContent(prompt);
            return parseVerificationResponse(response);
        } catch (Exception e) {
            log.error("Error in AI verification", e);
            return new VerificationDecision("not_enough_info", 0.0, "Error in AI verification");
        }
    }

    /**
     * Build prompt for AI verification
     */
    private String buildVerificationPrompt(String claim, String evidence) {
        return String.format(
                """
            You are a scientific fact-checker. Analyze if the evidence supports the claim.

            CLAIM: "%s"

            EVIDENCE: "%s"

            Respond in JSON format:
            {
              "decision": "supports|contradicts|not_enough_info",
              "confidence": 0.85,
              "rationale": "Brief explanation"
            }

            Rules:
            - Only use "supports" if evidence clearly backs the claim with confidence ≥ 0.66
            - Use "contradicts" if evidence clearly disputes the claim
            - Use "not_enough_info" if evidence is unclear or insufficient
            - Be conservative - prefer "not_enough_info" over weak "supports"
            """,
                claim, evidence);
    }

    /**
     * Parse AI verification response
     */
    private VerificationDecision parseVerificationResponse(String response) {
        try {
            // Extract JSON from response if wrapped in markdown
            String jsonContent = response;
            if (response.contains("```json")) {
                int startIdx = response.indexOf("```json") + 7;
                int endIdx = response.indexOf("```", startIdx);
                if (endIdx > startIdx) {
                    jsonContent = response.substring(startIdx, endIdx).trim();
                }
            } else if (response.contains("```")) {
                int startIdx = response.indexOf("```") + 3;
                int endIdx = response.indexOf("```", startIdx);
                if (endIdx > startIdx) {
                    jsonContent = response.substring(startIdx, endIdx).trim();
                }
            }

            Map<String, Object> parsed =
                    objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

            String decision = (String) parsed.get("decision");
            Double confidence = ((Number) parsed.get("confidence")).doubleValue();
            String rationale = (String) parsed.get("rationale");

            return new VerificationDecision(decision, confidence, rationale);

        } catch (Exception e) {
            log.error("Failed to parse verification response: {}", response, e);
            return new VerificationDecision("not_enough_info", 0.0, "Failed to parse AI response");
        }
    }

    /**
     * Simple text similarity calculation
     */
    private double calculateTextSimilarity(String text1, String text2) {
        // Simple word overlap similarity
        Set<String> words1 = Arrays.stream(text1.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());
        Set<String> words2 = Arrays.stream(text2.toLowerCase().split("\\W+"))
                .filter(w -> w.length() > 3)
                .collect(Collectors.toSet());

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Determine if sentence needs citation based on content and verification result
     */
    private boolean needsCitation(LatexSentence sentence, LocalVerificationResult localResult) {
        String text = sentence.getText().toLowerCase();

        // Needs citation if it makes factual claims
        if (CLAIM_PATTERN.matcher(text).find()) return true;
        if (text.contains("according to") || text.contains("research shows")) return true;
        if (text.contains("study") || text.contains("analysis")) return true;

        // Needs citation if we found supporting evidence but no citation
        return !localResult.getVerifiedEvidence().isEmpty()
                && sentence.getCitedKeys().isEmpty();
    }

    /**
     * Check if sentence has adequate citation
     */
    private boolean hasAdequateCitation(LatexSentence sentence, LocalVerificationResult localResult) {
        // If no citations present, it's not adequate if citation is needed
        if (sentence.getCitedKeys().isEmpty()) return false;

        // If we have verified evidence and citations, it's likely adequate
        return !localResult.getVerifiedEvidence().isEmpty();
    }

    /**
     * Determine severity based on verification result
     */
    private CitationIssue.Severity determineSeverity(LocalVerificationResult localResult) {
        if (localResult.getVerifiedEvidence().isEmpty()) {
            return CitationIssue.Severity.LOW; // No supporting evidence found
        }

        double maxConfidence = localResult.getVerifiedEvidence().stream()
                .mapToDouble(ve -> ve.getDecision().getConfidence())
                .max()
                .orElse(0.0);

        if (maxConfidence > 0.8) return CitationIssue.Severity.HIGH;
        if (maxConfidence > 0.6) return CitationIssue.Severity.MEDIUM;
        return CitationIssue.Severity.LOW;
    }

    /**
     * Create suggestions for citation issues with rich metadata
     */
    private List<Map<String, Object>> createSuggestions(LocalVerificationResult localResult) {
        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (VerifiedEvidence evidence : localResult.getVerifiedEvidence()) {
            try {
                // Get paper details from repository
                Optional<Paper> paperOpt = paperRepository.findById(
                        UUID.fromString(evidence.getCandidate().getPaperId()));
                if (paperOpt.isPresent()) {
                    Paper paper = paperOpt.get();

                    Map<String, Object> suggestion = new HashMap<>();
                    suggestion.put("kind", "local");
                    suggestion.put("score", evidence.getDecision().getConfidence());
                    suggestion.put("paperId", evidence.getCandidate().getPaperId());
                    suggestion.put("title", paper.getTitle());
                    suggestion.put("authors", getAuthorsString(paper));
                    suggestion.put("year", extractYearFromDate(paper.getPublicationDate()));
                    suggestion.put("venue", getVenueString(paper));
                    suggestion.put("doi", paper.getDoi());
                    suggestion.put("url", paper.getPaperUrl());
                    suggestion.put("rationale", evidence.getDecision().getRationale());

                    // Generate BibTeX entry
                    String bibtex = generateBibTexEntry(paper);
                    if (bibtex != null) {
                        suggestion.put("bibtex", bibtex);
                    }

                    // Add citation key suggestion
                    String citationKey = generateCitationKey(paper);
                    suggestion.put("citationKey", citationKey);

                    suggestions.add(suggestion);
                }
            } catch (Exception e) {
                log.warn(
                        "Failed to create suggestion for paper {}: {}",
                        evidence.getCandidate().getPaperId(),
                        e.getMessage());

                // Fallback simple suggestion
                Map<String, Object> suggestion = new HashMap<>();
                suggestion.put("kind", "local");
                suggestion.put("score", evidence.getDecision().getConfidence());
                suggestion.put("paperId", evidence.getCandidate().getPaperId());
                suggestion.put("title", "Paper reference (details unavailable)");
                suggestion.put("rationale", evidence.getDecision().getRationale());
                suggestions.add(suggestion);
            }
        }

        return suggestions;
    }

    /**
     * Create evidence objects for citation issue
     */
    private List<CitationEvidence> createEvidence(CitationIssue issue, LocalVerificationResult localResult) {
        List<CitationEvidence> evidenceList = new ArrayList<>();

        for (VerifiedEvidence verified : localResult.getVerifiedEvidence()) {
            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("kind", "local");
            sourceData.put("paperId", verified.getCandidate().getPaperId());
            sourceData.put("page", verified.getCandidate().getParagraph().getPage());

            CitationEvidence evidence = CitationEvidence.builder()
                    .citationIssue(issue)
                    .source(sourceData)
                    .matchedText(verified.getCandidate().getParagraph().getText())
                    .similarity(verified.getCandidate().getSimilarity())
                    .supportScore(verified.getDecision().getConfidence())
                    .build();
            evidenceList.add(evidence);
        }

        return evidenceList;
    }

    // Inner classes for data structures
    public static class LatexSentence {
        private final String text;
        private final int startPos;
        private final int endPos;
        private final int lineStart;
        private final int lineEnd;
        private final Set<String> citedKeys;

        public LatexSentence(String text, int startPos, int endPos, int lineStart, int lineEnd, Set<String> citedKeys) {
            this.text = text;
            this.startPos = startPos;
            this.endPos = endPos;
            this.lineStart = lineStart;
            this.lineEnd = lineEnd;
            this.citedKeys = citedKeys;
        }

        // Getters
        public String getText() {
            return text;
        }

        public int getStartPos() {
            return startPos;
        }

        public int getEndPos() {
            return endPos;
        }

        public int getLineStart() {
            return lineStart;
        }

        public int getLineEnd() {
            return lineEnd;
        }

        public Set<String> getCitedKeys() {
            return citedKeys;
        }
    }

    public static class EvidenceCandidate {
        private final String paperId;
        private final ExtractedParagraph paragraph;
        private final double similarity;

        public EvidenceCandidate(String paperId, ExtractedParagraph paragraph, double similarity) {
            this.paperId = paperId;
            this.paragraph = paragraph;
            this.similarity = similarity;
        }

        // Getters
        public String getPaperId() {
            return paperId;
        }

        public ExtractedParagraph getParagraph() {
            return paragraph;
        }

        public double getSimilarity() {
            return similarity;
        }
    }

    public static class VerificationDecision {
        private final String decision;
        private final double confidence;
        private final String rationale;

        public VerificationDecision(String decision, double confidence, String rationale) {
            this.decision = decision;
            this.confidence = confidence;
            this.rationale = rationale;
        }

        // Getters
        public String getDecision() {
            return decision;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getRationale() {
            return rationale;
        }
    }

    public static class VerifiedEvidence {
        private final EvidenceCandidate candidate;
        private final VerificationDecision decision;

        public VerifiedEvidence(EvidenceCandidate candidate, VerificationDecision decision) {
            this.candidate = candidate;
            this.decision = decision;
        }

        // Getters
        public EvidenceCandidate getCandidate() {
            return candidate;
        }

        public VerificationDecision getDecision() {
            return decision;
        }
    }

    public static class LocalVerificationResult {
        private final List<EvidenceCandidate> candidates;
        private final List<VerifiedEvidence> verifiedEvidence;

        public LocalVerificationResult(List<EvidenceCandidate> candidates, List<VerifiedEvidence> verifiedEvidence) {
            this.candidates = candidates;
            this.verifiedEvidence = verifiedEvidence;
        }

        // Getters
        public List<EvidenceCandidate> getCandidates() {
            return candidates;
        }

        public List<VerifiedEvidence> getVerifiedEvidence() {
            return verifiedEvidence;
        }
    }

    /**
     * Find orphan references - bibliography entries that are never cited in the text
     */
    private List<CitationIssue> findOrphanReferences(
            CitationCheck check, String latexContent, List<LatexSentence> sentences) {
        List<CitationIssue> issues = new ArrayList<>();

        try {
            // Extract bibliography entries
            Set<String> bibliographyKeys = extractBibliographyKeys(latexContent);
            log.info("Orphan check - Bibliography keys: {}", bibliographyKeys);

            // Extract all cited keys from sentences
            Set<String> citedKeys =
                    sentences.stream().flatMap(s -> s.getCitedKeys().stream()).collect(Collectors.toSet());
            log.info("Orphan check - Cited keys: {}", citedKeys);

            // Find orphan references
            for (String bibKey : bibliographyKeys) {
                if (!citedKeys.contains(bibKey)) {
                    // Find position of bibliography entry
                    int bibPosition = findBibliographyEntryPosition(latexContent, bibKey);
                    int lineNumber = getLineNumberFromPosition(latexContent, bibPosition);

                    CitationIssue issue = CitationIssue.builder()
                            .citationCheck(check)
                            .projectId(check.getProjectId())
                            .documentId(check.getDocumentId())
                            .type(CitationIssue.IssueType.ORPHAN_REFERENCE)
                            .severity(CitationIssue.Severity.MEDIUM)
                            .fromPos(bibPosition)
                            .toPos(bibPosition + bibKey.length())
                            .lineStart(lineNumber)
                            .lineEnd(lineNumber)
                            .snippet("Bibliography entry '" + bibKey + "' is never cited in the text")
                            .citedKeys(new String[] {bibKey})
                            .suggestions(new ArrayList<>())
                            .build();

                    issues.add(issue);
                    log.debug("Found orphan reference: {}", bibKey);
                }
            }

        } catch (Exception e) {
            log.error("Error finding orphan references", e);
        }

        return issues;
    }

    /**
     * Find dangling citations - cite keys that have no corresponding bibliography entry
     */
    private List<CitationIssue> findDanglingCitations(
            CitationCheck check, String latexContent, List<LatexSentence> sentences) {
        List<CitationIssue> issues = new ArrayList<>();

        try {
            Set<String> bibliographyKeys = extractBibliographyKeys(latexContent);
            log.info("Bibliography keys found: {}", bibliographyKeys);

            for (LatexSentence sentence : sentences) {
                for (String citedKey : sentence.getCitedKeys()) {
                    log.info("Checking cited key '{}' against bibliography keys", citedKey);
                    if (!bibliographyKeys.contains(citedKey)) {
                        CitationIssue issue = CitationIssue.builder()
                                .citationCheck(check)
                                .projectId(check.getProjectId())
                                .documentId(check.getDocumentId())
                                .type(CitationIssue.IssueType.MISSING_CITATION) // Could add DANGLING_CITATION type
                                .severity(CitationIssue.Severity.HIGH)
                                .fromPos(sentence.getStartPos())
                                .toPos(sentence.getEndPos())
                                .lineStart(sentence.getLineStart())
                                .lineEnd(sentence.getLineEnd())
                                .snippet("Citation key '" + citedKey + "' has no bibliography entry")
                                .citedKeys(new String[] {citedKey})
                                .suggestions(new ArrayList<>())
                                .build();

                        issues.add(issue);
                        log.debug("Found dangling citation: {}", citedKey);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error finding dangling citations", e);
        }

        return issues;
    }

    /**
     * Validate metadata - check for incorrect years, DOIs, etc.
     */
    private List<CitationIssue> validateMetadata(
            CitationCheck check, String latexContent, Map<String, List<ExtractedReference>> localReferences) {
        List<CitationIssue> issues = new ArrayList<>();

        try {
            Map<String, BibliographyEntry> bibEntries = parseBibliographyEntries(latexContent);

            for (Map.Entry<String, BibliographyEntry> entry : bibEntries.entrySet()) {
                String bibKey = entry.getKey();
                BibliographyEntry bibEntry = entry.getValue();

                // Check against local corpus for metadata validation
                for (List<ExtractedReference> refs : localReferences.values()) {
                    for (ExtractedReference ref : refs) {
                        if (isLikelyMatch(bibEntry, ref)) {
                            List<String> metadataIssues = validateBibliographyMetadata(bibEntry, ref);

                            for (String metadataIssue : metadataIssues) {
                                int bibPosition = findBibliographyEntryPosition(latexContent, bibKey);
                                int lineNumber = getLineNumberFromPosition(latexContent, bibPosition);

                                CitationIssue issue = CitationIssue.builder()
                                        .citationCheck(check)
                                        .projectId(check.getProjectId())
                                        .documentId(check.getDocumentId())
                                        .type(
                                                CitationIssue.IssueType
                                                        .WEAK_CITATION) // Could add INCORRECT_METADATA type
                                        .severity(CitationIssue.Severity.MEDIUM)
                                        .fromPos(bibPosition)
                                        .toPos(bibPosition + bibKey.length())
                                        .lineStart(lineNumber)
                                        .lineEnd(lineNumber)
                                        .snippet("Metadata issue in bibliography entry '" + bibKey + "': "
                                                + metadataIssue)
                                        .citedKeys(new String[] {bibKey})
                                        .suggestions(createMetadataSuggestions(ref))
                                        .build();

                                issues.add(issue);
                                log.debug("Found metadata issue for {}: {}", bibKey, metadataIssue);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error validating metadata", e);
        }

        return issues;
    }

    /**
     * Detect potential plagiarism - high similarity spans without proper citation
     */
    private List<CitationIssue> detectPotentialPlagiarism(
            CitationCheck check, List<LatexSentence> sentences, Map<String, List<ExtractedParagraph>> localCorpus) {
        List<CitationIssue> issues = new ArrayList<>();

        try {
            for (LatexSentence sentence : sentences) {
                for (Map.Entry<String, List<ExtractedParagraph>> entry : localCorpus.entrySet()) {
                    String paperId = entry.getKey();

                    for (ExtractedParagraph paragraph : entry.getValue()) {
                        if (paragraph.getText() != null && paragraph.getText().length() > 30) {
                            double similarity = calculateTextSimilarity(sentence.getText(), paragraph.getText());

                            // High similarity threshold for plagiarism detection
                            if (similarity > 0.85 && sentence.getCitedKeys().isEmpty()) {
                                CitationIssue issue = CitationIssue.builder()
                                        .citationCheck(check)
                                        .projectId(check.getProjectId())
                                        .documentId(check.getDocumentId())
                                        .type(
                                                CitationIssue.IssueType
                                                        .MISSING_CITATION) // Could add POSSIBLE_PLAGIARISM type
                                        .severity(CitationIssue.Severity.HIGH)
                                        .fromPos(sentence.getStartPos())
                                        .toPos(sentence.getEndPos())
                                        .lineStart(sentence.getLineStart())
                                        .lineEnd(sentence.getLineEnd())
                                        .snippet("High similarity to source material without citation (similarity: "
                                                + String.format("%.2f", similarity) + ")")
                                        .citedKeys(new String[] {})
                                        .suggestions(createPlagiarismSuggestions(paperId, paragraph))
                                        .build();

                                // Add evidence
                                Map<String, Object> sourceData = new HashMap<>();
                                sourceData.put("kind", "local");
                                sourceData.put("paperId", paperId);
                                sourceData.put("page", paragraph.getPage());

                                CitationEvidence evidence = CitationEvidence.builder()
                                        .citationIssue(issue)
                                        .source(sourceData)
                                        .matchedText(paragraph.getText())
                                        .similarity(similarity)
                                        .supportScore(similarity)
                                        .build();

                                issue.setEvidence(Arrays.asList(evidence));
                                issues.add(issue);
                                log.debug(
                                        "Found potential plagiarism: similarity {} with paper {}", similarity, paperId);
                                break; // Only report once per sentence
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error detecting plagiarism", e);
        }

        return issues;
    }

    // Helper methods for the new detection algorithms

    private Set<String> extractBibliographyKeys(String latexContent) {
        Set<String> keys = new HashSet<>();
        Pattern bibitemPattern = Pattern.compile("\\\\bibitem\\{([^}]+)\\}");
        Matcher matcher = bibitemPattern.matcher(latexContent);
        while (matcher.find()) {
            keys.add(matcher.group(1));
        }
        return keys;
    }

    private int findBibliographyEntryPosition(String latexContent, String bibKey) {
        String searchPattern = "\\bibitem{" + bibKey + "}";
        int pos = latexContent.indexOf(searchPattern);
        return pos >= 0 ? pos : 0;
    }

    private int getLineNumberFromPosition(String content, int position) {
        if (position <= 0) return 1;
        String beforePosition = content.substring(0, Math.min(position, content.length()));
        return beforePosition.split("\n").length;
    }

    private Map<String, BibliographyEntry> parseBibliographyEntries(String latexContent) {
        Map<String, BibliographyEntry> entries = new HashMap<>();
        Pattern bibitemPattern = Pattern.compile(
                "\\\\bibitem\\{([^}]+)\\}\\s*([^\\\\]*?)(?=\\\\bibitem|\\\\end\\{thebibliography\\}|$)",
                Pattern.DOTALL);
        Matcher matcher = bibitemPattern.matcher(latexContent);

        while (matcher.find()) {
            String key = matcher.group(1);
            String content = matcher.group(2).trim();
            entries.put(key, new BibliographyEntry(key, content));
        }

        return entries;
    }

    private boolean isLikelyMatch(BibliographyEntry bibEntry, ExtractedReference ref) {
        // Simple title matching - could be enhanced with fuzzy matching
        if (ref.getTitle() != null && bibEntry.getContent() != null) {
            String bibTitle = extractTitleFromBibEntry(bibEntry.getContent());
            return bibTitle != null && calculateTextSimilarity(bibTitle, ref.getTitle()) > 0.7;
        }
        return false;
    }

    private String extractTitleFromBibEntry(String bibContent) {
        // Simple title extraction - could be enhanced with proper BibTeX parsing
        Pattern titlePattern = Pattern.compile("([A-Z][^.]*\\.)");
        Matcher matcher = titlePattern.matcher(bibContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> validateBibliographyMetadata(BibliographyEntry bibEntry, ExtractedReference ref) {
        List<String> issues = new ArrayList<>();

        // Check year mismatch
        String bibYear = extractYearFromBibEntry(bibEntry.getContent());
        if (bibYear != null
                && ref.getYear() != null
                && !bibYear.equals(ref.getYear().toString())) {
            issues.add("Year mismatch: bibliography has " + bibYear + ", actual is " + ref.getYear());
        }

        // Check for fake DOIs
        if (bibEntry.getContent().contains("fake-doi") || bibEntry.getContent().contains("10.0000/")) {
            issues.add("Fake or placeholder DOI detected");
        }

        return issues;
    }

    private String extractYearFromBibEntry(String bibContent) {
        Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
        Matcher matcher = yearPattern.matcher(bibContent);
        return matcher.find() ? matcher.group(0) : null;
    }

    private List<Map<String, Object>> createMetadataSuggestions(ExtractedReference ref) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("kind", "metadata_correction");
        suggestion.put("title", ref.getTitle());
        suggestion.put("year", ref.getYear());
        suggestion.put("doi", ref.getDoi());
        suggestion.put("rationale", "Corrected metadata from verified source");
        suggestions.add(suggestion);
        return suggestions;
    }

    private List<Map<String, Object>> createPlagiarismSuggestions(String paperId, ExtractedParagraph paragraph) {
        List<Map<String, Object>> suggestions = new ArrayList<>();
        Map<String, Object> suggestion = new HashMap<>();
        suggestion.put("kind", "local");
        suggestion.put("paperId", paperId);
        suggestion.put("title", "Add citation for similar content");
        suggestion.put("rationale", "High similarity detected - please add proper citation");
        suggestions.add(suggestion);
        return suggestions;
    }

    // Helper methods for suggestion generation

    private String getAuthorsString(Paper paper) {
        try {
            // Get authors from paper_authors table
            List<PaperAuthor> paperAuthors = paperAuthorRepository.findByPaperIdOrderByAuthorOrderAsc(paper.getId());
            if (!paperAuthors.isEmpty()) {
                return paperAuthors.stream().map(pa -> pa.getAuthor().getName()).collect(Collectors.joining(", "));
            }

            return "Unknown authors";
        } catch (Exception e) {
            return "Unknown authors";
        }
    }

    private Integer extractYearFromDate(LocalDate publicationDate) {
        return publicationDate != null ? publicationDate.getYear() : null;
    }

    private String getVenueString(Paper paper) {
        // For now, return a placeholder since venue repository isn't available
        // This could be enhanced to use publication venue data if needed
        return "Proceedings"; // Simple fallback
    }

    private String generateBibTexEntry(Paper paper) {
        try {
            StringBuilder bibtex = new StringBuilder();
            String citationKey = generateCitationKey(paper);

            bibtex.append("@article{").append(citationKey).append(",\n");
            bibtex.append("  title={").append(escapeBibTeX(paper.getTitle())).append("},\n");
            bibtex.append("  author={")
                    .append(escapeBibTeX(getAuthorsString(paper)))
                    .append("},\n");

            if (paper.getPublicationDate() != null) {
                bibtex.append("  year={")
                        .append(paper.getPublicationDate().getYear())
                        .append("},\n");
            }

            String venue = getVenueString(paper);
            if (!"Unknown venue".equals(venue)) {
                bibtex.append("  journal={").append(escapeBibTeX(venue)).append("},\n");
            }

            if (paper.getDoi() != null && !paper.getDoi().isEmpty()) {
                bibtex.append("  doi={").append(paper.getDoi()).append("},\n");
            }

            if (paper.getPaperUrl() != null && !paper.getPaperUrl().isEmpty()) {
                bibtex.append("  url={").append(paper.getPaperUrl()).append("},\n");
            }

            bibtex.append("}");

            return bibtex.toString();
        } catch (Exception e) {
            log.warn("Failed to generate BibTeX for paper {}: {}", paper.getId(), e.getMessage());
            return null;
        }
    }

    private String generateCitationKey(Paper paper) {
        try {
            String firstAuthor = getFirstAuthorLastName(paper);
            Integer year = extractYearFromDate(paper.getPublicationDate());

            if (firstAuthor != null && year != null) {
                return firstAuthor + year;
            } else if (firstAuthor != null) {
                return firstAuthor + "2024";
            } else {
                return "Unknown" + (year != null ? year : "2024");
            }
        } catch (Exception e) {
            return "Citation" + System.currentTimeMillis();
        }
    }

    private String getFirstAuthorLastName(Paper paper) {
        try {
            List<PaperAuthor> paperAuthors = paperAuthorRepository.findByPaperIdOrderByAuthorOrderAsc(paper.getId());
            if (!paperAuthors.isEmpty()) {
                String fullName = paperAuthors.get(0).getAuthor().getName();
                String[] nameParts = fullName.trim().split("\\s+");
                return nameParts[nameParts.length - 1]; // Last name
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeBibTeX(String text) {
        if (text == null) return "";
        return text.replace("{", "\\{").replace("}", "\\}").replace("&", "\\&");
    }

    // Bibliography entry helper class
    private static class BibliographyEntry {
        private final String key;
        private final String content;

        public BibliographyEntry(String key, String content) {
            this.key = key;
            this.content = content;
        }

        public String getKey() {
            return key;
        }

        public String getContent() {
            return content;
        }
    }
}
