package org.solace.scholar_ai.project_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.chat.PaperChatRequest;
import org.solace.scholar_ai.project_service.dto.response.chat.PaperChatResponse;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.exception.PaperNotExtractedException;
import org.solace.scholar_ai.project_service.service.chat.PaperContextChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Paper Context Chat", description = "AI-powered contextual Q&A for research papers")
public class PaperContextChatControllerClean {

    private final PaperContextChatService paperContextChatService;

    @PostMapping("/{paperId}/chat")
    @Operation(
        summary = "Chat with a paper using AI",
        description = "Ask questions about a paper and get contextual AI-powered responses based on the paper's content. " +
                     "The AI uses extracted paper content to provide accurate, context-aware answers.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Chat response generated successfully",
                content = @Content(schema = @Schema(implementation = PaperChatResponse.class))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "Invalid request - missing question or invalid parameters"
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Paper not found"
            ),
            @ApiResponse(
                responseCode = "422", 
                description = "Paper content not extracted yet - extraction in progress or failed"
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "Internal server error during chat processing"
            )
        }
    )
    public ResponseEntity<PaperChatResponse> chatWithPaper(
            @Parameter(description = "ID of the paper to chat about", required = true)
            @PathVariable UUID paperId,
            
            @Parameter(description = "Chat request containing the question and optional session ID", required = true)
            @Valid @RequestBody PaperChatRequest request) {
        
        log.info("📝 Chat request for paper {}: {}", paperId, request.getMessage());
        
        try {
            PaperChatResponse response = paperContextChatService.chatWithPaper(paperId, request);
            
            log.info("✅ Chat response generated for paper {} in session {}", 
                    paperId, response.getSessionId());
            
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
            @ApiResponse(
                responseCode = "200",
                description = "Chat session history retrieved successfully"
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "Chat session not found"
            )
        }
    )
    public ResponseEntity<?> getChatSessionHistory(
            @Parameter(description = "ID of the paper", required = true)
            @PathVariable String paperId,
            
            @Parameter(description = "ID of the chat session", required = true)
            @PathVariable String sessionId) {
        
        log.info("📖 Retrieving chat history for paper {} session {}", paperId, sessionId);
        
        try {
            // This would be implemented to return chat history
            // For now, return a placeholder response
            return ResponseEntity.ok().body("Chat history endpoint - to be implemented");
            
        } catch (Exception e) {
            log.error("❌ Error retrieving chat history for paper {} session {}: {}", 
                    paperId, sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving chat history");
        }
    }
}
