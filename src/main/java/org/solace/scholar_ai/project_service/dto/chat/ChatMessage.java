package org.solace.scholar_ai.project_service.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessage {
    private String id;
    private String role; // 'USER' or 'ASSISTANT'
    private String content;
    private String timestamp;
}
