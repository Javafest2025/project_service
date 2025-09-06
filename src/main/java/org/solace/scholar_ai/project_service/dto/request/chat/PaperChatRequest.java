package org.solace.scholar_ai.project_service.dto.request.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for chatting with a paper using extracted content as context")
public class PaperChatRequest {

    @NotBlank(message = "Message cannot be empty")
    @Size(max = 2000, message = "Message cannot exceed 2000 characters")
    @Schema(
            description = "User's question or message about the paper",
            example = "What is the main contribution of this paper?")
    private String message;

    @Schema(description = "Existing chat session ID (optional for new sessions)")
    private UUID sessionId;

    @Schema(description = "Title for new chat session", example = "Discussion about methodology")
    private String sessionTitle;

    @Schema(description = "Whether to use full context from the paper", example = "false")
    @Builder.Default
    private Boolean useFullContext = false;

    @Schema(description = "Specific sections to focus on (optional)")
    private java.util.List<String> focusSections;

    @Schema(
            description = "Selected text from the paper for context",
            example = "This approach shows significant improvement...")
    private String selectedText;

    @Schema(description = "Selection context information")
    private SelectionContext selectionContext;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Context information about text selection")
    public static class SelectionContext {
        @Schema(description = "Start position of selection")
        private Integer from;

        @Schema(description = "End position of selection")
        private Integer to;

        @Schema(description = "Page number where selection was made")
        private Integer pageNumber;

        @Schema(description = "Section title containing the selection")
        private String sectionTitle;
    }
}
