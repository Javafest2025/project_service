package org.solace.scholar_ai.project_service.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for complete chat session history
 * Contains session info and all messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatSessionHistoryResponse {

    private UUID sessionId;

    private UUID paperId;

    private String title;

    private Instant createdAt;

    private Instant lastMessageAt;

    private Integer messageCount;

    private Boolean isActive;

    /**
     * All messages in the session, ordered chronologically
     */
    private List<ChatMessageResponse> messages;

    /**
     * Session statistics
     */
    private SessionStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionStats {
        private Integer totalMessages;
        private Integer userMessages;
        private Integer assistantMessages;
        private Instant firstMessageAt;
        private Instant lastMessageAt;
        private Double averageResponseTime; // in seconds
    }
}
