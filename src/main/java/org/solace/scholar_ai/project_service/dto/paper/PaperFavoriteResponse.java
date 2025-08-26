package org.solace.scholar_ai.project_service.dto.paper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;
import org.solace.scholar_ai.project_service.dto.paper.PaperDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperFavoriteResponse {
    private UUID id;
    private UUID projectId;
    private UUID paperId;
    private UUID userId;
    private String notes;
    private String priority;
    private String tags;
    private Instant createdAt;
    private Instant updatedAt;
    private PaperDto paper;
}
