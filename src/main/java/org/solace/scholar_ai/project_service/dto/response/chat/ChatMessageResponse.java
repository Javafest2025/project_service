package org.solace.scholar_ai.project_service.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.chat.ChatMessage;

/**
 * Response DTO for individual chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {

    private UUID messageId;

    private UUID sessionId;

    private ChatMessage.Role role;

    private String content;

    private Instant timestamp;

    private Integer tokenCount;

    /**
     * Context metadata for assistant messages
     */
    private PaperChatResponse.ContextMetadata contextMetadata;

    /**
     * User-friendly role name
     */
    public String getRoleName() {
        return role == ChatMessage.Role.USER ? "You" : "Assistant";
    }

    /**
     * Indicates if this message is from the user
     */
    public boolean isUser() {
        return role == ChatMessage.Role.USER;
    }
}
