package org.solace.scholar_ai.project_service.dto.chat;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private String message;
    private String userId;
    private String sessionId;
    private String sessionTitle;
    private String selectedText;
    private SelectionContext selectionContext;
    private Map<String, Object> context;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectionContext {
        private int from;
        private int to;
        private Integer pageNumber;
        private String sectionTitle;
    }
}
