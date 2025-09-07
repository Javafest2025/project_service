package org.solace.scholar_ai.project_service.service.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.chat.CreateChatSessionRequest;
import org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest;
import org.solace.scholar_ai.project_service.dto.response.chat.ChatMessageResponse;
import org.solace.scholar_ai.project_service.dto.response.chat.ChatSessionHistoryResponse;
import org.solace.scholar_ai.project_service.dto.response.chat.ChatSessionResponse;
import org.solace.scholar_ai.project_service.dto.response.chat.PaperChatResponse;
import org.solace.scholar_ai.project_service.exception.ChatSessionNotFoundException;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.model.chat.ChatMessage;
import org.solace.scholar_ai.project_service.model.chat.ChatSession;
import org.solace.scholar_ai.project_service.repository.chat.ChatMessageRepository;
import org.solace.scholar_ai.project_service.repository.chat.ChatSessionRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.service.summary.GeminiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing chat sessions and conversation history
 * Provides GitHub Copilot-like chat experience with session persistence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PaperRepository paperRepository;
    private final PaperContextChatService paperContextChatService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new chat session with an initial message
     * AI generates session title based on the first message
     */
    @Transactional
    public ChatSessionResponse createChatSession(UUID paperId, CreateChatSessionRequest request) {
        log.info(
                "Creating new chat session for paper: {} with message: '{}'",
                paperId,
                truncateMessage(request.getInitialMessage()));

        // Validate paper exists
        paperRepository
                .findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException("Paper not found with ID: " + paperId));

        // Create chat session
        ChatSession session = ChatSession.builder()
                .paperId(paperId)
                .userId(request.getUserId())
                .title(request.getCustomTitle() != null ? request.getCustomTitle() : "New Chat")
                .createdAt(Instant.now())
                .lastMessageAt(Instant.now())
                .updatedAt(Instant.now())
                .messageCount(0)
                .isActive(true)
                .build();

        session = chatSessionRepository.save(session);

        // Process initial message using existing chat service
        PaperChatRequest chatRequest = PaperChatRequest.builder()
                .sessionId(session.getId())
                .message(request.getInitialMessage())
                .selectedText(request.getSelectedText())
                .selectionContext(request.getSelectionContext())
                .build();

        try {
            // Generate initial response
            PaperChatResponse response = paperContextChatService.chatWithPaper(paperId, chatRequest);

            // Generate session title if not provided
            if (request.getCustomTitle() == null) {
                String generatedTitle = generateSessionTitle(request.getInitialMessage(), response.getResponse());
                session.setTitle(generatedTitle);
                session = chatSessionRepository.save(session);
            }

            log.info("Created chat session {} with title: '{}'", session.getId(), session.getTitle());

            return mapToSessionResponse(session, response.getResponse());

        } catch (Exception e) {
            log.error("Error creating chat session for paper {}: {}", paperId, e.getMessage(), e);
            throw new RuntimeException("Failed to create chat session", e);
        }
    }

    /**
     * Get all chat sessions for a specific paper
     */
    public List<ChatSessionResponse> getChatSessionsForPaper(UUID paperId) {
        log.debug("Retrieving chat sessions for paper: {}", paperId);

        List<ChatSession> sessions =
                chatSessionRepository.findByPaperIdAndIsActiveTrueOrderByLastMessageAtDesc(paperId);

        return sessions.stream()
                .map(session -> {
                    String lastMessagePreview = getLastMessagePreview(session.getId());
                    return mapToSessionResponse(session, lastMessagePreview);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get complete chat history for a session
     */
    public ChatSessionHistoryResponse getChatSessionHistory(UUID sessionId) {
        log.debug("Retrieving chat history for session: {}", sessionId);

        ChatSession session = chatSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ChatSessionNotFoundException("Chat session not found: " + sessionId));

        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);

        List<ChatMessageResponse> messageResponses =
                messages.stream().map(this::mapToMessageResponse).collect(Collectors.toList());

        ChatSessionHistoryResponse.SessionStats stats = calculateSessionStats(messages);

        return ChatSessionHistoryResponse.builder()
                .sessionId(session.getId())
                .paperId(session.getPaperId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .lastMessageAt(session.getLastMessageAt())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
                .messages(messageResponses)
                .stats(stats)
                .build();
    }

    /**
     * Continue an existing chat session
     */
    @Transactional
    public PaperChatResponse continueChat(UUID sessionId, PaperChatRequest request) {
        log.info("Continuing chat in session: {} with message: '{}'", sessionId, truncateMessage(request.getMessage()));

        ChatSession session = chatSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ChatSessionNotFoundException("Chat session not found: " + sessionId));

        // Update session activity
        session.setLastMessageAt(Instant.now());
        session.setUpdatedAt(Instant.now());
        chatSessionRepository.save(session);

        // Set session ID in request
        request.setSessionId(sessionId);

        // Process message using existing chat service
        return paperContextChatService.chatWithPaper(session.getPaperId(), request);
    }

    /**
     * Archive/delete a chat session
     */
    @Transactional
    public void archiveChatSession(UUID sessionId) {
        log.info("Archiving chat session: {}", sessionId);

        ChatSession session = chatSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ChatSessionNotFoundException("Chat session not found: " + sessionId));

        session.setIsActive(false);
        chatSessionRepository.save(session);
    }

    /**
     * Update chat session title
     */
    @Transactional
    public ChatSessionResponse updateSessionTitle(UUID sessionId, String newTitle) {
        log.info("Updating session title for {}: '{}'", sessionId, newTitle);

        ChatSession session = chatSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ChatSessionNotFoundException("Chat session not found: " + sessionId));

        session.setTitle(newTitle);
        session = chatSessionRepository.save(session);

        String lastMessagePreview = getLastMessagePreview(sessionId);
        return mapToSessionResponse(session, lastMessagePreview);
    }

    /**
     * Generate meaningful session title using AI
     */
    private String generateSessionTitle(String initialMessage, String aiResponse) {
        try {
            String prompt = String.format(
                    "Generate a concise, descriptive title (max 50 characters) for a research paper discussion based on this conversation:\n\n"
                            + "User: %s\n\n"
                            + "Assistant: %s\n\n"
                            + "Title should capture the main topic/question. Examples: 'Machine Learning Algorithms', 'Data Analysis Methods', 'Research Methodology'.\n"
                            + "Return only the title, no quotes or extra text.",
                    truncateMessage(initialMessage), truncateMessage(aiResponse));

            String title = geminiService.generate(
                    prompt,
                    org.solace.scholar_ai.project_service.service.summary.GeminiService.GenerationConfig.builder()
                            .temperature(0.3)
                            .maxOutputTokens(100)
                            .build());

            // Clean and validate title
            title = title.trim().replaceAll("^[\"']|[\"']$", ""); // Remove quotes
            if (title.length() > 50) {
                title = title.substring(0, 47) + "...";
            }

            return title.isEmpty() ? "Research Discussion" : title;

        } catch (Exception e) {
            log.warn("Failed to generate session title, using default: {}", e.getMessage());
            return "Research Discussion";
        }
    }

    /**
     * Get preview of last message in session
     */
    private String getLastMessagePreview(UUID sessionId) {
        Optional<ChatMessage> lastMessage = chatMessageRepository.findFirstBySessionIdOrderByTimestampDesc(sessionId);

        if (lastMessage.isPresent()) {
            String content = lastMessage.get().getContent();
            return content.length() > 100 ? content.substring(0, 97) + "..." : content;
        }
        return "No messages yet";
    }

    /**
     * Calculate session statistics
     */
    private ChatSessionHistoryResponse.SessionStats calculateSessionStats(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return ChatSessionHistoryResponse.SessionStats.builder()
                    .totalMessages(0)
                    .userMessages(0)
                    .assistantMessages(0)
                    .build();
        }

        long userMessages = messages.stream()
                .filter(m -> m.getRole() == ChatMessage.Role.USER)
                .count();

        long assistantMessages = messages.stream()
                .filter(m -> m.getRole() == ChatMessage.Role.ASSISTANT)
                .count();

        return ChatSessionHistoryResponse.SessionStats.builder()
                .totalMessages(messages.size())
                .userMessages((int) userMessages)
                .assistantMessages((int) assistantMessages)
                .firstMessageAt(messages.get(0).getTimestamp())
                .lastMessageAt(messages.get(messages.size() - 1).getTimestamp())
                .build();
    }

    /**
     * Map ChatSession to ChatSessionResponse
     */
    private ChatSessionResponse mapToSessionResponse(ChatSession session, String lastMessagePreview) {
        return ChatSessionResponse.builder()
                .sessionId(session.getId())
                .paperId(session.getPaperId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .lastMessageAt(session.getLastMessageAt())
                .messageCount(session.getMessageCount())
                .isActive(session.getIsActive())
                .lastMessagePreview(lastMessagePreview)
                .isCurrent(false) // Will be set by controller if needed
                .build();
    }

    /**
     * Map ChatMessage to ChatMessageResponse
     */
    private ChatMessageResponse mapToMessageResponse(ChatMessage message) {
        ChatMessageResponse response = ChatMessageResponse.builder()
                .messageId(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .tokenCount(message.getTokenCount())
                .build();

        // Parse context metadata for assistant messages
        if (message.getRole() == ChatMessage.Role.ASSISTANT && message.getContextMetadata() != null) {
            try {
                PaperChatResponse.ContextMetadata metadata =
                        objectMapper.readValue(message.getContextMetadata(), PaperChatResponse.ContextMetadata.class);
                response.setContextMetadata(metadata);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse context metadata for message {}: {}", message.getId(), e.getMessage());
            }
        }

        return response;
    }

    /**
     * Truncate message for logging
     */
    private String truncateMessage(String message) {
        return message != null && message.length() > 100 ? message.substring(0, 97) + "..." : message;
    }

    // ============ NEW METHODS FOR CONTROLLER INTEGRATION ============

    /**
     * Create a new chat session from ChatRequest
     */
    @Transactional
    public org.solace.scholar_ai.project_service.dto.chat.ChatSession createSession(
            String paperId, org.solace.scholar_ai.project_service.dto.chat.ChatRequest request) {
        try {
            UUID paperUuid = UUID.fromString(paperId);

            CreateChatSessionRequest sessionRequest = CreateChatSessionRequest.builder()
                    .userId(request.getUserId() != null ? request.getUserId() : "anonymous")
                    .initialMessage(request.getMessage())
                    .customTitle(request.getSessionTitle())
                    .selectedText(request.getSelectedText())
                    .selectionContext(
                            request.getSelectionContext() != null
                                    ? org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest
                                            .SelectionContext.builder()
                                            .from(request.getSelectionContext().getFrom())
                                            .to(request.getSelectionContext().getTo())
                                            .pageNumber(request.getSelectionContext()
                                                    .getPageNumber())
                                            .sectionTitle(request.getSelectionContext()
                                                    .getSectionTitle())
                                            .build()
                                    : null)
                    .build();

            ChatSessionResponse response = createChatSession(paperUuid, sessionRequest);

            return org.solace.scholar_ai.project_service.dto.chat.ChatSession.builder()
                    .sessionId(response.getSessionId().toString())
                    .paperId(paperId)
                    .title(response.getTitle())
                    .createdAt(response.getCreatedAt().toString())
                    .lastMessageAt(response.getLastMessageAt().toString())
                    .messageCount(response.getMessageCount())
                    .build();

        } catch (Exception e) {
            log.error("Error creating session for paper {}: {}", paperId, e.getMessage(), e);
            // Return a basic session structure
            return org.solace.scholar_ai.project_service.dto.chat.ChatSession.builder()
                    .sessionId(UUID.randomUUID().toString())
                    .paperId(paperId)
                    .title(request.getSessionTitle() != null ? request.getSessionTitle() : "New Chat Session")
                    .createdAt(java.time.LocalDateTime.now().toString())
                    .lastMessageAt(java.time.LocalDateTime.now().toString())
                    .messageCount(0)
                    .build();
        }
    }

    /**
     * Send a message to an existing session
     */
    @Transactional
    public org.solace.scholar_ai.project_service.dto.chat.ChatResponse sendMessage(
            String paperId, String sessionId, org.solace.scholar_ai.project_service.dto.chat.ChatRequest request) {
        try {
            UUID paperUuid = UUID.fromString(paperId);
            UUID sessionUuid = UUID.fromString(sessionId);

            PaperChatRequest chatRequest = PaperChatRequest.builder()
                    .sessionId(sessionUuid)
                    .message(request.getMessage())
                    .selectedText(request.getSelectedText())
                    .selectionContext(
                            request.getSelectionContext() != null
                                    ? org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest
                                            .SelectionContext.builder()
                                            .from(request.getSelectionContext().getFrom())
                                            .to(request.getSelectionContext().getTo())
                                            .pageNumber(request.getSelectionContext()
                                                    .getPageNumber())
                                            .sectionTitle(request.getSelectionContext()
                                                    .getSectionTitle())
                                            .build()
                                    : null)
                    .build();

            PaperChatResponse response = paperContextChatService.chatWithPaper(paperUuid, chatRequest);

            return org.solace.scholar_ai.project_service.dto.chat.ChatResponse.builder()
                    .sessionId(sessionId)
                    .response(response.getResponse())
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error sending message to session {} for paper {}: {}", sessionId, paperId, e.getMessage(), e);

            return org.solace.scholar_ai.project_service.dto.chat.ChatResponse.builder()
                    .sessionId(sessionId)
                    .response("I'm sorry, I encountered an error processing your request. Please try again.")
                    .timestamp(java.time.LocalDateTime.now().toString())
                    .success(false)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Get all chat sessions for a paper
     */
    public java.util.List<org.solace.scholar_ai.project_service.dto.chat.ChatSession> getChatSessions(String paperId) {
        try {
            UUID paperUuid = UUID.fromString(paperId);
            List<ChatSessionResponse> sessions = getChatSessionsForPaper(paperUuid);

            return sessions.stream()
                    .map(session -> org.solace.scholar_ai.project_service.dto.chat.ChatSession.builder()
                            .sessionId(session.getSessionId().toString())
                            .paperId(paperId)
                            .title(session.getTitle())
                            .createdAt(session.getCreatedAt().toString())
                            .lastMessageAt(session.getLastMessageAt().toString())
                            .messageCount(session.getMessageCount())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting sessions for paper {}: {}", paperId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get a specific chat session with message history
     */
    public java.util.Map<String, Object> getChatSession(String paperId, String sessionId) {
        try {
            UUID paperUuid = UUID.fromString(paperId);
            UUID sessionUuid = UUID.fromString(sessionId);

            ChatSessionHistoryResponse sessionHistory = getChatSessionHistory(sessionUuid);

            List<org.solace.scholar_ai.project_service.dto.chat.ChatMessage> messages =
                    sessionHistory.getMessages().stream()
                            .map(msg -> org.solace.scholar_ai.project_service.dto.chat.ChatMessage.builder()
                                    .id(msg.getMessageId().toString())
                                    .role(msg.getRole().toString())
                                    .content(msg.getContent())
                                    .timestamp(msg.getTimestamp().toString())
                                    .build())
                            .collect(Collectors.toList());

            return java.util.Map.of(
                    "sessionId", sessionId,
                    "paperId", paperId,
                    "title", sessionHistory.getTitle(),
                    "messages", messages,
                    "createdAt", sessionHistory.getCreatedAt().toString(),
                    "lastMessageAt", sessionHistory.getLastMessageAt().toString(),
                    "messageCount", sessionHistory.getMessageCount());

        } catch (Exception e) {
            log.error("Error getting session {} for paper {}: {}", sessionId, paperId, e.getMessage(), e);

            return java.util.Map.of(
                    "sessionId",
                    sessionId,
                    "paperId",
                    paperId,
                    "title",
                    "Chat Session",
                    "messages",
                    List.of(),
                    "createdAt",
                    java.time.LocalDateTime.now().toString(),
                    "lastMessageAt",
                    java.time.LocalDateTime.now().toString(),
                    "messageCount",
                    0);
        }
    }
}
