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
import org.solace.scholar_ai.project_service.model.extraction.*;
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
@Transactional
public class PaperContextChatServiceV2 {

    private final PaperRepository paperRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GeminiGeneralService geminiService;

    // Constants for RAG optimization
    private static final int MAX_CONTEXT_CHUNKS = 5;
    private static final int MAX_CHUNK_LENGTH = 800;
    private static final int MAX_CONVERSATION_HISTORY = 3; // Last 3 Q&A pairs
    private static final double RELEVANCE_THRESHOLD = 0.1;
    private static final int MAX_RESPONSE_TOKENS = 1000; // Limit response length for token efficiency

    /**
     * Main method for chatting with a paper using extracted content as context
     */
    public PaperChatResponse chatWithPaper(UUID paperId, PaperChatRequest request) {
        log.info(
                "Processing chat request for paper: {} with message: '{}'",
                paperId,
                truncateMessage(request.getMessage()));

        try {
            // 1. Validate paper and extraction
            Paper paper = validatePaperAndExtraction(paperId);
            PaperExtraction extraction = paper.getPaperExtraction();

            // 2. Get or create chat session
            ChatSession session = getOrCreateChatSession(paperId, request.getSessionId());

            // 3. Store user message
            ChatMessage userMessage = storeUserMessage(session, request.getMessage());

            // 4. Get recent conversation history
            List<ChatMessage> recentHistory = getRecentChatHistory(session.getId());

            // 5. Retrieve relevant content chunks
            List<ContentChunk> relevantChunks = retrieveRelevantContent(extraction, request.getMessage());

            // 6. Build optimized prompt
            String prompt = buildOptimizedPrompt(extraction, relevantChunks, recentHistory, request.getMessage());

            // 7. Get AI response
            String aiResponse = geminiService.generateResponse(prompt, 0.4, MAX_RESPONSE_TOKENS);

            // 8. Store assistant message
            ChatMessage assistantMessage = storeAssistantMessage(session, aiResponse);

            // 9. Build response with context metadata
            return buildChatResponse(session, assistantMessage, relevantChunks, extraction);

        } catch (Exception e) {
            log.error("Error generating chat response for paper: {}", paperId, e);

            // Create a minimal session for error response
            ChatSession errorSession = getOrCreateChatSession(paperId, request.getSessionId());
            String errorResponse =
                    "I apologize, but I encountered an error while analyzing the paper. Please try rephrasing your question or try again later.";
            ChatMessage errorMessage = storeAssistantMessage(errorSession, errorResponse);

            return PaperChatResponse.builder()
                    .sessionId(errorSession.getId())
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
     * Get existing chat session or create new one
     */
    private ChatSession getOrCreateChatSession(UUID paperId, UUID sessionId) {
        if (sessionId != null) {
            Optional<ChatSession> existingSession = chatSessionRepository.findById(sessionId);
            if (existingSession.isPresent()) {
                return existingSession.get();
            }
        }

        // Create new session
        ChatSession newSession = ChatSession.builder()
                .id(UUID.randomUUID())
                .paperId(paperId)
                .title("New Chat")
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
        chatSessionRepository.save(session);

        return chatMessageRepository.save(assistantMessage);
    }

    /**
     * Retrieve relevant content chunks using RAG approach
     */
    private List<ContentChunk> retrieveRelevantContent(PaperExtraction extraction, String question) {
        log.debug("Retrieving relevant content for question: {}", truncateMessage(question));

        List<ContentChunk> allChunks = new ArrayList<>();

        // Extract keywords from question
        Set<String> questionKeywords = extractKeywords(question.toLowerCase());

        // Check for specific references (pages, figures, sections)
        // Extract specific references (currently unused - part of legacy functionality)
        // SpecificReferences specificRefs = extractSpecificReferences(question);

        // 1. Process sections with content from paragraphs
        extraction.getSections().forEach(section -> {
            String sectionContent = extractSectionContent(section);
            if (sectionContent != null && !sectionContent.trim().isEmpty()) {
                double relevance = calculateRelevanceScore(sectionContent, questionKeywords);
                if (relevance > RELEVANCE_THRESHOLD) {
                    allChunks.add(ContentChunk.builder()
                            .content(sectionContent)
                            .source("Section: "
                                    + (section.getTitle() != null ? section.getTitle() : section.getLabel()))
                            .type("section")
                            .relevanceScore(relevance)
                            .pageNumber(section.getPageStart())
                            .build());
                }
            }
        });

        // 2. Process figures
        extraction.getFigures().forEach(figure -> {
            String figureText = buildFigureText(figure);
            double relevance = calculateRelevanceScore(figureText, questionKeywords);
            if (relevance > RELEVANCE_THRESHOLD) {
                allChunks.add(ContentChunk.builder()
                        .content(figureText)
                        .source(figure.getLabel() + " (Page " + figure.getPage() + ")")
                        .type("figure")
                        .relevanceScore(relevance)
                        .pageNumber(figure.getPage())
                        .build());
            }
        });

        // 3. Process tables
        extraction.getTables().forEach(table -> {
            String tableText = buildTableText(table);
            double relevance = calculateRelevanceScore(tableText, questionKeywords);
            if (relevance > RELEVANCE_THRESHOLD) {
                allChunks.add(ContentChunk.builder()
                        .content(tableText)
                        .source(table.getLabel() + " (Page " + table.getPage() + ")")
                        .type("table")
                        .relevanceScore(relevance)
                        .pageNumber(table.getPage())
                        .build());
            }
        });

        // 4. Sort by relevance and return top chunks
        List<ContentChunk> topChunks = allChunks.stream()
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(MAX_CONTEXT_CHUNKS)
                .collect(Collectors.toList());

        log.debug("Selected {} relevant content chunks", topChunks.size());
        return topChunks;
    }

    /**
     * Extract content from section paragraphs
     */
    private String extractSectionContent(ExtractedSection section) {
        if (section.getParagraphs() == null || section.getParagraphs().isEmpty()) {
            return section.getTitle(); // Return title if no paragraphs
        }

        return section.getParagraphs().stream()
                .sorted(Comparator.comparing(
                        ExtractedParagraph::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ExtractedParagraph::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    /**
     * Build searchable text for figures
     */
    private String buildFigureText(ExtractedFigure figure) {
        StringBuilder text = new StringBuilder();

        if (figure.getLabel() != null) {
            text.append(figure.getLabel()).append(": ");
        }

        if (figure.getCaption() != null) {
            text.append(figure.getCaption());
        }

        if (figure.getOcrText() != null && !figure.getOcrText().trim().isEmpty()) {
            text.append(" ").append(figure.getOcrText());
        }

        return text.toString();
    }

    /**
     * Build searchable text for tables
     */
    private String buildTableText(ExtractedTable table) {
        StringBuilder text = new StringBuilder();

        if (table.getLabel() != null) {
            text.append(table.getLabel()).append(": ");
        }

        if (table.getCaption() != null) {
            text.append(table.getCaption());
        }

        // Add table content from headers and rows if available
        if (table.getHeaders() != null) {
            text.append(" Headers: ").append(table.getHeaders());
        }

        if (table.getRows() != null) {
            text.append(" Data: ").append(truncateText(table.getRows(), 500));
        }

        return text.toString();
    }

    /**
     * Extract keywords from question text
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
     * Calculate relevance score between content and keywords
     */
    private double calculateRelevanceScore(String content, Set<String> questionKeywords) {
        if (content == null || questionKeywords.isEmpty()) return 0.0;

        String contentLower = content.toLowerCase();
        long matchCount = questionKeywords.stream()
                .mapToLong(keyword -> countOccurrences(contentLower, keyword))
                .sum();

        // Normalize by content length and keyword count
        double score = (double) matchCount / Math.max(questionKeywords.size(), 1);
        return Math.min(score, 1.0);
    }

    private long countOccurrences(String text, String word) {
        return Arrays.stream(text.split("\\W+")).filter(w -> w.equals(word)).count();
    }

    /**
     * Extract specific references from question
     */
    // Legacy method - currently unused but kept for backwards compatibility
    @SuppressWarnings("unused")
    private SpecificReferences extractSpecificReferences(String question) {
        SpecificReferences refs = new SpecificReferences();

        // Page references
        Pattern pagePattern = Pattern.compile("page\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher pageMatcher = pagePattern.matcher(question);
        while (pageMatcher.find()) {
            refs.pages.add(Integer.parseInt(pageMatcher.group(1)));
        }

        // Figure references
        Pattern figPattern = Pattern.compile("fig(?:ure)?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher figMatcher = figPattern.matcher(question);
        while (figMatcher.find()) {
            refs.figures.add(Integer.parseInt(figMatcher.group(1)));
        }

        // Table references
        Pattern tablePattern = Pattern.compile("table\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(question);
        while (tableMatcher.find()) {
            refs.tables.add(Integer.parseInt(tableMatcher.group(1)));
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
     * Get recent chat history for conversation context
     */
    private List<ChatMessage> getRecentChatHistory(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampDesc(sessionId).stream()
                .limit(MAX_CONVERSATION_HISTORY * 2) // Get pairs of Q&A
                .collect(Collectors.toList());
    }

    /**
     * Build optimized prompt for Gemini API
     */
    private String buildOptimizedPrompt(
            PaperExtraction extraction,
            List<ContentChunk> relevantChunks,
            List<ChatMessage> recentHistory,
            String currentQuestion) {
        StringBuilder prompt = new StringBuilder();

        // System instructions
        prompt.append(
                "You are a helpful academic research assistant. Answer questions about the research paper using ONLY the provided excerpts. ");
        prompt.append("Be accurate, concise, and cite sources (page numbers/sections) when relevant. ");
        prompt.append("If the answer isn't in the provided content, say so clearly.\n\n");

        // Paper context
        prompt.append("PAPER INFORMATION:\n");
        prompt.append("Title: ").append(extraction.getTitle()).append("\n");
        if (extraction.getAbstractText() != null) {
            prompt.append("Abstract: ")
                    .append(truncateText(extraction.getAbstractText(), 300))
                    .append("\n");
        }
        prompt.append("Pages: ").append(extraction.getPageCount()).append("\n\n");

        // Relevant content chunks
        prompt.append("RELEVANT CONTENT:\n");
        for (int i = 0; i < relevantChunks.size(); i++) {
            ContentChunk chunk = relevantChunks.get(i);
            prompt.append("[")
                    .append(i + 1)
                    .append("] ")
                    .append(chunk.getSource())
                    .append(":\n");
            prompt.append(truncateText(chunk.getContent(), MAX_CHUNK_LENGTH)).append("\n\n");
        }

        // Recent conversation history if available
        if (!recentHistory.isEmpty()) {
            prompt.append("RECENT CONVERSATION:\n");
            for (int i = recentHistory.size() - 1; i >= 0; i--) {
                ChatMessage msg = recentHistory.get(i);
                String role = msg.getRole() == ChatMessage.Role.USER ? "User" : "Assistant";
                prompt.append(role)
                        .append(": ")
                        .append(truncateText(msg.getContent(), 200))
                        .append("\n");
            }
            prompt.append("\n");
        }

        // Current question
        prompt.append("CURRENT QUESTION: ").append(currentQuestion).append("\n\n");
        prompt.append("ANSWER:");

        return prompt.toString();
    }

    /**
     * Build the final chat response
     */
    private PaperChatResponse buildChatResponse(
            ChatSession session,
            ChatMessage assistantMessage,
            List<ContentChunk> relevantChunks,
            PaperExtraction extraction) {

        PaperChatResponse.ContextMetadata contextMetadata = PaperChatResponse.ContextMetadata.builder()
                .sectionsUsed(relevantChunks.stream()
                        .filter(chunk -> "section".equals(chunk.getType()))
                        .map(ContentChunk::getSource)
                        .collect(Collectors.toList()))
                .figuresReferenced(relevantChunks.stream()
                        .filter(chunk -> "figure".equals(chunk.getType()))
                        .map(ContentChunk::getSource)
                        .collect(Collectors.toList()))
                .tablesReferenced(relevantChunks.stream()
                        .filter(chunk -> "table".equals(chunk.getType()))
                        .map(ContentChunk::getSource)
                        .collect(Collectors.toList()))
                .pagesReferenced(relevantChunks.stream()
                        .map(ContentChunk::getPageNumber)
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted()
                        .collect(Collectors.toList()))
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
     * Calculate confidence score based on relevance chunks
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
     * Truncate text to specified length
     */
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private String truncateMessage(String message) {
        return truncateText(message, 100);
    }

    // Helper classes
    private static class SpecificReferences {
        Set<Integer> pages = new HashSet<>();
        Set<Integer> figures = new HashSet<>();
        Set<Integer> tables = new HashSet<>();
        Set<String> sections = new HashSet<>();
    }

    @lombok.Builder
    @lombok.Data
    private static class ContentChunk {
        private String content;
        private String source;
        private String type;
        private double relevanceScore;
        private Integer pageNumber;
    }
}
