package org.solace.scholar_ai.project_service.dto.request.extraction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Message DTO sent to extractor service via RabbitMQ
 */
public record ExtractorMessageRequest(
        @NotBlank(message = "Job ID is required") String jobId,
        @NotBlank(message = "Paper ID is required") String paperId,
        @NotBlank(message = "Correlation ID is required") String correlationId,
        @NotBlank(message = "B2 URL is required") String b2Url,

        // Extraction options
        @NotNull Boolean extractText,
        @NotNull Boolean extractFigures,
        @NotNull Boolean extractTables,
        @NotNull Boolean extractEquations,
        @NotNull Boolean extractCode,
        @NotNull Boolean extractReferences,
        @NotNull Boolean useOcr,
        @NotNull Boolean detectEntities) {}
