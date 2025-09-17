package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response DTO for AI content generation")
public record AIContentResponseDto(
        @Schema(
                        description = "Generated content in markdown format",
                        example = "## Summary\n\nMachine learning algorithms are...")
                String content,
        @Schema(description = "Status of the generation", example = "success") String status,
        @Schema(description = "Error message if generation failed", example = "Failed to generate content")
                String error) {}
