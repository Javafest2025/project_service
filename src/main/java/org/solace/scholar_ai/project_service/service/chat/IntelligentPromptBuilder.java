package org.solace.scholar_ai.project_service.service.chat;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.solace.scholar_ai.project_service.model.chat.ChatMessage;
import org.solace.scholar_ai.project_service.model.chat.ContentChunk;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.springframework.stereotype.Service;

/**
 * Intelligent Prompt Builder that creates optimized prompts for different query types
 * Uses query analysis to structure prompts for maximum AI accuracy and relevance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligentPromptBuilder {

    /**
     * Build comprehensive prompt optimized for the specific query type and context
     */
    public String buildOptimizedPrompt(
            PaperExtraction extraction,
            List<ContentChunk> relevantChunks,
            List<ChatMessage> conversationHistory,
            String userQuery,
            String selectedText,
            IntelligentQueryStrategy.QueryAnalysis analysis,
            List<Author> authors) {

        StringBuilder prompt = new StringBuilder();

        // 1. System role and instructions based on query type
        prompt.append(buildSystemInstructions(analysis));
        prompt.append("\n\n");

        // 2. Paper context and metadata
        prompt.append(buildPaperContext(extraction, authors));
        prompt.append("\n\n");

        // 3. Relevant content chunks organized by priority
        prompt.append(buildContentContext(relevantChunks, analysis));
        prompt.append("\n\n");

        // 4. Conversation history (if exists)
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append(buildConversationContext(conversationHistory));
            prompt.append("\n\n");
        }

        // 5. Selected text context (highest priority)
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            prompt.append(buildSelectedTextContext(selectedText));
            prompt.append("\n\n");
        }

        // 6. User query with specific instructions
        prompt.append(buildQueryContext(userQuery, analysis));
        prompt.append("\n\n");

        // 7. Response formatting instructions
        prompt.append(buildResponseInstructions(analysis));

        String finalPrompt = prompt.toString();
        log.debug(
                "Built optimized prompt for query type: {} (length: {} chars)",
                analysis.getPrimaryType(),
                finalPrompt.length());

        return finalPrompt;
    }

    /**
     * Build optimized prompt with AI-determined data requirements
     * This method bridges the new AI requirement analysis with the existing prompt building
     */
    public String buildOptimizedPromptWithRequirements(
            PaperExtraction extraction,
            List<ContentChunk> relevantChunks,
            List<ChatMessage> conversationHistory,
            String userQuery,
            String selectedText,
            QueryRequirementAnalysisService.DataRequirement[] dataRequirements,
            List<Author> authors) {

        StringBuilder prompt = new StringBuilder();

        // 1. System instructions for comprehensive analysis
        prompt.append("You are an advanced research paper analysis expert. ");
        prompt.append(
                "Provide detailed, accurate, and well-structured responses based on the provided paper content. ");
        prompt.append("Always reference specific sections when making claims. ");
        prompt.append("If you mention authors, ALWAYS include their full names as provided in the paper.\n\n");

        // 2. Paper context with authors (if needed)
        prompt.append(buildPaperContextWithAuthors(extraction, authors));
        prompt.append("\n\n");

        // 3. Relevant content chunks
        if (relevantChunks != null && !relevantChunks.isEmpty()) {
            prompt.append("RELEVANT PAPER CONTENT:\n");
            for (ContentChunk chunk : relevantChunks) {
                prompt.append("=== ").append(chunk.getSource()).append(" ===\n");
                prompt.append(chunk.getContent()).append("\n\n");
            }
        }

        // 4. Conversation history (if exists)
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            prompt.append("CONVERSATION HISTORY:\n");
            for (ChatMessage msg : conversationHistory) {
                prompt.append(msg.getRole().toString())
                        .append(": ")
                        .append(msg.getContent())
                        .append("\n");
            }
            prompt.append("\n");
        }

        // 5. Selected text context (highest priority)
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            prompt.append("USER SELECTED TEXT: ").append(selectedText).append("\n\n");
        }

        // 6. User query
        prompt.append("USER QUESTION: ").append(userQuery).append("\n\n");

        // 7. Response instructions
        prompt.append("Please provide a comprehensive answer based on the paper content. ");
        prompt.append("When mentioning authors, include their full names. ");
        prompt.append("Reference specific sections or pages when possible. ");
        prompt.append("Be accurate and detailed in your response.");

        String finalPrompt = prompt.toString();
        log.debug("Built AI-requirements optimized prompt (length: {} chars)", finalPrompt.length());

        return finalPrompt;
    }

    /**
     * Build paper context section including authors when available
     */
    private String buildPaperContextWithAuthors(PaperExtraction extraction, List<Author> authors) {
        StringBuilder context = new StringBuilder();

        context.append("PAPER INFORMATION:\n");

        if (extraction.getTitle() != null) {
            context.append("Title: ").append(extraction.getTitle()).append("\n");
        }

        if (authors != null && !authors.isEmpty()) {
            context.append("Authors: ");
            String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
            context.append(authorNames).append("\n");
        }

        if (extraction.getAbstractText() != null) {
            context.append("Abstract: ").append(extraction.getAbstractText()).append("\n");
        }

        return context.toString();
    }

    /**
     * Build system instructions based on query type
     */
    private String buildSystemInstructions(IntelligentQueryStrategy.QueryAnalysis analysis) {
        StringBuilder instructions = new StringBuilder();

        instructions.append("You are an advanced research paper analysis expert. ");
        instructions.append(analysis.getPromptStrategy().getSystemPrompt());
        instructions.append("\n\n");

        // Add specific instructions based on query type
        switch (analysis.getPrimaryType()) {
            case SUMMARY:
                instructions
                        .append("Focus on providing a comprehensive yet concise summary. ")
                        .append("Include key contributions, methodology highlights, and main findings. ")
                        .append("Structure your response with clear sections.");
                break;
            case METHODOLOGY:
                instructions
                        .append("Provide detailed explanations of methods and procedures. ")
                        .append("Include step-by-step breakdowns where appropriate. ")
                        .append("Reference specific techniques and their rationale.");
                break;
            case RESULTS:
                instructions
                        .append("Focus on quantitative results, performance metrics, and outcomes. ")
                        .append("Include specific numbers, percentages, and comparisons. ")
                        .append("Explain the significance of the findings.");
                break;
            case TECHNICAL_DETAILS:
                instructions
                        .append("Provide in-depth technical explanations with precision. ")
                        .append("Include algorithms, formulas, and implementation details. ")
                        .append("Maintain technical accuracy throughout.");
                break;
            case COMPARISON:
                instructions
                        .append("Clearly identify similarities and differences. ")
                        .append("Provide balanced analysis of advantages and disadvantages. ")
                        .append("Use structured comparisons where helpful.");
                break;
            case SPECIFIC_REFERENCE:
                instructions
                        .append("Focus precisely on the referenced content. ")
                        .append("Provide detailed explanation of the specific element. ")
                        .append("Include relevant context where necessary.");
                break;
            case CONCEPTUAL:
                instructions
                        .append("Explain concepts clearly and build understanding progressively. ")
                        .append("Use analogies and examples where helpful. ")
                        .append("Define technical terms and provide context.");
                break;
            default:
                instructions
                        .append("Provide comprehensive, accurate, and well-structured analysis. ")
                        .append("Focus on relevance and clarity.");
        }

        return instructions.toString();
    }

    /**
     * Build paper context and metadata
     */
    private String buildPaperContext(PaperExtraction extraction, List<Author> authors) {
        StringBuilder context = new StringBuilder();
        context.append("=== PAPER CONTEXT ===\n");

        if (extraction.getTitle() != null) {
            context.append("Title: ").append(extraction.getTitle()).append("\n");
        }

        // Add author information if available
        if (authors != null && !authors.isEmpty()) {
            context.append("Authors: ");
            String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
            context.append(authorNames).append("\n");

            // Add author affiliations if available
            String affiliations = authors.stream()
                    .map(Author::getPrimaryAffiliation)
                    .filter(affiliation ->
                            affiliation != null && !affiliation.trim().isEmpty())
                    .distinct()
                    .collect(Collectors.joining("; "));
            if (!affiliations.isEmpty()) {
                context.append("Affiliations: ").append(affiliations).append("\n");
            }
        }

        if (extraction.getAbstractText() != null) {
            context.append("Abstract: ")
                    .append(truncateText(extraction.getAbstractText(), 500))
                    .append("\n");
        }

        // Add structural information
        context.append("Paper Structure:\n");
        context.append("- Total Sections: ")
                .append(extraction.getSections().size())
                .append("\n");
        context.append("- Figures: ").append(extraction.getFigures().size()).append("\n");
        context.append("- Tables: ").append(extraction.getTables().size()).append("\n");
        context.append("- Equations: ").append(extraction.getEquations().size()).append("\n");
        context.append("- References: ")
                .append(extraction.getReferences().size())
                .append("\n");

        return context.toString();
    }

    /**
     * Build content context organized by relevance and type
     */
    private String buildContentContext(
            List<ContentChunk> relevantChunks, IntelligentQueryStrategy.QueryAnalysis analysis) {

        StringBuilder context = new StringBuilder();
        context.append("=== RELEVANT CONTENT ===\n");

        // Group chunks by type for better organization
        var chunksByType = relevantChunks.stream().collect(Collectors.groupingBy(ContentChunk::getType));

        // Process in priority order based on query type
        String[] priorityOrder = getPriorityOrder(analysis.getPrimaryType());

        for (String type : priorityOrder) {
            List<ContentChunk> chunks = chunksByType.get(type);
            if (chunks != null && !chunks.isEmpty()) {
                context.append("\n--- ").append(type.toUpperCase()).append(" CONTENT ---\n");
                for (ContentChunk chunk : chunks) {
                    context.append("Source: ").append(chunk.getSource()).append("\n");
                    context.append("Content: ").append(chunk.getContent()).append("\n");
                    if (chunk.getPageNumber() != null) {
                        context.append("Page: ").append(chunk.getPageNumber()).append("\n");
                    }
                    context.append("Relevance: ")
                            .append(String.format("%.2f", chunk.getRelevanceScore()))
                            .append("\n\n");
                }
            }
        }

        return context.toString();
    }

    /**
     * Build conversation context from chat history
     */
    private String buildConversationContext(List<ChatMessage> conversationHistory) {
        StringBuilder context = new StringBuilder();
        context.append("=== CONVERSATION HISTORY ===\n");

        conversationHistory.forEach(message -> {
            context.append(message.getRole().toString())
                    .append(": ")
                    .append(truncateText(message.getContent(), 300))
                    .append("\n");
        });

        return context.toString();
    }

    /**
     * Build selected text context
     */
    private String buildSelectedTextContext(String selectedText) {
        StringBuilder context = new StringBuilder();
        context.append("=== USER SELECTED TEXT ===\n");
        context.append("The user has specifically selected this text for analysis:\n");
        context.append("\"").append(selectedText).append("\"\n");
        context.append("Please pay special attention to this selected content in your response.\n");

        return context.toString();
    }

    /**
     * Build query context with specific instructions
     */
    private String buildQueryContext(String userQuery, IntelligentQueryStrategy.QueryAnalysis analysis) {
        StringBuilder context = new StringBuilder();
        context.append("=== USER QUERY ===\n");
        context.append("Query Type: ").append(analysis.getPrimaryType()).append("\n");
        context.append("Complexity Score: ")
                .append(String.format("%.2f", analysis.getComplexityScore()))
                .append("\n");
        context.append("User Question: ").append(userQuery).append("\n");

        // Add specific query handling instructions
        if (!analysis.getSecondaryTypes().isEmpty()) {
            context.append("Secondary Query Types: ")
                    .append(analysis.getSecondaryTypes().stream()
                            .map(Enum::toString)
                            .collect(Collectors.joining(", ")))
                    .append("\n");
            context.append("Note: This is a multi-faceted question. Address all relevant aspects.\n");
        }

        if (analysis.getSpecificReferences() != null && hasSpecificReferences(analysis.getSpecificReferences())) {
            context.append("Specific References Mentioned: ");
            IntelligentQueryStrategy.SpecificReferences refs = analysis.getSpecificReferences();
            if (!refs.figures.isEmpty())
                context.append("Figures ").append(refs.figures).append(" ");
            if (!refs.tables.isEmpty())
                context.append("Tables ").append(refs.tables).append(" ");
            if (!refs.pages.isEmpty())
                context.append("Pages ").append(refs.pages).append(" ");
            if (!refs.equations.isEmpty())
                context.append("Equations ").append(refs.equations).append(" ");
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * Build response formatting instructions
     */
    private String buildResponseInstructions(IntelligentQueryStrategy.QueryAnalysis analysis) {
        StringBuilder instructions = new StringBuilder();
        instructions.append("=== RESPONSE INSTRUCTIONS ===\n");

        instructions
                .append("Response Format: ")
                .append(analysis.getPromptStrategy().getResponseFormat())
                .append("\n");

        if (analysis.getPromptStrategy().isStructuredResponse()) {
            instructions.append("Use clear headings and bullet points for organization.\n");
        }

        if (analysis.getPromptStrategy().isEmphasizeAccuracy()) {
            instructions.append("Prioritize factual accuracy over creativity.\n");
        }

        if (analysis.getPromptStrategy().isIncludeSourceCitations()) {
            instructions.append("Include references to specific sections, figures, or pages when relevant.\n");
        }

        instructions
                .append("Contextual Depth: ")
                .append(analysis.getPromptStrategy().getContextualDepth())
                .append("\n");

        // Response length guidance
        switch (analysis.getPrimaryType()) {
            case SUMMARY:
                instructions.append("Aim for a comprehensive but concise response (300-800 words).\n");
                break;
            case TECHNICAL_DETAILS:
                instructions.append("Provide detailed technical explanation as needed (500-1500 words).\n");
                break;
            case SPECIFIC_REFERENCE:
                instructions.append("Focus on precise, targeted response (200-600 words).\n");
                break;
            default:
                instructions.append("Provide thorough but focused response (400-1000 words).\n");
        }

        instructions
                .append("\nIMPORTANT: Base your response strictly on the provided paper content. ")
                .append("If information is not available in the content, clearly state this limitation. ")
                .append("Always maintain accuracy and avoid speculation beyond the paper's scope.");

        return instructions.toString();
    }

    // Helper methods
    private String[] getPriorityOrder(IntelligentQueryStrategy.QueryType queryType) {
        return switch (queryType) {
            case SUMMARY -> new String[] {
                "selected_text", "abstract", "introduction", "conclusion", "results", "methodology"
            };
            case METHODOLOGY -> new String[] {"selected_text", "methodology", "technical", "introduction", "experiments"
            };
            case RESULTS -> new String[] {"selected_text", "results", "figure", "table", "experiments", "conclusion"};
            case TECHNICAL_DETAILS -> new String[] {"selected_text", "technical", "equation", "methodology", "figure"};
            case COMPARISON -> new String[] {"selected_text", "results", "reference", "methodology", "conclusion"};
            case SPECIFIC_REFERENCE -> new String[] {
                "selected_text", "specific_figure", "specific_table", "specific_page"
            };
            default -> new String[] {"selected_text", "introduction", "methodology", "results", "conclusion"};
        };
    }

    private boolean hasSpecificReferences(IntelligentQueryStrategy.SpecificReferences refs) {
        return !refs.figures.isEmpty()
                || !refs.tables.isEmpty()
                || !refs.pages.isEmpty()
                || !refs.equations.isEmpty()
                || !refs.sections.isEmpty();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
