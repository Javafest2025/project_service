package org.solace.scholar_ai.project_service.dto.response.extraction;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing paper extraction status and progress information")
public class ExtractionStatusResponse {

    @Schema(description = "Paper ID")
    private UUID paperId;

    @Schema(description = "Whether the paper has been successfully extracted")
    private boolean isExtracted;

    @Schema(
            description = "Current extraction status",
            allowableValues = {"NOT_STARTED", "PENDING", "PROCESSING", "COMPLETED", "FAILED"})
    private String status;

    @Schema(description = "Extraction progress percentage (0-100)")
    private Double progress;

    @Schema(description = "Extraction job ID for tracking")
    private String extractionId;

    @Schema(description = "When extraction was started")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant startedAt;

    @Schema(description = "When extraction was completed")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant completedAt;

    @Schema(description = "Error message if extraction failed")
    private String error;

    @Schema(description = "Estimated time remaining in seconds")
    private Integer estimatedTimeRemaining;

    @Schema(description = "Summary of extracted content")
    private ExtractionSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Summary of extracted content for chat context")
    public static class ExtractionSummary {

        @Schema(description = "Number of sections extracted")
        private Integer sectionsCount;

        @Schema(description = "Number of figures extracted")
        private Integer figuresCount;

        @Schema(description = "Number of tables extracted")
        private Integer tablesCount;

        @Schema(description = "Number of equations extracted")
        private Integer equationsCount;

        @Schema(description = "Number of references extracted")
        private Integer referencesCount;

        @Schema(description = "Total number of pages")
        private Integer pageCount;

        @Schema(description = "Document language")
        private String language;

        @Schema(description = "Extraction coverage percentage")
        private Double extractionCoverage;

        @Schema(description = "Types of content available")
        private List<String> contentTypes;
    }
}
