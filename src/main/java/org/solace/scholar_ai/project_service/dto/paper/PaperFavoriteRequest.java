package org.solace.scholar_ai.project_service.dto.paper;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperFavoriteRequest {
    private UUID paperId;
    private String notes;
    private String priority;
    private String tags;
}
