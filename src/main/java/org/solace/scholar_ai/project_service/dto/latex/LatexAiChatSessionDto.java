package org.solace.scholar_ai.project_service.dto.latex;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatexAiChatSessionDto {
    private UUID id;
    private UUID documentId;
    private UUID projectId;
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
