package org.solace.scholar_ai.project_service.service.chat;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest;
import org.solace.scholar_ai.project_service.dto.response.chat.PaperChatResponse;
import org.solace.scholar_ai.project_service.exception.PaperNotExtractedException;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.model.chat.ChatMessage;
import org.solace.scholar_ai.project_service.model.chat.ChatSession;
import org.solace.scholar_ai.project_service.model.extraction.ExtractedFigure;
import org.solace.scholar_ai.project_service.model.extraction.ExtractedParagraph;
import org.solace.scholar_ai.project_service.model.extraction.ExtractedSection;
import org.solace.scholar_ai.project_service.model.extraction.ExtractedTable;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.chat.ChatMessageRepository;
import org.solace.scholar_ai.project_service.repository.chat.ChatSessionRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.service.ai.GeminiGeneralService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for contextual Q&A chat with papers using extracted content
 * Implements RAG (Retrieval Augmented Generation) for accurate responses
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperContextChatService {

    private final PaperRepository paperRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GeminiGeneralService geminiService;

    // Configuration constants
    private static final int MAX_CONTEXT_CHUNKS = 8;
    private static final int MAX_CONVERSATION_HISTORY = 3;
    private static final double RELEVANCE_THRESHOLD = 0.1;

    /**
     * Main method for chatting with a paper using extracted content as context
     */
    @Transactional
    public PaperChatResponse chatWithPaper(UUID paperId, PaperChatRequest request) {
        log.info(
                "Processing chat request for paper: {} with message: '{}', selectedText: '{}'",
                paperId,
                truncateMessage(request.getMessage()),
                request.getSelectedText() != null ? truncateMessage(request.getSelectedText()) : "None");

        try {
            // 1. Validate paper and extraction
            Paper paper = validatePaperAndExtraction(paperId);

            // 2. Get or create chat session
            ChatSession session = getOrCreateChatSession(paperId, request.getSessionId());

            // 3. Store user message
            storeUserMessage(session, request.getMessage());

            // 4. Retrieve relevant content using enhanced RAG
            List<ContentChunk> relevantChunks = retrieveRelevantContent(
                    paper.getPaperExtraction(),
                    request.getMessage(),
                    request.getSelectedText(),
                    request.getSelectionContext());

            // 5. Get recent conversation history
            List<ChatMessage> recentHistory = getRecentChatHistory(session.getId());

            // 6. Build comprehensive prompt for Gemini
            String prompt = buildComprehensivePrompt(
                    paper.getPaperExtraction(),
                    relevantChunks,
                    recentHistory,
                    request.getMessage(),
                    request.getSelectedText());

            // 7. Generate detailed response using Gemini
            String aiResponse = geminiService.generateResponse(prompt, 0.3, 3000); // Lower temperature, higher token
            // limit

            // 8. Store assistant response
            ChatMessage assistantMessage = storeAssistantMessage(session, aiResponse);

            // 9. Build and return comprehensive response
            return buildComprehensiveChatResponse(
                    session, assistantMessage, relevantChunks, paper.getPaperExtraction());

        } catch (Exception e) {
            log.error("Error generating chat response for paper: {}", paperId, e);

            // Store error message
            String errorResponse =
                    "I apologize, but I encountered an error while analyzing the paper. Please try rephrasing your question or try again later.";
            ChatSession session = getOrCreateChatSession(paperId, request.getSessionId());
            ChatMessage errorMessage = storeAssistantMessage(session, errorResponse);

            return PaperChatResponse.builder()
                    .sessionId(session.getId())
                    .response(errorResponse)
                    .timestamp(errorMessage.getTimestamp())
                    .success(false)
                    .error("Error generating response: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Validate paper exists and is extracted
     */
    private Paper validatePaperAndExtraction(UUID paperId) {
        Paper paper = paperRepository
                .findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException("Paper not found with ID: " + paperId));

        if (!Boolean.TRUE.equals(paper.getIsExtracted()) || paper.getPaperExtraction() == null) {
            throw new PaperNotExtractedException(
                    "Paper has not been extracted yet. Please wait for extraction to complete.");
        }

        return paper;
    }

    /**
     * Get existing chat session or create a new one
     */
    private ChatSession getOrCreateChatSession(UUID paperId, UUID sessionId) {
        if (sessionId != null) {
            Optional<ChatSession> existingSession = chatSessionRepository.findById(sessionId);
            if (existingSession.isPresent()) {
                return existingSession.get();
            }
        }

        // Generate session title from first message
        String sessionTitle = "New Chat"; // Could be enhanced based on first message

        // Create new session
        ChatSession newSession = ChatSession.builder()
                .id(UUID.randomUUID())
                .paperId(paperId)
                .title(sessionTitle != null ? sessionTitle : "New Chat")
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .updatedAt(Instant.now())
                .messageCount(0)
                .build();

        return chatSessionRepository.save(newSession);
    }

    /**
     * Store user message in database
     */
    private ChatMessage storeUserMessage(ChatSession session, String message) {
        ChatMessage userMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(session.getId())
                .role(ChatMessage.Role.USER)
                .content(message)
                .timestamp(Instant.now())
                .build();

        session.setMessageCount(session.getMessageCount() + 1);
        chatSessionRepository.save(session);

        return chatMessageRepository.save(userMessage);
    }

    /**
     * Store assistant message in database
     */
    private ChatMessage storeAssistantMessage(ChatSession session, String response) {
        ChatMessage assistantMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(session.getId())
                .role(ChatMessage.Role.ASSISTANT)
                .content(response)
                .timestamp(Instant.now())
                .build();

        session.setMessageCount(session.getMessageCount() + 1);
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);

        return chatMessageRepository.save(assistantMessage);
    }

    /**
     * Retrieve relevant content chunks using enhanced RAG approach with selected
     * text context
     */
    private List<ContentChunk> retrieveRelevantContent(
            PaperExtraction extraction,
            String question,
            String selectedText,
            PaperChatRequest.SelectionContext selectionContext) {
        log.debug(
                "Retrieving relevant content for question: {}, selectedText: {}",
                truncateMessage(question),
                selectedText != null ? truncateMessage(selectedText) : "None");

        // Extract keywords from question and selected text
        Set<String> questionKeywords = extractKeywords(question.toLowerCase());
        Set<String> selectionKeywords =
                selectedText != null ? extractKeywords(selectedText.toLowerCase()) : new HashSet<>();

        // Combine keywords for better matching
        Set<String> allKeywords = new HashSet<>(questionKeywords);
        allKeywords.addAll(selectionKeywords);

        // Check for specific references (pages, figures, sections)
        SpecificReferences specificRefs = extractSpecificReferences(question);

        // If we have selection context, add it to specific references
        if (selectionContext != null && selectionContext.getPageNumber() != null) {
            specificRefs.pages.add(selectionContext.getPageNumber());
        }

        List<ContentChunk> allChunks = new ArrayList<>();

        // 1. HIGHEST PRIORITY: Selected text context if provided
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            allChunks.add(ContentChunk.builder()
                    .content("SELECTED TEXT CONTEXT: " + selectedText)
                    .source("User-selected text"
                            + (selectionContext != null && selectionContext.getPageNumber() != null
                                    ? " (Page " + selectionContext.getPageNumber() + ")"
                                    : ""))
                    .type("selected_text")
                    .relevanceScore(1.0) // Highest priority
                    .pageNumber(selectionContext != null ? selectionContext.getPageNumber() : null)
                    .build());
        }

        // 2. Add specific references (high priority)
        allChunks.addAll(getSpecificReferences(extraction, specificRefs));

        // 3. Retrieve relevant sections based on enhanced keyword matching
        extraction.getSections().forEach(section -> {
            String sectionContent = extractSectionContent(section);
            if (sectionContent != null && !sectionContent.isEmpty()) {
                double relevance = calculateEnhancedRelevanceScore(sectionContent, allKeywords, selectedText);
                if (relevance > RELEVANCE_THRESHOLD) {
                    allChunks.add(ContentChunk.builder()
                            .content(sectionContent)
                            .source("Section: " + section.getTitle())
                            .type("section")
                            .relevanceScore(relevance)
                            .pageNumber(section.getPageStart())
                            .build());
                }
            }
        });

        // 4. Process figures with enhanced relevance
        extraction.getFigures().forEach(figure -> {
            String figureText = buildFigureText(figure);
            double relevance = calculateEnhancedRelevanceScore(figureText, allKeywords, selectedText);
            if (relevance > RELEVANCE_THRESHOLD || hasSpecificFigureReference(question, figure)) {
                allChunks.add(ContentChunk.builder()
                        .content("Figure " + figure.getLabel() + ": " + figure.getCaption()
                                + (figure.getOcrText() != null ? "\nOCR Text: " + figure.getOcrText() : ""))
                        .source("Figure " + figure.getLabel() + " (Page " + figure.getPage() + ")")
                        .type("figure")
                        .relevanceScore(relevance)
                        .pageNumber(figure.getPage())
                        .build());
            }
        });

        // 5. Process tables with enhanced relevance
        extraction.getTables().forEach(table -> {
            String tableText = buildTableText(table);
            double relevance = calculateEnhancedRelevanceScore(tableText, allKeywords, selectedText);
            if (relevance > RELEVANCE_THRESHOLD || hasSpecificTableReference(question, table)) {
                allChunks.add(ContentChunk.builder()
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

        // 6. Process equations if relevant
        extraction.getEquations().forEach(equation -> {
            if (equation.getLatex() != null || equation.getLabel() != null) {
                String equationText = (equation.getLabel() != null ? equation.getLabel() : "")
                        + (equation.getLatex() != null ? " LaTeX: " + equation.getLatex() : "");
                double relevance = calculateEnhancedRelevanceScore(equationText, allKeywords, selectedText);
                if (relevance > RELEVANCE_THRESHOLD
                        || questionContainsKeywords(question, "equation", "formula", "math")) {
                    allChunks.add(ContentChunk.builder()
                            .content("Equation " + equation.getEquationId() + ": " + equationText)
                            .source("Equation " + equation.getEquationId() + " (Page " + equation.getPage() + ")")
                            .type("equation")
                            .relevanceScore(relevance)
                            .pageNumber(equation.getPage())
                            .build());
                }
            }
        });

        // 7. Sort by relevance and limit results (selected text always first)
        List<ContentChunk> topChunks = allChunks.stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(MAX_CONTEXT_CHUNKS)
                .collect(Collectors.toList());

        log.debug(
                "Selected {} relevant content chunks (including {} with selected text)",
                topChunks.size(),
                selectedText != null ? 1 : 0);
        return topChunks;
    }

    /**
     * Extract content from section by aggregating paragraphs
     */
    private String extractSectionContent(ExtractedSection section) {
        if (section.getParagraphs() == null || section.getParagraphs().isEmpty()) {
            return section.getTitle(); // Fallback to just title if no paragraphs
        }

        return section.getParagraphs().stream()
                .map(ExtractedParagraph::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    /**
     * Build searchable text from figure
     */
    private String buildFigureText(ExtractedFigure figure) {
        StringBuilder text = new StringBuilder();
        if (figure.getCaption() != null) {
            text.append(figure.getCaption());
        }
        if (figure.getOcrText() != null) {
            text.append(" ").append(figure.getOcrText());
        }
        return text.toString();
    }

    /**
     * Build searchable text from table
     */
    private String buildTableText(ExtractedTable table) {
        StringBuilder text = new StringBuilder();
        if (table.getCaption() != null) {
            text.append(table.getCaption());
        }
        if (table.getHeaders() != null) {
            text.append(" ").append(table.getHeaders());
        }
        if (table.getRows() != null) {
            text.append(" ").append(truncateText(table.getRows(), 200));
        }
        return text.toString();
    }

    /**
     * Extract keywords from text for relevance scoring
     */
    private Set<String> extractKeywords(String text) {
        // Simple keyword extraction - can be enhanced with NLP libraries
        Set<String> stopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by", "is", "are",
                "was", "were", "what", "how", "why", "when", "where", "who", "which", "can", "could", "would", "should",
                "this", "that", "these", "those");

        return Arrays.stream(text.split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .collect(Collectors.toSet());
    }

    /**
     * Calculate enhanced relevance score with selected text context
     */
    private double calculateEnhancedRelevanceScore(String content, Set<String> keywords, String selectedText) {
        if (content == null || content.isEmpty()) return 0.0;

        String contentLower = content.toLowerCase();
        double score = 0.0;

        // Basic keyword matching
        for (String keyword : keywords) {
            if (contentLower.contains(keyword)) {
                score += 1.0;
            }
        }

        // Boost score if content is similar to selected text
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            String selectedLower = selectedText.toLowerCase();

            // Check for common words/phrases
            Set<String> selectedWords = extractKeywords(selectedLower);
            Set<String> contentWords = extractKeywords(contentLower);

            int commonWords = 0;
            for (String word : selectedWords) {
                if (contentWords.contains(word)) {
                    commonWords++;
                }
            }

            // Calculate similarity boost
            if (!selectedWords.isEmpty()) {
                double similarity = (double) commonWords / selectedWords.size();
                score += similarity * 3.0; // Boost for similarity to selected text
            }

            // Additional boost for exact phrase matches
            String[] selectedPhrases = selectedLower.split("[.!?]");
            for (String phrase : selectedPhrases) {
                phrase = phrase.trim();
                if (phrase.length() > 10 && contentLower.contains(phrase)) {
                    score += 2.0; // High boost for phrase matches
                }
            }
        }

        // Normalize score
        return Math.min(score / Math.max(keywords.size(), 1), 10.0);
    }

    /**
     * Check if question contains specific keywords
     */
    private boolean questionContainsKeywords(String question, String... keywords) {
        String questionLower = question.toLowerCase();
        for (String keyword : keywords) {
            if (questionLower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for specific figure references in question
     */
    private boolean hasSpecificFigureReference(String question, ExtractedFigure figure) {
        String questionLower = question.toLowerCase();
        return questionLower.contains("figure " + figure.getLabel())
                || questionLower.contains("fig " + figure.getLabel())
                || questionLower.contains("figure" + figure.getLabel())
                || (figure.getCaption() != null
                        && extractKeywords(questionLower).stream()
                                .anyMatch(keyword ->
                                        figure.getCaption().toLowerCase().contains(keyword)));
    }

    /**
     * Check for specific table references in question
     */
    private boolean hasSpecificTableReference(String question, ExtractedTable table) {
        String questionLower = question.toLowerCase();
        return questionLower.contains("table " + table.getLabel())
                || questionLower.contains("table" + table.getLabel())
                || (table.getCaption() != null
                        && extractKeywords(questionLower).stream()
                                .anyMatch(keyword ->
                                        table.getCaption().toLowerCase().contains(keyword)));
    }

    /**
     * Extract specific references from question (figures, tables, pages, sections)
     */
    private SpecificReferences extractSpecificReferences(String question) {
        SpecificReferences refs = new SpecificReferences();

        // Figure references
        Pattern figurePattern = Pattern.compile("(?i)figure\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher figureMatcher = figurePattern.matcher(question);
        while (figureMatcher.find()) {
            refs.figures.add(Integer.parseInt(figureMatcher.group(1)));
        }

        // Table references
        Pattern tablePattern = Pattern.compile("(?i)table\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(question);
        while (tableMatcher.find()) {
            refs.tables.add(Integer.parseInt(tableMatcher.group(1)));
        }

        // Page references
        Pattern pagePattern = Pattern.compile("(?i)page\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher pageMatcher = pagePattern.matcher(question);
        while (pageMatcher.find()) {
            refs.pages.add(Integer.parseInt(pageMatcher.group(1)));
        }

        // Section references
        String[] sectionKeywords = {
            "introduction", "conclusion", "abstract", "methodology", "results", "discussion", "references"
        };
        for (String keyword : sectionKeywords) {
            if (question.toLowerCase().contains(keyword)) {
                refs.sections.add(keyword);
            }
        }

        return refs;
    }

    /**
     * Get content for specific references mentioned in the question
     */
    private List<ContentChunk> getSpecificReferences(PaperExtraction extraction, SpecificReferences refs) {
        List<ContentChunk> chunks = new ArrayList<>();

        // Add specific figures
        refs.figures.forEach(figNum -> {
            extraction.getFigures().stream()
                    .filter(fig -> fig.getLabel() != null && fig.getLabel().contains(figNum.toString()))
                    .forEach(fig -> chunks.add(ContentChunk.builder()
                            .content("Figure " + fig.getLabel() + ": " + fig.getCaption())
                            .source("Figure " + fig.getLabel() + " (Page " + fig.getPage() + ")")
                            .type("figure")
                            .relevanceScore(1.0) // Highest relevance for specific references
                            .pageNumber(fig.getPage())
                            .build()));
        });

        // Add specific tables
        refs.tables.forEach(tableNum -> {
            extraction.getTables().stream()
                    .filter(table ->
                            table.getLabel() != null && table.getLabel().contains(tableNum.toString()))
                    .forEach(table -> chunks.add(ContentChunk.builder()
                            .content("Table " + table.getLabel() + ": " + table.getCaption()
                                    + (table.getHeaders() != null ? "\nHeaders: " + table.getHeaders() : "")
                                    + (table.getRows() != null ? "\nData: " + truncateText(table.getRows(), 300) : ""))
                            .source("Table " + table.getLabel() + " (Page " + table.getPage() + ")")
                            .type("table")
                            .relevanceScore(1.0)
                            .pageNumber(table.getPage())
                            .build()));
        });

        // Add specific sections
        refs.sections.forEach(sectionKeyword -> {
            extraction.getSections().stream()
                    .filter(section -> section.getTitle() != null
                            && section.getTitle().toLowerCase().contains(sectionKeyword))
                    .forEach(section -> {
                        String content = extractSectionContent(section);
                        if (content != null && !content.isEmpty()) {
                            chunks.add(ContentChunk.builder()
                                    .content(content)
                                    .source("Section: " + section.getTitle())
                                    .type("section")
                                    .relevanceScore(1.0)
                                    .pageNumber(section.getPageStart())
                                    .build());
                        }
                    });
        });

        return chunks;
    }

    /**
     * Get recent chat history for conversation context
     */
    private List<ChatMessage> getRecentChatHistory(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampDesc(sessionId).stream()
                .limit(MAX_CONVERSATION_HISTORY * 2) // Get pairs of Q&A
                .collect(Collectors.toList());
    }

    /**
     * Calculate confidence score based on relevance of retrieved chunks
     */
    private Double calculateConfidenceScore(List<ContentChunk> chunks) {
        if (chunks.isEmpty()) return 0.5;

        double averageRelevance = chunks.stream()
                .mapToDouble(ContentChunk::getRelevanceScore)
                .average()
                .orElse(0.5);

        // Factor in number of chunks (more relevant content = higher confidence)
        double chunkBonus = Math.min(chunks.size() * 0.1, 0.3);

        return Math.min(averageRelevance + chunkBonus, 1.0);
    }

    /**
     * Utility method to truncate text for logging and token optimization
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    /**
     * Truncate message for logging
     */
    private String truncateMessage(String message) {
        return truncateText(message, 100);
    }

    /**
     * Inner class to represent content chunks for RAG
     */
    @lombok.Builder
    @lombok.Getter
    private static class ContentChunk {
        private String content;
        private String source;
        private String type;
        private double relevanceScore;
        private Integer pageNumber;
    }

    /**
     * Build comprehensive chat response with enhanced context metadata
     */
    private PaperChatResponse buildComprehensiveChatResponse(
            ChatSession session,
            ChatMessage assistantMessage,
            List<ContentChunk> relevantChunks,
            PaperExtraction extraction) {

        // Separate content sources by type for better organization
        List<String> sectionsUsed = relevantChunks.stream()
                .filter(chunk -> "section".equals(chunk.getType()))
                .map(ContentChunk::getSource)
                .collect(Collectors.toList());

        List<String> figuresReferenced = relevantChunks.stream()
                .filter(chunk -> "figure".equals(chunk.getType()))
                .map(ContentChunk::getSource)
                .collect(Collectors.toList());

        List<String> tablesReferenced = relevantChunks.stream()
                .filter(chunk -> "table".equals(chunk.getType()))
                .map(ContentChunk::getSource)
                .collect(Collectors.toList());

        List<String> equationsReferenced = relevantChunks.stream()
                .filter(chunk -> "equation".equals(chunk.getType()))
                .map(ContentChunk::getSource)
                .collect(Collectors.toList());

        // Build comprehensive content sources
        List<String> contentSources = new ArrayList<>();

        // Add selected text source if present
        relevantChunks.stream()
                .filter(chunk -> "selected_text".equals(chunk.getType()))
                .findFirst()
                .ifPresent(chunk -> contentSources.add(chunk.getSource()));

        // Add other content sources
        relevantChunks.stream()
                .filter(chunk -> !"selected_text".equals(chunk.getType()))
                .forEach(chunk -> contentSources.add(chunk.getSource()));

        PaperChatResponse.ContextMetadata contextMetadata = PaperChatResponse.ContextMetadata.builder()
                .sectionsUsed(sectionsUsed)
                .figuresReferenced(figuresReferenced)
                .tablesReferenced(tablesReferenced)
                .equationsUsed(equationsReferenced)
                .pagesReferenced(relevantChunks.stream()
                        .map(ContentChunk::getPageNumber)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList()))
                .contentSources(contentSources)
                .confidenceScore(calculateConfidenceScore(relevantChunks))
                .build();

        return PaperChatResponse.builder()
                .sessionId(session.getId())
                .response(assistantMessage.getContent())
                .context(contextMetadata)
                .timestamp(assistantMessage.getTimestamp())
                .success(true)
                .build();
    }

    /**
     * Build comprehensive prompt for enhanced AI responses with selected text
     * context
     */
    private String buildComprehensivePrompt(
            PaperExtraction extraction,
            List<ContentChunk> relevantChunks,
            List<ChatMessage> recentHistory,
            String currentQuestion,
            String selectedText) {

        StringBuilder prompt = new StringBuilder();

        // System context and instructions
        prompt.append("You are an advanced AI research assistant specializing in academic paper analysis. ");
        prompt.append(
                "Your task is to provide comprehensive, detailed, and well-structured responses based on the provided paper content.\n\n");

        // Response guidelines
        prompt.append("RESPONSE GUIDELINES:\n");
        prompt.append(
                "1. Provide detailed, comprehensive explanations that are at least 200-300 words when explaining concepts\n");
        prompt.append("2. Use clear structure with headings, bullet points, and numbered lists when appropriate\n");
        prompt.append("3. Reference specific sections, figures, tables, or equations when relevant\n");
        prompt.append("4. If explaining a concept, provide context, methodology, and implications\n");
        prompt.append("5. When discussing figures or tables, describe what they show and their significance\n");
        prompt.append("6. Always maintain academic tone while being accessible\n");
        prompt.append(
                "7. If information is insufficient, clearly state what cannot be determined from the provided content\n\n");

        // Paper metadata
        prompt.append("PAPER INFORMATION:\n");
        prompt.append("Title: ").append(extraction.getPaper().getTitle()).append("\n");
        if (extraction.getPaper().getAuthors() != null) {
            prompt.append("Authors: ")
                    .append(extraction.getPaper().getAuthors())
                    .append("\n");
        }
        prompt.append("\n");

        // Selected text context (highest priority)
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            prompt.append("SELECTED TEXT CONTEXT (User highlighted this specific text for discussion):\n");
            prompt.append("\"").append(selectedText).append("\"\n\n");
            prompt.append("IMPORTANT: The user has specifically selected the above text. ");
            prompt.append("Please focus your response on this selected content and provide detailed explanation, ");
            prompt.append("context, and analysis of this specific section.\n\n");
        }

        // Conversation history context
        if (recentHistory != null && !recentHistory.isEmpty()) {
            prompt.append("RECENT CONVERSATION HISTORY:\n");
            for (ChatMessage message : recentHistory) {
                String role = message.getRole() == ChatMessage.Role.USER ? "User" : "Assistant";
                prompt.append(role)
                        .append(": ")
                        .append(truncateText(message.getContent(), 200))
                        .append("\n");
            }
            prompt.append("\n");
        }

        // Relevant content chunks
        prompt.append("RELEVANT PAPER CONTENT:\n");
        for (int i = 0; i < relevantChunks.size(); i++) {
            ContentChunk chunk = relevantChunks.get(i);
            prompt.append("--- Content ")
                    .append(i + 1)
                    .append(" (")
                    .append(chunk.getSource())
                    .append(") ---\n");
            prompt.append(chunk.getContent()).append("\n\n");
        }

        // Current question
        prompt.append("CURRENT QUESTION:\n");
        prompt.append(currentQuestion).append("\n\n");

        // Final instructions
        prompt.append("RESPONSE INSTRUCTIONS:\n");
        if (selectedText != null && !selectedText.trim().isEmpty()) {
            prompt.append("- PRIORITY: Focus primarily on the selected text provided above\n");
            prompt.append("- Provide a comprehensive explanation of the selected content\n");
            prompt.append("- Explain the context and significance of the selected text within the paper\n");
            prompt.append("- Connect the selected text to other relevant parts of the paper if applicable\n");
        }
        prompt.append("- Provide a detailed, comprehensive response (aim for 200-300+ words)\n");
        prompt.append("- Use clear structure with appropriate formatting\n");
        prompt.append("- Reference specific figures, tables, equations, or sections when relevant\n");
        prompt.append("- If explaining methodology, include steps and reasoning\n");
        prompt.append("- If discussing results, include implications and significance\n");
        prompt.append("- Maintain academic rigor while being accessible\n");
        prompt.append(
                "- If the selected text or question refers to specific elements not provided, state what additional information would be needed\n\n");

        prompt.append("Please provide your comprehensive response:\n");

        return prompt.toString();
    }

    /**
     * Inner class to track specific references in questions
     */
    private static class SpecificReferences {
        public List<Integer> figures = new ArrayList<>();
        public List<Integer> tables = new ArrayList<>();
        public List<Integer> pages = new ArrayList<>();
        public List<String> sections = new ArrayList<>();
    }
}
