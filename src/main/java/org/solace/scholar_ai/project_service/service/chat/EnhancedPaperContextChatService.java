package org.solace.scholar_ai.project_service.service.chat;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest;
import org.solace.scholar_ai.project_service.dto.response.chat.PaperChatResponse;
import org.solace.scholar_ai.project_service.exception.PaperNotExtractedException;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.solace.scholar_ai.project_service.model.chat.ChatMessage;
import org.solace.scholar_ai.project_service.model.chat.ChatSession;
import org.solace.scholar_ai.project_service.model.chat.ContentChunk;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.paper.PaperAuthor;
import org.solace.scholar_ai.project_service.repository.chat.ChatMessageRepository;
import org.solace.scholar_ai.project_service.repository.chat.ChatSessionRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperAuthorRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.service.chat.QueryRequirementAnalysisService.DataRequirement;
import org.solace.scholar_ai.project_service.service.summary.GeminiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enhanced Paper Context Chat Service with Intelligent Query Processing
 * Leverages advanced query analysis, optimized content retrieval, and intelligent prompting
 * for maximum AI response accuracy and relevance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedPaperContextChatService {

    private final PaperRepository paperRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final GeminiService geminiService;

    // Enhanced AI components
    private final QueryRequirementAnalysisService queryRequirementAnalysisService;
    private final EnhancedContentRetrievalService contentRetrievalService;
    private final IntelligentPromptBuilder promptBuilder;

    // Configuration constants
    private static final int MAX_CONVERSATION_HISTORY = 3;

    /**
     * Main method for intelligent chat with papers using comprehensive AI optimization
     */
    @Transactional
    public PaperChatResponse chatWithPaper(UUID paperId, PaperChatRequest request) {
        log.info("Processing intelligent chat request for paper: {} with query type analysis", paperId);

        try {
            // 1. Validate paper and extraction
            Paper paper = validatePaperAndExtraction(paperId);

            // 2. Get or create chat session
            ChatSession session = getOrCreateChatSession(paperId, request.getSessionId());

            // 3. Store user message
            storeUserMessage(session, request.getMessage());

            // 4. AI-powered analysis of what data is needed for this query
            Set<DataRequirement> dataRequirements =
                    queryRequirementAnalysisService.analyzeQueryRequirements(request.getMessage());

            log.debug("AI determined data requirements: {}", dataRequirements);

            // 5. Get paper authors if AI determined they're needed
            List<Author> authors = queryRequirementAnalysisService.shouldIncludeAuthors(dataRequirements)
                    ? getAuthorsForPaper(paperId)
                    : null;

            // 6. Retrieve content based on AI-determined requirements
            List<ContentChunk> relevantChunks = contentRetrievalService.retrieveContentBasedOnRequirements(
                    paper.getPaperExtraction(),
                    request.getMessage(),
                    request.getSelectedText(),
                    request.getSelectionContext(),
                    authors,
                    dataRequirements);

            log.debug("Retrieved {} AI-prioritized content chunks based on requirements", relevantChunks.size());

            // 7. Get recent conversation history
            List<ChatMessage> recentHistory = getRecentChatHistory(session.getId());

            // 8. Build intelligent prompt optimized for determined requirements
            String optimizedPrompt = promptBuilder.buildOptimizedPromptWithRequirements(
                    paper.getPaperExtraction(),
                    relevantChunks,
                    recentHistory,
                    request.getMessage(),
                    request.getSelectedText(),
                    dataRequirements.toArray(new DataRequirement[0]),
                    authors);

            // 9. Generate AI response with standard parameters
            String aiResponse;
            try {
                aiResponse = geminiService.generate(
                        optimizedPrompt,
                        org.solace.scholar_ai.project_service.service.summary.GeminiService.GenerationConfig.builder()
                                .temperature(0.3)
                                .maxOutputTokens(2000)
                                .build());
                if (aiResponse == null || aiResponse.trim().isEmpty()) {
                    throw new RuntimeException("AI service returned empty response");
                }
            } catch (Exception aiError) {
                log.warn("AI service error, providing fallback response: {}", aiError.getMessage());
                aiResponse = generateFallbackResponse(dataRequirements, authors, paper.getPaperExtraction());
            }

            log.debug("Generated AI response using AI-powered query analysis system");

            // 10. Store assistant response
            ChatMessage assistantMessage = storeAssistantMessage(session, aiResponse);

            // 11. Build comprehensive response with metadata
            return buildSimplifiedChatResponse(
                    session, assistantMessage, relevantChunks, paper.getPaperExtraction(), dataRequirements);

        } catch (Exception e) {
            log.error("Error in enhanced chat processing for paper: {}", paperId, e);
            return handleChatError(paperId, request.getSessionId(), e);
        }
    }

    /**
     * Validate paper exists and is fully extracted
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

        // Create new session
        ChatSession newSession = ChatSession.builder()
                .id(UUID.randomUUID())
                .paperId(paperId)
                .title("New Chat") // Will be updated by session service with AI-generated title
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
     * Get recent chat history for context
     */
    private List<ChatMessage> getRecentChatHistory(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampDesc(sessionId).stream()
                .limit(MAX_CONVERSATION_HISTORY * 2) // Include both user and assistant messages
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp())) // Restore chronological order
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get authors for the paper if author information is requested
     */
    private List<Author> getAuthorsForPaper(UUID paperId) {
        try {
            return paperAuthorRepository.findByPaperIdOrderByAuthorOrderAsc(paperId).stream()
                    .map(PaperAuthor::getAuthor)
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not retrieve authors for paper: {}", paperId, e);
            return new ArrayList<>();
        }
    }

    /**
     * Build enhanced chat response with comprehensive metadata
     */
    private PaperChatResponse buildEnhancedChatResponse(
            ChatSession session,
            ChatMessage assistantMessage,
            List<ContentChunk> relevantChunks,
            IntelligentQueryStrategy.QueryAnalysis queryAnalysis,
            PaperExtraction extraction) {

        // Build metadata about the AI processing
        Map<String, Object> processingMetadata = new HashMap<>();
        processingMetadata.put("queryType", queryAnalysis.getPrimaryType().toString());
        processingMetadata.put("complexityScore", queryAnalysis.getComplexityScore());
        processingMetadata.put("chunksUsed", relevantChunks.size());
        processingMetadata.put("temperature", queryAnalysis.getPromptStrategy().getTemperature());
        processingMetadata.put("maxTokens", queryAnalysis.getPromptStrategy().getMaxTokens());
        processingMetadata.put(
                "responseFormat",
                queryAnalysis.getPromptStrategy().getResponseFormat().toString());

        return PaperChatResponse.builder()
                .sessionId(session.getId())
                .response(assistantMessage.getContent())
                .timestamp(assistantMessage.getTimestamp())
                .success(true)
                .build();
    }

    /**
     * Handle chat errors gracefully
     */
    private PaperChatResponse handleChatError(UUID paperId, UUID sessionId, Exception e) {
        String errorResponse = "I apologize, but I encountered an error while analyzing the paper. "
                + "This might be due to complex content or a temporary issue. "
                + "Please try rephrasing your question or try again later.";

        try {
            ChatSession session = getOrCreateChatSession(paperId, sessionId);
            ChatMessage errorMessage = storeAssistantMessage(session, errorResponse);

            return PaperChatResponse.builder()
                    .sessionId(session.getId())
                    .response(errorResponse)
                    .timestamp(errorMessage.getTimestamp())
                    .success(false)
                    .error("Enhanced chat processing error: " + e.getMessage())
                    .build();
        } catch (Exception innerException) {
            log.error("Failed to handle chat error gracefully", innerException);
            return PaperChatResponse.builder()
                    .response(errorResponse)
                    .timestamp(Instant.now())
                    .success(false)
                    .error("Critical error in chat processing")
                    .build();
        }
    }

    /**
     * Build simplified chat response with AI-determined requirements metadata
     */
    private PaperChatResponse buildSimplifiedChatResponse(
            ChatSession session,
            ChatMessage assistantMessage,
            List<ContentChunk> relevantChunks,
            PaperExtraction extraction,
            Set<DataRequirement> dataRequirements) {

        // Build metadata about the AI processing
        Map<String, Object> processingMetadata = new HashMap<>();
        processingMetadata.put("aiQueryAnalysis", true);
        processingMetadata.put("dataRequirements", dataRequirements.toString());
        processingMetadata.put("chunksUsed", relevantChunks.size());
        processingMetadata.put("processingMethod", "AI-powered requirement analysis");

        return PaperChatResponse.builder()
                .sessionId(session.getId())
                .response(assistantMessage.getContent())
                .timestamp(assistantMessage.getTimestamp())
                .success(true)
                .build();
    }

    /**
     * Generate a fallback response when AI service fails
     */
    private String generateFallbackResponse(
            Set<DataRequirement> dataRequirements, List<Author> authors, PaperExtraction extraction) {
        StringBuilder response = new StringBuilder();

        // Handle author queries specifically
        if (dataRequirements.contains(DataRequirement.AUTHORS)) {
            if (authors != null && !authors.isEmpty()) {
                response.append("The authors of this paper are: ");
                String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
                response.append(authorNames).append(".");
            } else {
                response.append("Author information is not available for this paper.");
            }
            return response.toString();
        }

        // Handle title queries
        if (dataRequirements.contains(DataRequirement.TITLE)) {
            if (extraction.getTitle() != null) {
                response.append("The title of this paper is: \"")
                        .append(extraction.getTitle())
                        .append("\"");
                if (authors != null && !authors.isEmpty()) {
                    response.append(" by ");
                    String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
                    response.append(authorNames);
                }
                response.append(".");
            } else {
                response.append("Title information is not available for this paper.");
            }
            return response.toString();
        }

        // Default fallback for other queries
        response.append("I apologize, but I'm experiencing difficulty processing your request at the moment. ");
        if (extraction.getTitle() != null) {
            response.append("This paper is titled \"")
                    .append(extraction.getTitle())
                    .append("\"");
            if (authors != null && !authors.isEmpty()) {
                response.append(" by ");
                String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
                response.append(authorNames);
            }
            response.append(". ");
        }
        response.append("Please try rephrasing your question or try again later.");

        return response.toString();
    }
}
