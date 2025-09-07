package org.solace.scholar_ai.project_service.dto.request.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new chat session
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateChatSessionRequest {

    @NotBlank(message = "Initial message is required")
    private String initialMessage;

    /**
     * Optional custom title for the session
     * If not provided, AI will generate one based on the initial message
     */
    private String customTitle;

    /**
     * Optional user ID for authenticated users
     */
    private String userId;

    /**
     * Selected text context for the initial message
     */
    private String selectedText;

    /**
     * Selection context metadata
     */
    private PaperChatRequest.SelectionContext selectionContext;
}
