package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request DTO for AI content generation")
public record AIContentRequestDto(
        @Schema(
                        description = "User prompt for content generation",
                        example = "write a summary of machine learning algorithms")
                @NotBlank(message = "Prompt is required")
                @Size(max = 1000, message = "Prompt must not exceed 1000 characters")
                String prompt,
        @Schema(
                        description = "Current note context to help AI understand the context",
                        example = "This note is about research methodology...")
                @Size(max = 5000, message = "Context must not exceed 5000 characters")
                String context) {}
