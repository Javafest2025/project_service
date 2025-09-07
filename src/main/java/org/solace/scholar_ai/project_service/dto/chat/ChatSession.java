package org.solace.scholar_ai.project_service.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSession {
    private String sessionId;
    private String paperId;
    private String title;
    private String createdAt;
    private String lastMessageAt;
    private int messageCount;
}
