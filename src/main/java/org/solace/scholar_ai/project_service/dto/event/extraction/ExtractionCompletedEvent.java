package org.solace.scholar_ai.project_service.dto.event.extraction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Event DTO received when extraction is completed
 */
public record ExtractionCompletedEvent(
        @NotBlank(message = "Job ID is required") String jobId,
        @NotBlank(message = "Paper ID is required") String paperId,
        @NotBlank(message = "Correlation ID is required") String correlationId,
        @NotBlank(message = "Status is required") String status, // completed, failed
        String message,

        // Extraction result data (JSON string from extractor)
        String extractionResult,

        // Processing metadata
        Double processingTime,
        Double extractionCoverage,
        String confidenceScores, // JSON string
        String errors, // JSON array of errors
        String warnings, // JSON array of warnings
        @NotNull Instant completedAt) {}
