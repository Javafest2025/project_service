package org.solace.scholar_ai.project_service.dto.response.gap;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;

/**
 * Response DTO for gap analysis results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisResponse {

    private UUID id;
    private UUID paperId;
    private String correlationId;
    private String requestId;
    private GapAnalysis.GapStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;

    // Summary statistics
    private Integer totalGapsIdentified;
    private Integer validGapsCount;
    private Integer invalidGapsCount;
    private Integer modifiedGapsCount;

    // Gap details
    private List<ResearchGapResponse> gaps;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
}
