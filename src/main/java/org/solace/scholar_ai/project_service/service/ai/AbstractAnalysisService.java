package org.solace.scholar_ai.project_service.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.ai.AbstractAnalysisDto;
import org.solace.scholar_ai.project_service.dto.ai.AbstractHighlightDto;
import org.solace.scholar_ai.project_service.model.paper.AbstractAnalysis;
import org.solace.scholar_ai.project_service.model.paper.AbstractHighlight;
import org.solace.scholar_ai.project_service.repository.AbstractAnalysisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbstractAnalysisService {

    private final GeminiGeneralService geminiGeneralService;
    private final ObjectMapper objectMapper;
    private final AbstractAnalysisRepository abstractAnalysisRepository;

    /**
     * Extract JSON content from Gemini response, handling markdown code blocks
     */
    private String extractJsonFromResponse(String response) {
        // Remove markdown code blocks if present
        if (response.contains("```json")) {
            int startIndex = response.indexOf("```json") + 7;
            int endIndex = response.lastIndexOf("```");
            if (endIndex > startIndex) {
                return response.substring(startIndex, endIndex).trim();
            }
        } else if (response.contains("```")) {
            int startIndex = response.indexOf("```") + 3;
            int endIndex = response.lastIndexOf("```");
            if (endIndex > startIndex) {
                return response.substring(startIndex, endIndex).trim();
            }
        }
        return response.trim();
    }

    public AbstractHighlightDto analyzeAbstractHighlights(String abstractText) {
        log.info(
                "🔍 Analyzing abstract for highlights: {}",
                abstractText.substring(0, Math.min(100, abstractText.length())) + "...");

        String prompt = String.format(
                """
                        Analyze the following research paper abstract and identify ONLY the most critical and technically significant terms that should be highlighted for emphasis.
                        Return ONLY a JSON object with the following structure:
                        {
                            "highlights": [
                                {
                                    "text": "exact text to highlight",
                                    "type": "algorithm|methodology|concept|metric|framework",
                                    "startIndex": 0,
                                    "endIndex": 10
                                }
                            ]
                        }

                        Abstract: %s

                        CATEGORIZATION RULES:
                        - "algorithm": Specific algorithms, methods, or techniques (e.g., "FMixCut", "DDM", "cross-entropy minimization", "entropy maximization")
                        - "methodology": Research approaches or methodologies (e.g., "semi-supervised learning", "data augmentation", "transfer learning", "pseudo-labeling")
                        - "concept": Key theoretical concepts or principles (e.g., "knowledge transferability", "trustworthiness", "IID", "non-IID", "adversarial robustness")
                        - "metric": Performance measures or evaluation metrics (e.g., "accuracy", "F1-score", "cross-entropy", "entropy")
                        - "framework": Complete systems or frameworks (e.g., "FMixCutMatch", "FMCmatch", "SSL framework")

                        STRICT SELECTION RULES:
                        - Highlight ONLY terms that are absolutely central to the paper's technical contribution
                        - Focus on domain-specific, technical terms that convey significant academic value
                        - DO NOT highlight: common words, articles (a, an, the), prepositions (in, on, at, for), conjunctions (and, or, but), generic verbs (is, are, have, been)
                        - DO NOT highlight partial words or incomplete terms
                        - Maximum 10-15 highlights for the entire abstract - be very selective
                        - Prioritize terms that represent novel contributions, key techniques, or central ideas
                        - Ensure startIndex and endIndex capture complete words only
                        - Return valid JSON only, no additional text
                        """,
                abstractText);

        try {
            String response = geminiGeneralService.generateContent(prompt);
            log.info("📥 Gemini response for highlights: {}", response);

            // Extract JSON from markdown code blocks if present
            String jsonContent = extractJsonFromResponse(response);
            log.info("📄 Extracted JSON content: {}", jsonContent);

            // Parse the JSON response
            Map<String, Object> parsedResponse =
                    objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> highlightsData = (List<Map<String, Object>>) parsedResponse.get("highlights");

            List<AbstractHighlightDto.Highlight> highlights = highlightsData.stream()
                    .map(highlight -> new AbstractHighlightDto.Highlight(
                            (String) highlight.get("text"),
                            (String) highlight.get("type"),
                            ((Number) highlight.get("startIndex")).intValue(),
                            ((Number) highlight.get("endIndex")).intValue()))
                    .toList();

            return new AbstractHighlightDto(highlights);

        } catch (JsonProcessingException e) {
            log.error("❌ Error parsing Gemini response for highlights: {}", e.getMessage(), e);
            return new AbstractHighlightDto(List.of());
        } catch (Exception e) {
            log.error("❌ Error analyzing abstract highlights: {}", e.getMessage(), e);
            return new AbstractHighlightDto(List.of());
        }
    }

    public AbstractAnalysisDto analyzeAbstractInsights(String abstractText) {
        log.info(
                "🧠 Analyzing abstract for insights: {}",
                abstractText.substring(0, Math.min(100, abstractText.length())) + "...");

        String prompt = String.format(
                """
                        Analyze the following research paper abstract and extract key insights with high precision and technical accuracy.
                        Return ONLY a JSON object with the following structure:
                        {
                            "focus": "primary research focus or main objective",
                            "approach": "specific methodology, technique, or approach used",
                            "emphasis": "key contribution, innovation, or emphasis",
                            "methodology": "research methodology type (e.g., Survey, Experiment, Theoretical Analysis, etc.)",
                            "impact": "potential impact, significance, or implications",
                            "challenges": "key challenges, limitations, or problems addressed"
                        }

                        Abstract: %s

                        ANALYSIS RULES:
                        - Be highly specific and technically accurate based on the abstract content
                        - Use precise, academic terminology that reflects the paper's contribution
                        - For "methodology": Identify the specific research methodology (e.g., "Literature Review", "Empirical Study", "Theoretical Analysis", "Experimental Evaluation", "Comparative Study")
                        - For "approach": Describe the specific technique, algorithm, or method used
                        - For "challenges": Focus on technical challenges, limitations, or problems the research addresses
                        - Keep responses concise but informative (2-3 sentences max)
                        - If information is not clearly available, use "Not specified"
                        - Return valid JSON only, no additional text
                        """,
                abstractText);

        try {
            String response = geminiGeneralService.generateContent(prompt);
            log.info("📥 Gemini response for insights: {}", response);

            // Extract JSON from markdown code blocks if present
            String jsonContent = extractJsonFromResponse(response);
            log.info("📄 Extracted JSON content: {}", jsonContent);

            // Parse the JSON response
            Map<String, Object> parsedResponse =
                    objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

            return new AbstractAnalysisDto(
                    (String) parsedResponse.get("focus"),
                    (String) parsedResponse.get("approach"),
                    (String) parsedResponse.get("emphasis"),
                    (String) parsedResponse.get("methodology"),
                    (String) parsedResponse.get("impact"),
                    (String) parsedResponse.get("challenges"));

        } catch (JsonProcessingException e) {
            log.error("❌ Error parsing Gemini response for insights: {}", e.getMessage(), e);
            return new AbstractAnalysisDto(
                    "Not specified",
                    "Not specified",
                    "Not specified",
                    "Not specified",
                    "Not specified",
                    "Not specified");
        } catch (Exception e) {
            log.error("❌ Error analyzing abstract insights: {}", e.getMessage(), e);
            return new AbstractAnalysisDto(
                    "Not specified",
                    "Not specified",
                    "Not specified",
                    "Not specified",
                    "Not specified",
                    "Not specified");
        }
    }

    /**
     * Generate hash for abstract text to detect changes
     */
    private String generateAbstractHash(String abstractText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(abstractText.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("❌ Error generating hash: {}", e.getMessage(), e);
            return abstractText.hashCode() + "";
        }
    }

    /**
     * Get or create analysis for a paper
     */
    @Transactional
    public AbstractAnalysisDto getOrCreateAnalysis(String paperId, String abstractText) {
        try {
            String abstractHash = generateAbstractHash(abstractText);

            // Try to find existing analysis
            Optional<AbstractAnalysis> existingAnalysis =
                    abstractAnalysisRepository.findByPaperIdAndAbstractTextHash(paperId, abstractHash);

            if (existingAnalysis.isPresent()) {
                log.info("📋 Found existing analysis for paper: {}", paperId);
                return convertToDto(existingAnalysis.get());
            }

            // Create new analysis
            log.info("🤖 Creating new analysis for paper: {}", paperId);
            AbstractAnalysisDto insights = analyzeAbstractInsights(abstractText);
            AbstractHighlightDto highlights = analyzeAbstractHighlights(abstractText);

            // Truncate fields if they're too long for database
            String truncatedFocus = truncateField(insights.getFocus(), 1000);
            String truncatedApproach = truncateField(insights.getApproach(), 1000);
            String truncatedEmphasis = truncateField(insights.getEmphasis(), 1000);
            String truncatedMethodology = truncateField(insights.getMethodology(), 1000);
            String truncatedImpact = truncateField(insights.getImpact(), 1000);
            String truncatedChallenges = truncateField(insights.getChallenges(), 1000);

            // Save to database
            AbstractAnalysis analysis = AbstractAnalysis.builder()
                    .paperId(paperId)
                    .abstractTextHash(abstractHash)
                    .focus(truncatedFocus)
                    .approach(truncatedApproach)
                    .emphasis(truncatedEmphasis)
                    .methodology(truncatedMethodology)
                    .impact(truncatedImpact)
                    .challenges(truncatedChallenges)
                    .analysisVersion("1.0")
                    .isActive(true)
                    .build();

            AbstractAnalysis savedAnalysis = abstractAnalysisRepository.save(analysis);

            // Save highlights
            if (highlights.getHighlights() != null
                    && !highlights.getHighlights().isEmpty()) {
                List<AbstractHighlight> highlightEntities = highlights.getHighlights().stream()
                        .map(highlight -> AbstractHighlight.builder()
                                .abstractAnalysis(savedAnalysis)
                                .text(truncateField(highlight.getText(), 500))
                                .type(truncateField(highlight.getType(), 50))
                                .startIndex(highlight.getStartIndex())
                                .endIndex(highlight.getEndIndex())
                                .build())
                        .collect(Collectors.toList());

                savedAnalysis.setHighlights(highlightEntities);
            }

            return insights;
        } catch (Exception e) {
            log.error("❌ Error in getOrCreateAnalysis for paper {}: {}", paperId, e.getMessage(), e);
            throw new RuntimeException("Failed to analyze abstract: " + e.getMessage(), e);
        }
    }

    /**
     * Truncate field to specified length
     */
    private String truncateField(String field, int maxLength) {
        if (field == null) return null;
        if (field.length() <= maxLength) return field;
        return field.substring(0, maxLength - 3) + "...";
    }

    /**
     * Re-analyze abstract and update database
     */
    @Transactional
    public AbstractAnalysisDto reanalyzeAbstract(String paperId, String abstractText) {
        try {
            log.info("🔄 Re-analyzing abstract for paper: {}", paperId);

            // Deactivate existing analysis
            Optional<AbstractAnalysis> existingAnalysis = abstractAnalysisRepository.findLatestByPaperId(paperId);
            if (existingAnalysis.isPresent()) {
                existingAnalysis.get().setIsActive(false);
                abstractAnalysisRepository.save(existingAnalysis.get());
            }

            // Create new analysis
            return getOrCreateAnalysis(paperId, abstractText);
        } catch (Exception e) {
            log.error("❌ Error in reanalyzeAbstract for paper {}: {}", paperId, e.getMessage(), e);
            throw new RuntimeException("Failed to re-analyze abstract: " + e.getMessage(), e);
        }
    }

    /**
     * Convert database entity to DTO
     */
    private AbstractAnalysisDto convertToDto(AbstractAnalysis analysis) {
        return new AbstractAnalysisDto(
                analysis.getFocus(),
                analysis.getApproach(),
                analysis.getEmphasis(),
                analysis.getMethodology(),
                analysis.getImpact(),
                analysis.getChallenges());
    }

    /**
     * Get highlights from database
     */
    @Transactional(readOnly = true)
    public AbstractHighlightDto getHighlightsFromDb(String paperId) {
        Optional<AbstractAnalysis> analysis = abstractAnalysisRepository.findLatestByPaperIdWithHighlights(paperId);

        if (analysis.isPresent() && analysis.get().getHighlights() != null) {
            List<AbstractHighlightDto.Highlight> highlights = analysis.get().getHighlights().stream()
                    .map(h -> new AbstractHighlightDto.Highlight(
                            h.getText(), h.getType(), h.getStartIndex(), h.getEndIndex()))
                    .collect(Collectors.toList());

            return new AbstractHighlightDto(highlights);
        }

        return new AbstractHighlightDto(List.of());
    }
}
