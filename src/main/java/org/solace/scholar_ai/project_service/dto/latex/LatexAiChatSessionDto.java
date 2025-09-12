package org.solace.scholar_ai.project_service.dto.latex;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatexAiChatSessionDto {
    private Long id;
    private Long documentId;
    private Long projectId;
    private String sessionTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
    private Long messageCount;
    private LocalDateTime lastMessageTime;
    private List<LatexAiChatMessageDto> messages;
    private List<LatexDocumentCheckpointDto> checkpoints;
    private LatexDocumentCheckpointDto currentCheckpoint;
}
