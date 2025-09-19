package org.solace.scholar_ai.project_service.service.latex;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.latex.*;
import org.solace.scholar_ai.project_service.model.latex.*;
import org.solace.scholar_ai.project_service.repository.latex.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LatexAiChatService {

    private final LatexAiChatSessionRepository sessionRepository;
    private final LatexAiChatMessageRepository messageRepository;
    private final LatexDocumentCheckpointRepository checkpointRepository;
    private final DocumentRepository documentRepository;
    private final AIAssistanceService aiAssistanceService;

    /**
     * Get or create a chat session for a document
     */
    public LatexAiChatSessionDto getOrCreateChatSession(UUID documentId, UUID projectId) {
        log.info("Getting or creating chat session for document: {}, project: {}", documentId, projectId);

        Optional<LatexAiChatSession> existingSession =
                sessionRepository.findByDocumentIdWithMessagesAndCheckpoints(documentId);

        if (existingSession.isPresent()) {
            log.info("Found existing chat session: {}", existingSession.get().getId());
            return convertToDto(existingSession.get());
        }

        // Create new session
        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        LatexAiChatSession newSession = LatexAiChatSession.builder()
                .document(document)
                .projectId(projectId)
                .sessionTitle("LaTeX AI Chat")
                .isActive(true)
                .build();

        newSession = sessionRepository.save(newSession);
        log.info("Created new chat session: {}", newSession.getId());

        // Add welcome message
        createWelcomeMessage(newSession);

        return convertToDto(newSession);
    }

    /**
     * Send a message to the chat (user message + AI response)
     */
    public LatexAiChatMessageDto sendMessage(UUID documentId, CreateLatexChatMessageRequest request) {
        log.info("Sending message to chat for document: {}", documentId);

        LatexAiChatSession session = sessionRepository
                .findByDocument_Id(documentId)
                .orElseThrow(() -> new RuntimeException("Chat session not found for document: " + documentId));

        // Create user message
        LatexAiChatMessage userMessage = LatexAiChatMessage.builder()
                .session(session)
                .messageType(LatexAiChatMessage.MessageType.USER)
                .content(request.getContent())
                .selectionRangeFrom(request.getSelectionRangeFrom())
                .selectionRangeTo(request.getSelectionRangeTo())
                .cursorPosition(request.getCursorPosition())
                .build();

        userMessage = messageRepository.save(userMessage);

        // Process AI response
        try {
            String aiResponse = aiAssistanceService.processChatRequest(
                    request.getSelectedText() != null ? request.getSelectedText() : "",
                    request.getUserRequest() != null ? request.getUserRequest() : request.getContent(),
                    request.getFullDocument() != null ? request.getFullDocument() : "");

            // Parse AI response to extract LaTeX suggestion and action type
            ParsedAiResponse parsedResponse = parseAiResponse(aiResponse, request);

            // Create AI message
            LatexAiChatMessage aiMessage = LatexAiChatMessage.builder()
                    .session(session)
                    .messageType(LatexAiChatMessage.MessageType.AI)
                    .content(parsedResponse.getExplanation())
                    .latexSuggestion(parsedResponse.getLatexCode())
                    .actionType(parsedResponse.getActionType())
                    .selectionRangeFrom(request.getSelectionRangeFrom())
                    .selectionRangeTo(request.getSelectionRangeTo())
                    .cursorPosition(request.getCursorPosition())
                    .isApplied(false)
                    .build();

            aiMessage = messageRepository.save(aiMessage);

            // Create checkpoint before applying AI suggestion
            if (parsedResponse.getLatexCode() != null
                    && !parsedResponse.getLatexCode().trim().isEmpty()) {
                createCheckpoint(
                        documentId,
                        session.getId(),
                        aiMessage.getId(),
                        "Before AI Suggestion",
                        request.getFullDocument(),
                        null);
            }

            log.info("Created user message: {}, AI message: {}", userMessage.getId(), aiMessage.getId());
            return convertToDto(aiMessage);

        } catch (Exception e) {
            log.error("Error processing AI response", e);

            // Create error AI message
            LatexAiChatMessage errorMessage = LatexAiChatMessage.builder()
                    .session(session)
                    .messageType(LatexAiChatMessage.MessageType.AI)
                    .content("I'm sorry, I encountered an error processing your request. Please try again.")
                    .isApplied(false)
                    .build();

            errorMessage = messageRepository.save(errorMessage);
            return convertToDto(errorMessage);
        }
    }

    /**
     * Mark an AI suggestion as applied
     */
    public void markSuggestionAsApplied(Long messageId, String contentAfter) {
        log.info("Marking message {} as applied", messageId);

        LatexAiChatMessage message = messageRepository
                .findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        message.setIsApplied(true);
        messageRepository.save(message);

        // Update checkpoint with the content after application
        checkpointRepository
                .findByDocument_IdAndIsCurrentTrue(
                        message.getSession().getDocument().getId())
                .ifPresent(checkpoint -> {
                    checkpoint.setContentAfter(contentAfter);
                    checkpointRepository.save(checkpoint);
                });
    }

    /**
     * Get chat history for a document
     */
    @Transactional(readOnly = true)
    public List<LatexAiChatMessageDto> getChatHistory(UUID documentId) {
        log.info("Getting chat history for document: {}", documentId);

        LatexAiChatSession session =
                sessionRepository.findByDocument_Id(documentId).orElse(null);

        if (session == null) {
            return List.of();
        }

        List<LatexAiChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
        return messages.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Create a document checkpoint
     */
    public LatexDocumentCheckpointDto createCheckpoint(
            UUID documentId,
            UUID sessionId,
            Long messageId,
            String checkpointName,
            String contentBefore,
            String contentAfter) {
        log.info("Creating checkpoint for document: {}", documentId);

        // Clear current checkpoint flag
        checkpointRepository.clearCurrentCheckpointForDocument(documentId);

        Document document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        LatexDocumentCheckpoint checkpoint = LatexDocumentCheckpoint.builder()
                .document(document)
                .session(sessionRepository.getReferenceById(sessionId))
                .message(messageId != null ? messageRepository.getReferenceById(messageId) : null)
                .checkpointName(checkpointName)
                .contentBefore(contentBefore)
                .contentAfter(contentAfter)
                .isCurrent(true)
                .build();

        checkpoint = checkpointRepository.save(checkpoint);
        return convertToDto(checkpoint);
    }

    /**
     * Restore document to a checkpoint
     */
    public String restoreToCheckpoint(Long checkpointId) {
        log.info("Restoring to checkpoint: {}", checkpointId);

        LatexDocumentCheckpoint checkpoint = checkpointRepository
                .findById(checkpointId)
                .orElseThrow(() -> new RuntimeException("Checkpoint not found: " + checkpointId));

        // Clear current checkpoint flag and set this one as current
        checkpointRepository.clearCurrentCheckpointForDocument(
                checkpoint.getDocument().getId());
        checkpointRepository.setCheckpointAsCurrent(checkpointId);

        return checkpoint.getContentBefore();
    }

    /**
     * Get checkpoints for a document
     */
    @Transactional(readOnly = true)
    public List<LatexDocumentCheckpointDto> getCheckpoints(UUID documentId) {
        log.info("Getting checkpoints for document: {}", documentId);

        List<LatexDocumentCheckpoint> checkpoints =
                checkpointRepository.findByDocument_IdOrderByCreatedAtDesc(documentId);
        return checkpoints.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // Private helper methods
    private void createWelcomeMessage(LatexAiChatSession session) {
        LatexAiChatMessage welcomeMessage = LatexAiChatMessage.builder()
                .session(session)
                .messageType(LatexAiChatMessage.MessageType.AI)
                .content("Welcome to **LaTeXAI**! 🚀 I'm your specialized LaTeX assistant for this document.\n\n"
                        + "**I can help you with:**\n"
                        + "• Writing and formatting LaTeX documents\n"
                        + "• Fixing compilation errors and syntax issues\n"
                        + "• Suggesting mathematical notation and environments\n"
                        + "• Optimizing document structure and styling\n"
                        + "• Using packages and custom commands\n"
                        + "• Converting content to LaTeX format\n\n"
                        + "Select text in your document and ask me anything about LaTeX!")
                .isApplied(false)
                .build();

        messageRepository.save(welcomeMessage);
    }

    private ParsedAiResponse parseAiResponse(String aiResponse, CreateLatexChatMessageRequest request) {
        // Extract LaTeX code from response
        String latexCode = extractLatexCode(aiResponse);
        String explanation = extractExplanation(aiResponse);
        LatexAiChatMessage.ActionType actionType = determineActionType(request, aiResponse);

        return ParsedAiResponse.builder()
                .explanation(explanation)
                .latexCode(latexCode)
                .actionType(actionType)
                .build();
    }

    private String extractLatexCode(String response) {
        // Try to extract LaTeX code from ```latex blocks
        java.util.regex.Pattern latexPattern =
                java.util.regex.Pattern.compile("```latex\\s*(.*?)\\s*```", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = latexPattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Try to extract from generic ``` blocks
        java.util.regex.Pattern codePattern =
                java.util.regex.Pattern.compile("```\\s*(.*?)\\s*```", java.util.regex.Pattern.DOTALL);
        matcher = codePattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }

    private String extractExplanation(String response) {
        // Extract everything before the first LaTeX code block
        String[] parts = response.split("```");
        if (parts.length > 0) {
            return parts[0].trim();
        }
        return response;
    }

    private LatexAiChatMessage.ActionType determineActionType(
            CreateLatexChatMessageRequest request, String aiResponse) {
        String content = request.getContent().toLowerCase();

        if (request.getSelectionRangeFrom() != null && request.getSelectionRangeTo() != null) {
            if (content.contains("replace") || content.contains("change") || content.contains("modify")) {
                return LatexAiChatMessage.ActionType.REPLACE;
            } else if (content.contains("delete") || content.contains("remove")) {
                return LatexAiChatMessage.ActionType.DELETE;
            } else {
                return LatexAiChatMessage.ActionType.REPLACE; // Default for selections
            }
        } else {
            if (content.contains("add") || content.contains("insert")) {
                return LatexAiChatMessage.ActionType.ADD;
            } else if (content.contains("modify") || content.contains("change")) {
                return LatexAiChatMessage.ActionType.MODIFY;
            } else {
                return LatexAiChatMessage.ActionType.ADD; // Default for no selection
            }
        }
    }

    // Conversion methods
    private LatexAiChatSessionDto convertToDto(LatexAiChatSession session) {
        return LatexAiChatSessionDto.builder()
                .id(session.getId())
                .documentId(session.getDocument().getId())
                .projectId(session.getProjectId())
                .sessionTitle(session.getSessionTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .isActive(session.getIsActive())
                .messageCount(session.getMessageCount())
                .lastMessageTime(session.getLastMessageTime())
                .messages(
                        session.getMessages() != null && !session.getMessages().isEmpty()
                                ? session.getMessages().stream()
                                        .map(this::convertToDto)
                                        .collect(Collectors.toList())
                                : List.of())
                .checkpoints(List.of()) // Don't fetch checkpoints here to avoid lazy loading issues
                .currentCheckpoint(null) // Don't fetch current checkpoint here to avoid lazy loading issues
                .build();
    }

    private LatexAiChatMessageDto convertToDto(LatexAiChatMessage message) {
        return LatexAiChatMessageDto.builder()
                .id(message.getId())
                .sessionId(message.getSession().getId())
                .messageType(message.getMessageType())
                .content(message.getContent())
                .latexSuggestion(message.getLatexSuggestion())
                .actionType(message.getActionType())
                .selectionRangeFrom(message.getSelectionRangeFrom())
                .selectionRangeTo(message.getSelectionRangeTo())
                .cursorPosition(message.getCursorPosition())
                .isApplied(message.getIsApplied())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private LatexDocumentCheckpointDto convertToDto(LatexDocumentCheckpoint checkpoint) {
        return LatexDocumentCheckpointDto.builder()
                .id(checkpoint.getId())
                .documentId(checkpoint.getDocument().getId())
                .sessionId(checkpoint.getSession().getId())
                .messageId(
                        checkpoint.getMessage() != null
                                ? checkpoint.getMessage().getId()
                                : null)
                .checkpointName(checkpoint.getCheckpointName())
                .contentBefore(checkpoint.getContentBefore())
                .contentAfter(checkpoint.getContentAfter())
                .createdAt(checkpoint.getCreatedAt())
                .isCurrent(checkpoint.getIsCurrent())
                .build();
    }

    @Data
    @Builder
    private static class ParsedAiResponse {
        private String explanation;
        private String latexCode;
        private LatexAiChatMessage.ActionType actionType;
    }
}
