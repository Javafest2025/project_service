package org.solace.scholar_ai.project_service.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chat session information
 * Contains session metadata and basic information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSessionResponse {

    private UUID sessionId;

    private UUID paperId;

    private String title;

    private Instant createdAt;

    private Instant lastMessageAt;

    private Integer messageCount;

    private Boolean isActive;

    /**
     * Preview of the last message in the session
     */
    private String lastMessagePreview;

    /**
     * Indicates if this is the current/active session
     */
    private Boolean isCurrent;
}
