package org.solace.scholar_ai.project_service.dto.library;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request DTO for library operations")
public record LibraryRequestDto(
        @Schema(description = "User ID for validation", example = "123e4567-e89b-12d3-a456-426614174000")
                @NotNull(message = "User ID is required") UUID userId,
        @Schema(description = "Project ID for the library operation", example = "123e4567-e89b-12d3-a456-426614174000")
                @NotNull(message = "Project ID is required") UUID projectId) {}
