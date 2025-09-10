package org.solace.scholar_ai.project_service.dto.request.gap;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration DTO for gap analysis parameters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Configuration parameters for gap analysis")
public class GapAnalysisConfigDto {

    @Schema(description = "Maximum number of research gaps to identify", example = "10", defaultValue = "10")
    @Builder.Default
    private Integer maxGaps = 10;

    @Schema(
            description = "Depth of validation to perform",
            example = "thorough",
            allowableValues = {"quick", "thorough", "comprehensive"},
            defaultValue = "thorough")
    @Builder.Default
    private String validationDepth = "thorough";

    @Schema(description = "Whether to include research topic suggestions", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean includeTopicSuggestions = true;

    @Schema(description = "Whether to include implementation details", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean includeImplementationDetails = true;

    @Schema(
            description = "Minimum confidence threshold for gap validation (0.0 to 1.0)",
            example = "0.7",
            minimum = "0.0",
            maximum = "1.0",
            defaultValue = "0.7")
    @Builder.Default
    private Double confidenceThreshold = 0.7;

    @Schema(description = "Whether to include potential impact analysis", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean includeImpactAnalysis = true;

    @Schema(description = "Whether to include research hints and suggestions", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean includeResearchHints = true;

    @Schema(description = "Whether to include risks and challenges analysis", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean includeRisksAnalysis = true;

    @Schema(
            description = "Whether to include resource requirements estimation",
            example = "true",
            defaultValue = "true")
    @Builder.Default
    private Boolean includeResourceEstimation = true;
}
