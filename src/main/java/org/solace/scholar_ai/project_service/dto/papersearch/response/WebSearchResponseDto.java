package org.solace.scholar_ai.project_service.dto.papersearch.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;

@Schema(description = "Response from web search operation")
public record WebSearchResponseDto(
        @Schema(
                        description = "Unique identifier for this search operation",
                        example = "123e4567-e89b-12d3-a456-426614174000")
                String projectId,
        @Schema(description = "Correlation ID for tracking", example = "corr-123-456") String correlationId,
        @Schema(description = "Search terms used", example = "[\"machine learning\", \"optimization\"]")
                List<String> queryTerms,
        @Schema(description = "Academic domain searched", example = "Computer Science") String domain,
        @Schema(description = "Number of papers requested", example = "10") Integer batchSize,
        @Schema(description = "Current status of the search", example = "SUBMITTED") String status,
        @Schema(description = "Timestamp when search was initiated") LocalDateTime submittedAt,
        @Schema(description = "Message about the operation", example = "Web search job submitted successfully")
                String message,
        @Schema(description = "List of papers found (populated when search completes)")
                List<PaperMetadataDto> papers) {}
