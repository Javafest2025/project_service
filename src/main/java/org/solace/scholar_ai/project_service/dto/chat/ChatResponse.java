package org.solace.scholar_ai.project_service.dto.chat;

import java.time.LocalDateTime;
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
    private String message;
    private Map<String, Object> data;
    private String commandType;
    private LocalDateTime timestamp;
}
