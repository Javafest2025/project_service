package org.solace.scholar_ai.project_service.dto.event.gap;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;

/**
 * Event DTO for gap analysis completion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisCompletedEvent {

    private String requestId;
    private String correlationId;
    private GapAnalysis.GapStatus status;
    private String message;
    private UUID gapAnalysisId;
    private UUID paperId;
    private Integer totalGaps;
    private Integer validGaps;
    private List<GapSummary> gaps;
    private Instant completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapSummary {
        private String gapId;
        private String name;
        private String description;
        private String category;
        private String validationStatus;
        private Double confidenceScore;
        private String potentialImpact;
        private String estimatedDifficulty;
        private String estimatedTimeline;
    }
}
