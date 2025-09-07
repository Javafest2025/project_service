package org.solace.scholar_ai.project_service.controller.chat;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.chat.ChatRequest;
import org.solace.scholar_ai.project_service.dto.chat.ChatResponse;
import org.solace.scholar_ai.project_service.service.ai.CommandExecutorService;
import org.solace.scholar_ai.project_service.service.ai.CommandParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final CommandParserService parserService;
    private final CommandExecutorService executorService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> processMessage(@RequestBody ChatRequest request) {
        try {
            log.info("Processing chat message: {}", request.getMessage());

            // Parse the command
            var parsedCommand = parserService.parseCommand(request.getMessage());

            // Execute the command
            Map<String, Object> executionResult = executorService.executeCommand(parsedCommand, request.getUserId());

            // Build response
            String message = (String) executionResult.getOrDefault("naturalResponse", "Command processed");
            if (message == null) {
                message = "Command processed successfully";
            }

            ChatResponse response = ChatResponse.builder()
                    .message(message)
                    .data(executionResult)
                    .commandType(parsedCommand.getCommandType().toString())
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);

            ChatResponse errorResponse = ChatResponse.builder()
                    .message("I encountered an error processing your request. Please try again.")
                    .data(Map.of("error", e.getMessage()))
                    .commandType("ERROR")
                    .timestamp(LocalDateTime.now().toString())
                    .build();

            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ScholarBot"));
    }
}
