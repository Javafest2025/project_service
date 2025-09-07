package org.solace.scholar_ai.project_service.dto.chat;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String sessionId;
    private String response;
    private String timestamp; // Changed to String for consistency with frontend
    private boolean success;
    private String error;
    private ChatContext context;

    // Legacy fields for backward compatibility
    private String message;
    private Map<String, Object> data;
    private String commandType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatContext {
        private List<String> sectionsUsed;
        private List<String> figuresReferenced;
        private List<String> tablesReferenced;
        private List<String> equationsUsed;
        private double confidenceScore;
        private List<String> contentSources;
    }
}
