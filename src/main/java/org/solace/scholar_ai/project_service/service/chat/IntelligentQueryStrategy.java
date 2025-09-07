package org.solace.scholar_ai.project_service.service.chat;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Intelligent Query Strategy for optimizing AI responses based on question type and content
 * Analyzes user questions to determine the best context retrieval and AI prompting strategy
 */
@Slf4j
@Component
public class IntelligentQueryStrategy {

    // Question classification patterns
    private static final Map<QueryType, List<Pattern>> QUERY_PATTERNS = Map.of(
            QueryType.SUMMARY,
                    List.of(
                            Pattern.compile("(?i).*(summarize|summary|overview|abstract|main\\s+points?).*"),
                            Pattern.compile("(?i).*(what\\s+is\\s+this\\s+paper\\s+about|key\\s+findings).*"),
                            Pattern.compile("(?i).*(tldr|too\\s+long\\s+didn't\\s+read).*")),
            QueryType.METHODOLOGY,
                    List.of(
                            Pattern.compile("(?i).*(how\\s+did\\s+they|methodology|approach|method|technique).*"),
                            Pattern.compile("(?i).*(experimental\\s+setup|procedure|implementation).*"),
                            Pattern.compile("(?i).*(algorithm|framework|system\\s+design).*")),
            QueryType.RESULTS,
                    List.of(
                            Pattern.compile("(?i).*(results?|findings?|outcomes?|performance).*"),
                            Pattern.compile("(?i).*(benchmarks?|evaluation|metrics|comparison).*"),
                            Pattern.compile("(?i).*(speed|latency|throughput|efficiency).*")),
            QueryType.TECHNICAL_DETAILS,
                    List.of(
                            Pattern.compile("(?i).*(technical|implementation|code|algorithm).*"),
                            Pattern.compile("(?i).*(equation|formula|calculation|math).*"),
                            Pattern.compile("(?i).*(architecture|design|structure).*")),
            QueryType.COMPARISON,
                    List.of(
                            Pattern.compile("(?i).*(compare|comparison|versus|vs\\.|difference|similar).*"),
                            Pattern.compile("(?i).*(better|worse|advantage|disadvantage).*"),
                            Pattern.compile("(?i).*(baseline|state\\s+of\\s+art|prior\\s+work).*")),
            QueryType.SPECIFIC_REFERENCE,
                    List.of(
                            Pattern.compile("(?i).*(figure|fig\\.?|table|equation|section|page)\\s*\\d+.*"),
                            Pattern.compile("(?i).*(reference|citation|author|paper).*"),
                            Pattern.compile("(?i).*(line|paragraph|mentioned).*")),
            QueryType.CONCEPTUAL,
                    List.of(
                            Pattern.compile("(?i).*(explain|understand|concept|idea|theory).*"),
                            Pattern.compile("(?i).*(why|what\\s+does|how\\s+does|what\\s+is).*"),
                            Pattern.compile("(?i).*(definition|meaning|purpose).*")));

    // Content priority weights for different query types
    private static final Map<QueryType, ContentPriority> CONTENT_PRIORITIES = Map.of(
            QueryType.SUMMARY,
                    ContentPriority.builder()
                            .abstractWeight(1.0)
                            .introductionWeight(0.9)
                            .conclusionWeight(0.9)
                            .resultsWeight(0.7)
                            .methodologyWeight(0.5)
                            .technicalWeight(0.3)
                            .build(),
            QueryType.METHODOLOGY,
                    ContentPriority.builder()
                            .methodologyWeight(1.0)
                            .technicalWeight(0.9)
                            .experimentsWeight(0.8)
                            .introductionWeight(0.6)
                            .resultsWeight(0.4)
                            .abstractWeight(0.3)
                            .build(),
            QueryType.RESULTS,
                    ContentPriority.builder()
                            .resultsWeight(1.0)
                            .experimentsWeight(0.9)
                            .figuresWeight(0.8)
                            .tablesWeight(0.8)
                            .conclusionWeight(0.7)
                            .methodologyWeight(0.5)
                            .introductionWeight(0.3)
                            .build(),
            QueryType.TECHNICAL_DETAILS,
                    ContentPriority.builder()
                            .technicalWeight(1.0)
                            .equationsWeight(0.9)
                            .codeBlocksWeight(0.9)
                            .methodologyWeight(0.7)
                            .figuresWeight(0.6)
                            .resultsWeight(0.5)
                            .build(),
            QueryType.COMPARISON,
                    ContentPriority.builder()
                            .resultsWeight(1.0)
                            .referencesWeight(0.9)
                            .conclusionWeight(0.8)
                            .introductionWeight(0.7)
                            .methodologyWeight(0.6)
                            .abstractWeight(0.5)
                            .build(),
            QueryType.SPECIFIC_REFERENCE,
                    ContentPriority.builder()
                            .specificRefWeight(1.0)
                            .contextualWeight(0.8)
                            .figuresWeight(0.7)
                            .tablesWeight(0.7)
                            .equationsWeight(0.6)
                            .technicalWeight(0.5)
                            .build(),
            QueryType.CONCEPTUAL,
                    ContentPriority.builder()
                            .introductionWeight(1.0)
                            .abstractWeight(0.9)
                            .technicalWeight(0.7)
                            .methodologyWeight(0.6)
                            .resultsWeight(0.4)
                            .referencesWeight(0.6)
                            .build());

    /**
     * Analyze query and determine optimal strategy
     */
    public QueryAnalysis analyzeQuery(String query, String selectedText, boolean hasSelectionContext) {
        log.debug("Analyzing query: {}", query);

        QueryType primaryType = classifyQuery(query);
        Set<QueryType> secondaryTypes = findSecondaryTypes(query);

        // Extract specific references
        SpecificReferences specificRefs = extractSpecificReferences(query);

        // Determine context requirements
        ContextRequirements contextReqs =
                determineContextRequirements(primaryType, secondaryTypes, specificRefs, hasSelectionContext, query);

        // Generate optimal prompt strategy
        PromptStrategy promptStrategy = generatePromptStrategy(primaryType, selectedText != null);

        return QueryAnalysis.builder()
                .primaryType(primaryType)
                .secondaryTypes(secondaryTypes)
                .specificReferences(specificRefs)
                .contextRequirements(contextReqs)
                .promptStrategy(promptStrategy)
                .complexityScore(calculateComplexityScore(query, secondaryTypes.size()))
                .build();
    }

    /**
     * Classify the primary query type
     */
    private QueryType classifyQuery(String query) {
        for (Map.Entry<QueryType, List<Pattern>> entry : QUERY_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(query).find()) {
                    return entry.getKey();
                }
            }
        }
        return QueryType.GENERAL; // Default fallback
    }

    /**
     * Find secondary query types for multi-faceted questions
     */
    private Set<QueryType> findSecondaryTypes(String query) {
        Set<QueryType> secondaryTypes = new HashSet<>();
        for (Map.Entry<QueryType, List<Pattern>> entry : QUERY_PATTERNS.entrySet()) {
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(query).find()) {
                    secondaryTypes.add(entry.getKey());
                }
            }
        }
        return secondaryTypes;
    }

    /**
     * Extract specific references from the query
     */
    private SpecificReferences extractSpecificReferences(String query) {
        SpecificReferences refs = new SpecificReferences();

        // Extract page numbers
        Pattern pagePattern = Pattern.compile("(?i)page\\s+(\\d+)");
        refs.pages.addAll(extractNumbers(query, pagePattern));

        // Extract figure numbers
        Pattern figPattern = Pattern.compile("(?i)(?:figure|fig\\.?)\\s+(\\d+)");
        refs.figures.addAll(extractNumbers(query, figPattern));

        // Extract table numbers
        Pattern tablePattern = Pattern.compile("(?i)table\\s+(\\d+)");
        refs.tables.addAll(extractNumbers(query, tablePattern));

        // Extract equation numbers
        Pattern eqPattern = Pattern.compile("(?i)equation\\s+(\\d+)");
        refs.equations.addAll(extractNumbers(query, eqPattern));

        // Extract section references
        Pattern sectionPattern = Pattern.compile("(?i)section\\s+(\\d+(?:\\.\\d+)*)");
        refs.sections.addAll(extractSections(query, sectionPattern));

        return refs;
    }

    /**
     * Determine context requirements based on query analysis
     */
    private ContextRequirements determineContextRequirements(
            QueryType primaryType,
            Set<QueryType> secondaryTypes,
            SpecificReferences specificRefs,
            boolean hasSelectionContext,
            String query) {

        ContentPriority priority = CONTENT_PRIORITIES.getOrDefault(
                primaryType, ContentPriority.builder().build());

        // Merge priorities from secondary types
        for (QueryType secondaryType : secondaryTypes) {
            ContentPriority secondaryPriority = CONTENT_PRIORITIES.get(secondaryType);
            if (secondaryPriority != null) {
                priority = mergePriorities(priority, secondaryPriority, 0.5);
            }
        }

        return ContextRequirements.builder()
                .contentPriority(priority)
                .maxChunks(determineMaxChunks(primaryType, secondaryTypes.size()))
                .includeReferences(primaryType == QueryType.COMPARISON || secondaryTypes.contains(QueryType.COMPARISON))
                .includeAuthorInfo(query.toLowerCase().contains("author")
                        || query.toLowerCase().contains("authors")
                        || query.toLowerCase().contains("researcher")
                        || query.toLowerCase().contains("researchers")
                        || query.toLowerCase().contains("who wrote")
                        || query.toLowerCase().contains("written by")
                        || query.toLowerCase().contains("paper by")
                        || primaryType == QueryType.SUMMARY) // Include authors in summary queries
                .prioritizeRecent(primaryType == QueryType.RESULTS)
                .requiresDeepAnalysis(
                        primaryType == QueryType.TECHNICAL_DETAILS || primaryType == QueryType.METHODOLOGY)
                .specificReferences(specificRefs)
                .needsSelectionContext(hasSelectionContext)
                .build();
    }

    /**
     * Generate optimal prompt strategy
     */
    private PromptStrategy generatePromptStrategy(QueryType primaryType, boolean hasSelectedText) {
        return PromptStrategy.builder()
                .temperature(getOptimalTemperature(primaryType))
                .maxTokens(getOptimalMaxTokens(primaryType))
                .systemPrompt(generateSystemPrompt(primaryType))
                .responseFormat(getResponseFormat(primaryType))
                .includeSourceCitations(true)
                .structuredResponse(primaryType == QueryType.SUMMARY || primaryType == QueryType.COMPARISON)
                .emphasizeAccuracy(primaryType == QueryType.TECHNICAL_DETAILS || primaryType == QueryType.RESULTS)
                .contextualDepth(hasSelectedText ? ContextualDepth.DEEP : ContextualDepth.COMPREHENSIVE)
                .build();
    }

    // Helper methods
    private List<Integer> extractNumbers(String text, Pattern pattern) {
        return pattern.matcher(text)
                .results()
                .map(match -> Integer.parseInt(match.group(1)))
                .collect(Collectors.toList());
    }

    private List<String> extractSections(String text, Pattern pattern) {
        return pattern.matcher(text).results().map(match -> match.group(1)).collect(Collectors.toList());
    }

    private ContentPriority mergePriorities(ContentPriority primary, ContentPriority secondary, double weight) {
        return ContentPriority.builder()
                .abstractWeight(Math.max(primary.abstractWeight, secondary.abstractWeight * weight))
                .introductionWeight(Math.max(primary.introductionWeight, secondary.introductionWeight * weight))
                .methodologyWeight(Math.max(primary.methodologyWeight, secondary.methodologyWeight * weight))
                .resultsWeight(Math.max(primary.resultsWeight, secondary.resultsWeight * weight))
                .conclusionWeight(Math.max(primary.conclusionWeight, secondary.conclusionWeight * weight))
                .technicalWeight(Math.max(primary.technicalWeight, secondary.technicalWeight * weight))
                .figuresWeight(Math.max(primary.figuresWeight, secondary.figuresWeight * weight))
                .tablesWeight(Math.max(primary.tablesWeight, secondary.tablesWeight * weight))
                .equationsWeight(Math.max(primary.equationsWeight, secondary.equationsWeight * weight))
                .referencesWeight(Math.max(primary.referencesWeight, secondary.referencesWeight * weight))
                .build();
    }

    private int determineMaxChunks(QueryType primaryType, int secondaryTypesCount) {
        int baseChunks =
                switch (primaryType) {
                    case SUMMARY -> 6;
                    case METHODOLOGY, TECHNICAL_DETAILS -> 10;
                    case RESULTS -> 8;
                    case COMPARISON -> 12;
                    case SPECIFIC_REFERENCE -> 4;
                    default -> 8;
                };
        return Math.min(baseChunks + secondaryTypesCount * 2, 15);
    }

    private double getOptimalTemperature(QueryType queryType) {
        return switch (queryType) {
            case TECHNICAL_DETAILS, RESULTS, SPECIFIC_REFERENCE -> 0.1; // Very precise
            case METHODOLOGY, COMPARISON -> 0.2; // Mostly precise
            case SUMMARY, CONCEPTUAL -> 0.3; // Slightly creative
            default -> 0.25; // Balanced
        };
    }

    private int getOptimalMaxTokens(QueryType queryType) {
        return switch (queryType) {
            case SUMMARY -> 2000;
            case METHODOLOGY, TECHNICAL_DETAILS -> 3500;
            case RESULTS, COMPARISON -> 3000;
            case SPECIFIC_REFERENCE -> 1500;
            case CONCEPTUAL -> 2500;
            default -> 2500;
        };
    }

    private String generateSystemPrompt(QueryType queryType) {
        return switch (queryType) {
            case SUMMARY -> "You are a research paper summarization expert. Provide comprehensive yet concise summaries that capture key insights, methodology, and findings.";
            case METHODOLOGY -> "You are a methodology analysis expert. Explain research methods, experimental procedures, and implementation details with technical accuracy.";
            case RESULTS -> "You are a results interpretation expert. Analyze experimental results, performance metrics, and research findings with precision.";
            case TECHNICAL_DETAILS -> "You are a technical analysis expert. Explain complex technical concepts, algorithms, and implementations with accuracy and clarity.";
            case COMPARISON -> "You are a comparative analysis expert. Identify similarities, differences, advantages, and trade-offs between approaches or results.";
            case SPECIFIC_REFERENCE -> "You are a document navigation expert. Provide precise information about specific references, figures, tables, or sections.";
            case CONCEPTUAL -> "You are an educational expert. Explain concepts clearly with context, helping users understand underlying principles and ideas.";
            default -> "You are a research paper analysis expert. Provide comprehensive, accurate, and well-structured responses based on the paper content.";
        };
    }

    private ResponseFormat getResponseFormat(QueryType queryType) {
        return switch (queryType) {
            case SUMMARY -> ResponseFormat.STRUCTURED;
            case METHODOLOGY -> ResponseFormat.STEP_BY_STEP;
            case RESULTS -> ResponseFormat.DATA_FOCUSED;
            case TECHNICAL_DETAILS -> ResponseFormat.DETAILED;
            case COMPARISON -> ResponseFormat.COMPARATIVE;
            default -> ResponseFormat.COMPREHENSIVE;
        };
    }

    private double calculateComplexityScore(String query, int secondaryTypesCount) {
        double baseComplexity = query.length() / 100.0; // Length factor
        double typeComplexity = secondaryTypesCount * 0.2; // Multi-faceted questions
        double technicalComplexity = countTechnicalTerms(query) * 0.1;
        return Math.min(baseComplexity + typeComplexity + technicalComplexity, 1.0);
    }

    private int countTechnicalTerms(String query) {
        String[] technicalTerms = {
            "algorithm",
            "implementation",
            "performance",
            "optimization",
            "architecture",
            "framework",
            "methodology",
            "analysis",
            "evaluation"
        };
        int count = 0;
        String lowerQuery = query.toLowerCase();
        for (String term : technicalTerms) {
            if (lowerQuery.contains(term)) count++;
        }
        return count;
    }

    // Data classes
    public enum QueryType {
        SUMMARY,
        METHODOLOGY,
        RESULTS,
        TECHNICAL_DETAILS,
        COMPARISON,
        SPECIFIC_REFERENCE,
        CONCEPTUAL,
        GENERAL
    }

    public enum ResponseFormat {
        STRUCTURED,
        STEP_BY_STEP,
        DATA_FOCUSED,
        DETAILED,
        COMPARATIVE,
        COMPREHENSIVE
    }

    public enum ContextualDepth {
        SHALLOW,
        MODERATE,
        COMPREHENSIVE,
        DEEP
    }

    @Data
    @Builder
    public static class QueryAnalysis {
        private QueryType primaryType;
        private Set<QueryType> secondaryTypes;
        private SpecificReferences specificReferences;
        private ContextRequirements contextRequirements;
        private PromptStrategy promptStrategy;
        private double complexityScore;
    }

    @Data
    @Builder
    public static class ContextRequirements {
        private ContentPriority contentPriority;
        private int maxChunks;
        private boolean includeReferences;
        private boolean includeAuthorInfo;
        private boolean prioritizeRecent;
        private boolean requiresDeepAnalysis;
        private SpecificReferences specificReferences;
        private boolean needsSelectionContext;
    }

    @Data
    @Builder
    public static class ContentPriority {
        @Builder.Default
        private double abstractWeight = 0.0;

        @Builder.Default
        private double introductionWeight = 0.0;

        @Builder.Default
        private double methodologyWeight = 0.0;

        @Builder.Default
        private double resultsWeight = 0.0;

        @Builder.Default
        private double conclusionWeight = 0.0;

        @Builder.Default
        private double technicalWeight = 0.0;

        @Builder.Default
        private double figuresWeight = 0.0;

        @Builder.Default
        private double tablesWeight = 0.0;

        @Builder.Default
        private double equationsWeight = 0.0;

        @Builder.Default
        private double referencesWeight = 0.0;

        @Builder.Default
        private double codeBlocksWeight = 0.0;

        @Builder.Default
        private double experimentsWeight = 0.0;

        @Builder.Default
        private double specificRefWeight = 0.0;

        @Builder.Default
        private double contextualWeight = 0.0;
    }

    @Data
    @Builder
    public static class PromptStrategy {
        private double temperature;
        private int maxTokens;
        private String systemPrompt;
        private ResponseFormat responseFormat;
        private boolean includeSourceCitations;
        private boolean structuredResponse;
        private boolean emphasizeAccuracy;
        private ContextualDepth contextualDepth;
    }

    @Data
    public static class SpecificReferences {
        public List<Integer> pages = new ArrayList<>();
        public List<Integer> figures = new ArrayList<>();
        public List<Integer> tables = new ArrayList<>();
        public List<Integer> equations = new ArrayList<>();
        public List<String> sections = new ArrayList<>();
        public List<String> authors = new ArrayList<>();
    }
}
