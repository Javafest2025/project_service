package org.solace.scholar_ai.project_service.service.chat;

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.solace.scholar_ai.project_service.model.chat.ContentChunk;
import org.solace.scholar_ai.project_service.model.extraction.*;
import org.solace.scholar_ai.project_service.service.chat.QueryRequirementAnalysisService.DataRequirement;
import org.springframework.stereotype.Service;

/**
 * Enhanced Content Retrieval Service that intelligently selects and ranks content
 * based on query analysis and comprehensive paper extraction data
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedContentRetrievalService {

    private final IntelligentQueryStrategy queryStrategy;

    /**
     * Retrieve optimally prioritized content for AI response generation
     */
    public List<ContentChunk> retrieveOptimalContent(
            PaperExtraction extraction,
            String query,
            String selectedText,
            PaperChatRequest.SelectionContext selectionContext,
            List<Author> authors) {

        log.debug("Retrieving optimal content for query: {}", query);

        // Analyze query to determine optimal strategy
        IntelligentQueryStrategy.QueryAnalysis analysis =
                queryStrategy.analyzeQuery(query, selectedText, selectionContext != null);

        List<ContentChunk> allChunks = new ArrayList<>();

        // 1. PRIORITY: Selected text context (always highest priority)
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            allChunks.add(createSelectedTextChunk(selectedText, selectionContext));
        }

        // 2. Apply intelligent content selection based on query type
        IntelligentQueryStrategy.ContextRequirements requirements = analysis.getContextRequirements();
        IntelligentQueryStrategy.ContentPriority priorities = requirements.getContentPriority();

        // Abstract and introduction (high priority for summaries and conceptual questions)
        if (priorities.getAbstractWeight() > 0.5 || priorities.getIntroductionWeight() > 0.5) {
            allChunks.addAll(getAbstractAndIntroduction(extraction, priorities));
        }

        // Methodology and technical content
        if (priorities.getMethodologyWeight() > 0.5 || priorities.getTechnicalWeight() > 0.5) {
            allChunks.addAll(getMethodologyContent(extraction, priorities, query));
        }

        // Results and experimental content
        if (priorities.getResultsWeight() > 0.5 || priorities.getExperimentsWeight() > 0.5) {
            allChunks.addAll(getResultsContent(extraction, priorities, query));
        }

        // Conclusion content
        if (priorities.getConclusionWeight() > 0.5) {
            allChunks.addAll(getConclusionContent(extraction, priorities));
        }

        // Figures and tables (important for results and technical queries)
        if (priorities.getFiguresWeight() > 0.3 || priorities.getTablesWeight() > 0.3) {
            allChunks.addAll(getVisualContent(extraction, priorities, query));
        }

        // Equations (for technical queries)
        if (priorities.getEquationsWeight() > 0.3) {
            allChunks.addAll(getEquationContent(extraction, priorities, query));
        }

        // References (for comparison queries)
        if (requirements.isIncludeReferences() && priorities.getReferencesWeight() > 0.3) {
            allChunks.addAll(getReferenceContent(extraction, priorities, query));
        }

        // Author information (when specifically requested)
        if (requirements.isIncludeAuthorInfo() && authors != null && !authors.isEmpty()) {
            allChunks.addAll(getAuthorContent(authors, priorities));
        }

        // Handle specific references (pages, figures, sections)
        if (analysis.getSpecificReferences() != null) {
            allChunks.addAll(getSpecificReferencedContent(extraction, analysis.getSpecificReferences()));
        }

        // 3. Apply intelligent ranking and filtering
        List<ContentChunk> rankedChunks = rankAndFilterContent(allChunks, query, analysis, requirements.getMaxChunks());

        log.debug(
                "Retrieved {} optimally ranked content chunks for query type: {}",
                rankedChunks.size(),
                analysis.getPrimaryType());

        return rankedChunks;
    }

    /**
     * AI-powered content retrieval based on determined data requirements
     * This uses the existing content retrieval methods but with AI-driven selection
     */
    public List<ContentChunk> retrieveContentBasedOnRequirements(
            PaperExtraction extraction,
            String query,
            String selectedText,
            PaperChatRequest.SelectionContext selectionContext,
            List<Author> authors,
            Set<DataRequirement> dataRequirements) {

        log.debug("Retrieving content based on AI-determined requirements: {}", dataRequirements);

        // Use existing method but create a mock analysis based on AI requirements
        IntelligentQueryStrategy.QueryAnalysis mockAnalysis = createMockAnalysisFromRequirements(dataRequirements);

        // Call the existing retrieval method
        return retrieveOptimalContent(extraction, query, selectedText, selectionContext, authors);
    }

    /**
     * Create a mock query analysis based on AI-determined requirements
     * This bridges the new AI system with the existing content retrieval logic
     */
    private IntelligentQueryStrategy.QueryAnalysis createMockAnalysisFromRequirements(
            Set<DataRequirement> requirements) {
        // For now, create a simple mock that triggers comprehensive content retrieval
        // This ensures we get all needed content while the existing ranking system handles prioritization
        return IntelligentQueryStrategy.QueryAnalysis.builder()
                .primaryType(IntelligentQueryStrategy.QueryType.GENERAL)
                .complexityScore(0.8)
                .contextRequirements(IntelligentQueryStrategy.ContextRequirements.builder()
                        .includeAuthorInfo(requirements.contains(DataRequirement.AUTHORS))
                        .maxChunks(determineMaxChunks(requirements))
                        .contentPriority(createContentPriorityFromRequirements(requirements))
                        .build())
                .build();
    }

    /**
     * Determine max chunks based on requirements scope
     */
    private int determineMaxChunks(Set<DataRequirement> requirements) {
        if (requirements.contains(DataRequirement.FULL_PAPER_CONTENT)
                || requirements.contains(DataRequirement.ALL_SECTIONS)) {
            return 50; // High for comprehensive queries
        } else if (requirements.size() > 5) {
            return 30; // Medium for multi-faceted queries
        } else {
            return 20; // Standard for focused queries
        }
    }

    /**
     * Create content priority weighting based on AI requirements
     */
    private IntelligentQueryStrategy.ContentPriority createContentPriorityFromRequirements(
            Set<DataRequirement> requirements) {
        return IntelligentQueryStrategy.ContentPriority.builder()
                .abstractWeight(requirements.contains(DataRequirement.ABSTRACT) ? 1.0 : 0.3)
                .introductionWeight(requirements.contains(DataRequirement.INTRODUCTION) ? 1.0 : 0.4)
                .methodologyWeight(requirements.contains(DataRequirement.METHODOLOGY) ? 1.0 : 0.3)
                .resultsWeight(requirements.contains(DataRequirement.RESULTS) ? 1.0 : 0.3)
                .conclusionWeight(requirements.contains(DataRequirement.CONCLUSION) ? 1.0 : 0.3)
                .referencesWeight(requirements.contains(DataRequirement.REFERENCES) ? 1.0 : 0.2)
                .figuresWeight(requirements.contains(DataRequirement.FIGURES) ? 1.0 : 0.3)
                .tablesWeight(requirements.contains(DataRequirement.TABLES) ? 1.0 : 0.3)
                .equationsWeight(requirements.contains(DataRequirement.EQUATIONS) ? 1.0 : 0.2)
                .technicalWeight(
                        requirements.contains(DataRequirement.METHODOLOGY)
                                        || requirements.contains(DataRequirement.ALGORITHMS)
                                ? 1.0
                                : 0.4)
                .experimentsWeight(
                        requirements.contains(DataRequirement.EXPERIMENTAL_SETUP)
                                        || requirements.contains(DataRequirement.RESULTS)
                                ? 1.0
                                : 0.3)
                .build();
    }

    /**
     * Create selected text chunk with highest priority
     */
    private ContentChunk createSelectedTextChunk(
            String selectedText, PaperChatRequest.SelectionContext selectionContext) {
        return ContentChunk.builder()
                .content("SELECTED TEXT CONTEXT: " + selectedText)
                .source("User-selected text"
                        + (selectionContext != null && selectionContext.getPageNumber() != null
                                ? " (Page " + selectionContext.getPageNumber() + ")"
                                : ""))
                .type("selected_text")
                .relevanceScore(1.0) // Always highest priority
                .pageNumber(selectionContext != null ? selectionContext.getPageNumber() : null)
                .build();
    }

    /**
     * Get abstract and introduction content
     */
    private List<ContentChunk> getAbstractAndIntroduction(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities) {
        List<ContentChunk> chunks = new ArrayList<>();

        // Abstract content
        if (extraction.getAbstractText() != null && priorities.getAbstractWeight() > 0) {
            chunks.add(ContentChunk.builder()
                    .content("ABSTRACT: " + extraction.getAbstractText())
                    .source("Paper Abstract")
                    .type("abstract")
                    .relevanceScore(priorities.getAbstractWeight())
                    .build());
        }

        // Introduction sections
        extraction.getSections().stream()
                .filter(section -> isIntroductionSection(section))
                .forEach(section -> {
                    String content = extractSectionContent(section);
                    if (content != null && !content.isEmpty()) {
                        chunks.add(ContentChunk.builder()
                                .content(content)
                                .source("Section: " + section.getTitle())
                                .type("introduction")
                                .relevanceScore(priorities.getIntroductionWeight())
                                .pageNumber(section.getPageStart())
                                .build());
                    }
                });

        return chunks;
    }

    /**
     * Get methodology and technical content
     */
    private List<ContentChunk> getMethodologyContent(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities, String query) {
        List<ContentChunk> chunks = new ArrayList<>();

        extraction.getSections().stream()
                .filter(section -> isMethodologySection(section) || isTechnicalSection(section, query))
                .forEach(section -> {
                    String content = extractSectionContent(section);
                    if (content != null && !content.isEmpty()) {
                        double relevance = calculateContentRelevance(content, query, priorities.getMethodologyWeight());
                        chunks.add(ContentChunk.builder()
                                .content(content)
                                .source("Section: " + section.getTitle())
                                .type("methodology")
                                .relevanceScore(relevance)
                                .pageNumber(section.getPageStart())
                                .build());
                    }
                });

        return chunks;
    }

    /**
     * Get results and experimental content
     */
    private List<ContentChunk> getResultsContent(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities, String query) {
        List<ContentChunk> chunks = new ArrayList<>();

        extraction.getSections().stream()
                .filter(section -> isResultsSection(section) || isExperimentalSection(section))
                .forEach(section -> {
                    String content = extractSectionContent(section);
                    if (content != null && !content.isEmpty()) {
                        double relevance = calculateContentRelevance(content, query, priorities.getResultsWeight());
                        chunks.add(ContentChunk.builder()
                                .content(content)
                                .source("Section: " + section.getTitle())
                                .type("results")
                                .relevanceScore(relevance)
                                .pageNumber(section.getPageStart())
                                .build());
                    }
                });

        return chunks;
    }

    /**
     * Get conclusion content
     */
    private List<ContentChunk> getConclusionContent(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities) {
        List<ContentChunk> chunks = new ArrayList<>();

        extraction.getSections().stream().filter(this::isConclusionSection).forEach(section -> {
            String content = extractSectionContent(section);
            if (content != null && !content.isEmpty()) {
                chunks.add(ContentChunk.builder()
                        .content(content)
                        .source("Section: " + section.getTitle())
                        .type("conclusion")
                        .relevanceScore(priorities.getConclusionWeight())
                        .pageNumber(section.getPageStart())
                        .build());
            }
        });

        return chunks;
    }

    /**
     * Get visual content (figures and tables)
     */
    private List<ContentChunk> getVisualContent(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities, String query) {
        List<ContentChunk> chunks = new ArrayList<>();

        // Figures
        if (priorities.getFiguresWeight() > 0.3) {
            extraction.getFigures().forEach(figure -> {
                String figureText = buildFigureText(figure);
                double relevance = calculateContentRelevance(figureText, query, priorities.getFiguresWeight());
                if (relevance > 0.2) {
                    chunks.add(ContentChunk.builder()
                            .content("Figure " + figure.getLabel() + ": " + figure.getCaption()
                                    + (figure.getOcrText() != null ? "\nOCR Text: " + figure.getOcrText() : ""))
                            .source("Figure " + figure.getLabel() + " (Page " + figure.getPage() + ")")
                            .type("figure")
                            .relevanceScore(relevance)
                            .pageNumber(figure.getPage())
                            .build());
                }
            });
        }

        // Tables
        if (priorities.getTablesWeight() > 0.3) {
            extraction.getTables().forEach(table -> {
                String tableText = buildTableText(table);
                double relevance = calculateContentRelevance(tableText, query, priorities.getTablesWeight());
                if (relevance > 0.2) {
                    chunks.add(ContentChunk.builder()
                            .content("Table " + table.getLabel() + ": " + table.getCaption()
                                    + (table.getHeaders() != null ? "\nHeaders: " + table.getHeaders() : "")
                                    + (table.getRows() != null ? "\nData: " + truncateText(table.getRows(), 500) : ""))
                            .source("Table " + table.getLabel() + " (Page " + table.getPage() + ")")
                            .type("table")
                            .relevanceScore(relevance)
                            .pageNumber(table.getPage())
                            .build());
                }
            });
        }

        return chunks;
    }

    /**
     * Get equation content
     */
    private List<ContentChunk> getEquationContent(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities, String query) {
        List<ContentChunk> chunks = new ArrayList<>();

        extraction.getEquations().forEach(equation -> {
            if (equation.getLatex() != null || equation.getLabel() != null) {
                String equationText = (equation.getLabel() != null ? equation.getLabel() : "")
                        + (equation.getLatex() != null ? " LaTeX: " + equation.getLatex() : "");
                double relevance = calculateContentRelevance(equationText, query, priorities.getEquationsWeight());
                if (relevance > 0.1 || containsMathKeywords(query)) {
                    chunks.add(ContentChunk.builder()
                            .content("Equation " + equation.getEquationId() + ": " + equationText)
                            .source("Equation " + equation.getEquationId() + " (Page " + equation.getPage() + ")")
                            .type("equation")
                            .relevanceScore(relevance)
                            .pageNumber(equation.getPage())
                            .build());
                }
            }
        });

        return chunks;
    }

    /**
     * Get reference content
     */
    private List<ContentChunk> getReferenceContent(
            PaperExtraction extraction, IntelligentQueryStrategy.ContentPriority priorities, String query) {
        List<ContentChunk> chunks = new ArrayList<>();

        extraction.getReferences().stream()
                .limit(10) // Limit references for context efficiency
                .forEach(reference -> {
                    String refText = buildReferenceText(reference);
                    double relevance = calculateContentRelevance(refText, query, priorities.getReferencesWeight());
                    if (relevance > 0.1) {
                        chunks.add(ContentChunk.builder()
                                .content(refText)
                                .source("Reference " + reference.getReferenceId())
                                .type("reference")
                                .relevanceScore(relevance)
                                .build());
                    }
                });

        return chunks;
    }

    /**
     * Get author content
     */
    private List<ContentChunk> getAuthorContent(
            List<Author> authors, IntelligentQueryStrategy.ContentPriority priorities) {
        List<ContentChunk> chunks = new ArrayList<>();

        String authorInfo = authors.stream()
                .map(author -> author.getName()
                        + (author.getPrimaryAffiliation() != null ? " (" + author.getPrimaryAffiliation() + ")" : ""))
                .collect(Collectors.joining(", "));

        chunks.add(ContentChunk.builder()
                .content("AUTHORS: " + authorInfo)
                .source("Paper Authors")
                .type("authors")
                .relevanceScore(0.8)
                .build());

        return chunks;
    }

    /**
     * Get content for specific references (pages, figures, etc.)
     */
    private List<ContentChunk> getSpecificReferencedContent(
            PaperExtraction extraction, IntelligentQueryStrategy.SpecificReferences specificRefs) {
        List<ContentChunk> chunks = new ArrayList<>();

        // Handle specific figure references
        specificRefs.figures.forEach(figNum -> {
            extraction.getFigures().stream()
                    .filter(fig -> fig.getLabel() != null && fig.getLabel().contains(figNum.toString()))
                    .forEach(figure -> {
                        chunks.add(ContentChunk.builder()
                                .content("Figure " + figure.getLabel() + ": " + figure.getCaption()
                                        + (figure.getOcrText() != null ? "\nOCR Text: " + figure.getOcrText() : ""))
                                .source("Specific Figure " + figure.getLabel())
                                .type("specific_figure")
                                .relevanceScore(1.0)
                                .pageNumber(figure.getPage())
                                .build());
                    });
        });

        // Handle specific table references
        specificRefs.tables.forEach(tableNum -> {
            extraction.getTables().stream()
                    .filter(table ->
                            table.getLabel() != null && table.getLabel().contains(tableNum.toString()))
                    .forEach(table -> {
                        chunks.add(ContentChunk.builder()
                                .content("Table " + table.getLabel() + ": " + table.getCaption()
                                        + (table.getHeaders() != null ? "\nHeaders: " + table.getHeaders() : ""))
                                .source("Specific Table " + table.getLabel())
                                .type("specific_table")
                                .relevanceScore(1.0)
                                .pageNumber(table.getPage())
                                .build());
                    });
        });

        // Handle specific page references
        specificRefs.pages.forEach(pageNum -> {
            extraction.getSections().stream()
                    .filter(section -> section.getPageStart() != null
                            && section.getPageStart().equals(pageNum))
                    .forEach(section -> {
                        String content = extractSectionContent(section);
                        if (content != null && !content.isEmpty()) {
                            chunks.add(ContentChunk.builder()
                                    .content(content)
                                    .source("Page " + pageNum + " - Section: " + section.getTitle())
                                    .type("specific_page")
                                    .relevanceScore(1.0)
                                    .pageNumber(pageNum)
                                    .build());
                        }
                    });
        });

        return chunks;
    }

    /**
     * Rank and filter content based on relevance and requirements
     */
    private List<ContentChunk> rankAndFilterContent(
            List<ContentChunk> allChunks,
            String query,
            IntelligentQueryStrategy.QueryAnalysis analysis,
            int maxChunks) {

        // Remove duplicates based on content similarity
        List<ContentChunk> uniqueChunks = removeDuplicateContent(allChunks);

        // Sort by relevance score (selected text always first)
        List<ContentChunk> sortedChunks = uniqueChunks.stream()
                .sorted((a, b) -> {
                    // Selected text always first
                    if ("selected_text".equals(a.getType())) return -1;
                    if ("selected_text".equals(b.getType())) return 1;

                    // Then by relevance score
                    return Double.compare(b.getRelevanceScore(), a.getRelevanceScore());
                })
                .limit(maxChunks)
                .collect(Collectors.toList());

        return sortedChunks;
    }

    // Helper methods
    private String extractSectionContent(ExtractedSection section) {
        if (section.getParagraphs() == null || section.getParagraphs().isEmpty()) {
            return section.getTitle();
        }
        return section.getParagraphs().stream()
                .map(ExtractedParagraph::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    private boolean isIntroductionSection(ExtractedSection section) {
        return section.getSectionType() != null
                && section.getSectionType().toLowerCase().contains("introduction");
    }

    private boolean isMethodologySection(ExtractedSection section) {
        String title = section.getTitle() != null ? section.getTitle().toLowerCase() : "";
        return title.contains("method")
                || title.contains("approach")
                || title.contains("implementation")
                || title.contains("design");
    }

    private boolean isTechnicalSection(ExtractedSection section, String query) {
        String title = section.getTitle() != null ? section.getTitle().toLowerCase() : "";
        return title.contains("algorithm")
                || title.contains("framework")
                || title.contains("architecture")
                || title.contains("system");
    }

    private boolean isResultsSection(ExtractedSection section) {
        return section.getSectionType() != null
                && section.getSectionType().toLowerCase().contains("results");
    }

    private boolean isExperimentalSection(ExtractedSection section) {
        return section.getSectionType() != null
                && (section.getSectionType().toLowerCase().contains("experiment")
                        || section.getSectionType().toLowerCase().contains("evaluation"));
    }

    private boolean isConclusionSection(ExtractedSection section) {
        return section.getSectionType() != null
                && section.getSectionType().toLowerCase().contains("conclusion");
    }

    private double calculateContentRelevance(String content, String query, double baseWeight) {
        if (content == null || content.isEmpty()) return 0.0;

        Set<String> queryWords = Arrays.stream(query.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)
                .collect(Collectors.toSet());

        Set<String> contentWords = Arrays.stream(content.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)
                .collect(Collectors.toSet());

        long matchingWords = queryWords.stream()
                .mapToLong(word -> contentWords.contains(word) ? 1 : 0)
                .sum();

        double textualRelevance = queryWords.isEmpty() ? 0.0 : (double) matchingWords / queryWords.size();
        return Math.min(1.0, baseWeight + (textualRelevance * 0.3));
    }

    private String buildFigureText(ExtractedFigure figure) {
        StringBuilder text = new StringBuilder();
        if (figure.getCaption() != null) text.append(figure.getCaption());
        if (figure.getOcrText() != null) text.append(" ").append(figure.getOcrText());
        return text.toString();
    }

    private String buildTableText(ExtractedTable table) {
        StringBuilder text = new StringBuilder();
        if (table.getCaption() != null) text.append(table.getCaption());
        if (table.getHeaders() != null) text.append(" ").append(table.getHeaders());
        if (table.getRows() != null) text.append(" ").append(truncateText(table.getRows(), 200));
        return text.toString();
    }

    private String buildReferenceText(ExtractedReference reference) {
        StringBuilder text = new StringBuilder();
        if (reference.getTitle() != null) text.append(reference.getTitle());
        if (reference.getAuthors() != null) text.append(" by ").append(reference.getAuthors());
        if (reference.getVenue() != null) text.append(" in ").append(reference.getVenue());
        return text.toString();
    }

    private boolean containsMathKeywords(String query) {
        String lowerQuery = query.toLowerCase();
        return lowerQuery.contains("equation")
                || lowerQuery.contains("formula")
                || lowerQuery.contains("math")
                || lowerQuery.contains("calculation");
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private List<ContentChunk> removeDuplicateContent(List<ContentChunk> chunks) {
        Set<String> seenContent = new HashSet<>();
        return chunks.stream()
                .filter(chunk -> {
                    String normalizedContent = chunk.getContent().toLowerCase().replaceAll("\\s+", " ");
                    return seenContent.add(normalizedContent);
                })
                .collect(Collectors.toList());
    }
}
