package org.solace.scholar_ai.project_service.dto.request.extraction;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Message DTO sent to extractor service via RabbitMQ
 */
public record ExtractorMessageRequest(
        @JsonProperty("jobId") @NotBlank(message = "Job ID is required") String jobId,
        @JsonProperty("paperId") @NotBlank(message = "Paper ID is required") String paperId,
        @JsonProperty("correlationId") @NotBlank(message = "Correlation ID is required") String correlationId,
        @JsonProperty("b2Url") @NotBlank(message = "B2 URL is required") String b2Url,

        // Extraction options
        @JsonProperty("extractText") @NotNull Boolean extractText,
        @JsonProperty("extractFigures") @NotNull Boolean extractFigures,
        @JsonProperty("extractTables") @NotNull Boolean extractTables,
        @JsonProperty("extractEquations") @NotNull Boolean extractEquations,
        @JsonProperty("extractCode") @NotNull Boolean extractCode,
        @JsonProperty("extractReferences") @NotNull Boolean extractReferences,
        @JsonProperty("useOcr") @NotNull Boolean useOcr,
        @JsonProperty("detectEntities") @NotNull Boolean detectEntities) {}
