package org.solace.scholar_ai.project_service.dto.request.gap;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating gap analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to initiate gap analysis for a paper")
public class GapAnalysisRequest {

    @Schema(
            description = "ID of the paper to analyze",
            example = "8de08615-3c4f-4fc6-bad5-d409aac5ee67",
            required = true)
    private UUID paperId;

    @Schema(description = "Configuration parameters for the gap analysis", required = false)
    private GapAnalysisConfigDto config;
}
