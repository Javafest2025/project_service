package org.solace.scholar_ai.project_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.chat.CreateChatSessionRequest;
import org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest;
import org.solace.scholar_ai.project_service.dto.response.chat.ChatSessionHistoryResponse;
import org.solace.scholar_ai.project_service.dto.response.chat.ChatSessionResponse;
import org.solace.scholar_ai.project_service.dto.response.chat.PaperChatResponse;
import org.solace.scholar_ai.project_service.exception.ChatSessionNotFoundException;
import org.solace.scholar_ai.project_service.exception.PaperNotExtractedException;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.service.chat.ChatSessionService;
import org.solace.scholar_ai.project_service.service.chat.EnhancedPaperContextChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Paper Context Chat", description = "AI-powered contextual Q&A for research papers with session management")
public class PaperContextChatController {

    private final EnhancedPaperContextChatService paperContextChatService;
    private final ChatSessionService chatSessionService;

    @PostMapping("/{paperId}/chat")
    @Operation(
            summary = "Chat with a paper using AI",
            description =
                    "Ask questions about a paper and get contextual AI-powered responses based on the paper's content. "
                            + "The AI uses extracted paper content to provide accurate, context-aware answers.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Chat response generated successfully",
                        content = @Content(schema = @Schema(implementation = PaperChatResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid request - missing question or invalid parameters"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(
                        responseCode = "422",
                        description = "Paper content not extracted yet - extraction in progress or failed"),
                @ApiResponse(responseCode = "500", description = "Internal server error during chat processing")
            })
    public ResponseEntity<PaperChatResponse> chatWithPaper(
            @Parameter(description = "ID of the paper to chat about", required = true) @PathVariable UUID paperId,
            @Parameter(description = "Chat request containing the question and optional session ID", required = true)
                    @Valid
                    @RequestBody
                    PaperChatRequest request) {

        log.info("📝 Chat request for paper {}: {}", paperId, request.getMessage());

        try {
            PaperChatResponse response = paperContextChatService.chatWithPaper(paperId, request);

            log.info("✅ Chat response generated for paper {} in session {}", paperId, response.getSessionId());

            return ResponseEntity.ok(response);

        } catch (PaperNotFoundException e) {
            log.warn("❌ Paper not found: {}", paperId);
            return ResponseEntity.notFound().build();

        } catch (PaperNotExtractedException e) {
            log.warn("❌ Paper content not extracted: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(PaperChatResponse.builder()
                            .error("Paper content is not yet available. Please wait for extraction to complete.")
                            .build());

        } catch (Exception e) {
            log.error("❌ Error processing chat request for paper {}: {}", paperId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaperChatResponse.builder()
                            .error("An error occurred while processing your request. Please try again.")
                            .build());
        }
    }

    @GetMapping("/{paperId}/chat/sessions/{sessionId}")
    @Operation(
            summary = "Get chat session history",
            description = "Retrieve the complete conversation history for a specific chat session with a paper.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Chat session history retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Chat session not found")
            })
    public ResponseEntity<ChatSessionHistoryResponse> getChatSessionHistory(
            @Parameter(description = "ID of the paper", required = true) @PathVariable UUID paperId,
            @Parameter(description = "ID of the chat session", required = true) @PathVariable UUID sessionId) {

        log.info("📖 Retrieving chat history for paper {} session {}", paperId, sessionId);

        try {
            ChatSessionHistoryResponse history = chatSessionService.getChatSessionHistory(sessionId);
            return ResponseEntity.ok(history);

        } catch (ChatSessionNotFoundException e) {
            log.warn("❌ Chat session not found: {}", sessionId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error(
                    "❌ Error retrieving chat history for paper {} session {}: {}",
                    paperId,
                    sessionId,
                    e.getMessage(),
                    e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== NEW SESSION MANAGEMENT ENDPOINTS ==========

    @PostMapping("/{paperId}/chat/sessions")
    @Operation(
            summary = "Create a new chat session",
            description =
                    "Start a new conversation with a paper. AI will generate a meaningful session title based on the initial message.",
            responses = {
                @ApiResponse(responseCode = "201", description = "Chat session created successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "422", description = "Paper content not extracted yet")
            })
    public ResponseEntity<ChatSessionResponse> createChatSession(
            @Parameter(description = "ID of the paper", required = true) @PathVariable UUID paperId,
            @Valid @RequestBody CreateChatSessionRequest request) {

        log.info(
                "🆕 Creating new chat session for paper {} with message: '{}'",
                paperId,
                truncateMessage(request.getInitialMessage()));

        try {
            ChatSessionResponse session = chatSessionService.createChatSession(paperId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(session);

        } catch (PaperNotFoundException e) {
            log.warn("❌ Paper not found: {}", paperId);
            return ResponseEntity.notFound().build();

        } catch (PaperNotExtractedException e) {
            log.warn("❌ Paper content not extracted: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();

        } catch (Exception e) {
            log.error("❌ Error creating chat session for paper {}: {}", paperId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{paperId}/chat/sessions")
    @Operation(
            summary = "Get all chat sessions for a paper",
            description = "Retrieve all chat sessions for a specific paper, ordered by last activity.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Chat sessions retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found")
            })
    public ResponseEntity<List<ChatSessionResponse>> getChatSessions(
            @Parameter(description = "ID of the paper", required = true) @PathVariable UUID paperId) {

        log.info("📋 Retrieving chat sessions for paper: {}", paperId);

        try {
            List<ChatSessionResponse> sessions = chatSessionService.getChatSessionsForPaper(paperId);
            return ResponseEntity.ok(sessions);

        } catch (Exception e) {
            log.error("❌ Error retrieving chat sessions for paper {}: {}", paperId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{paperId}/chat/sessions/{sessionId}/messages")
    @Operation(
            summary = "Continue chat in existing session",
            description = "Send a message to continue an existing chat session.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Message processed successfully"),
                @ApiResponse(responseCode = "404", description = "Chat session not found"),
                @ApiResponse(responseCode = "422", description = "Paper content not extracted yet")
            })
    public ResponseEntity<PaperChatResponse> continueChat(
            @Parameter(description = "ID of the paper", required = true) @PathVariable UUID paperId,
            @Parameter(description = "ID of the chat session", required = true) @PathVariable UUID sessionId,
            @Valid @RequestBody PaperChatRequest request) {

        log.info(
                "💬 Continuing chat in session {} for paper {} with message: '{}'",
                sessionId,
                paperId,
                truncateMessage(request.getMessage()));

        try {
            PaperChatResponse response = chatSessionService.continueChat(sessionId, request);
            return ResponseEntity.ok(response);

        } catch (ChatSessionNotFoundException e) {
            log.warn("❌ Chat session not found: {}", sessionId);
            return ResponseEntity.notFound().build();

        } catch (PaperNotExtractedException e) {
            log.warn("❌ Paper content not extracted: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(PaperChatResponse.builder()
                            .error("Paper content is not yet available. Please wait for extraction to complete.")
                            .build());

        } catch (Exception e) {
            log.error("❌ Error continuing chat in session {} for paper {}: {}", sessionId, paperId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaperChatResponse.builder()
                            .error("An error occurred while processing your request. Please try again.")
                            .build());
        }
    }

    @PutMapping("/{paperId}/chat/sessions/{sessionId}/title")
    @Operation(
            summary = "Update chat session title",
            description = "Update the title of an existing chat session.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Session title updated successfully"),
                @ApiResponse(responseCode = "404", description = "Chat session not found")
            })
    public ResponseEntity<ChatSessionResponse> updateSessionTitle(
            @Parameter(description = "ID of the paper", required = true) @PathVariable UUID paperId,
            @Parameter(description = "ID of the chat session", required = true) @PathVariable UUID sessionId,
            @RequestBody String newTitle) {

        log.info("✏️ Updating title for session {} to: '{}'", sessionId, newTitle);

        try {
            ChatSessionResponse session = chatSessionService.updateSessionTitle(sessionId, newTitle);
            return ResponseEntity.ok(session);

        } catch (ChatSessionNotFoundException e) {
            log.warn("❌ Chat session not found: {}", sessionId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Error updating session title for {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{paperId}/chat/sessions/{sessionId}")
    @Operation(
            summary = "Archive chat session",
            description = "Archive/delete a chat session (soft delete - marks as inactive).",
            responses = {
                @ApiResponse(responseCode = "204", description = "Session archived successfully"),
                @ApiResponse(responseCode = "404", description = "Chat session not found")
            })
    public ResponseEntity<Void> archiveChatSession(
            @Parameter(description = "ID of the paper", required = true) @PathVariable UUID paperId,
            @Parameter(description = "ID of the chat session", required = true) @PathVariable UUID sessionId) {

        log.info("🗄️ Archiving chat session: {}", sessionId);

        try {
            chatSessionService.archiveChatSession(sessionId);
            return ResponseEntity.noContent().build();

        } catch (ChatSessionNotFoundException e) {
            log.warn("❌ Chat session not found: {}", sessionId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Error archiving session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Helper method to truncate messages for logging
     */
    private String truncateMessage(String message) {
        return message != null && message.length() > 100 ? message.substring(0, 97) + "..." : message;
    }
}
