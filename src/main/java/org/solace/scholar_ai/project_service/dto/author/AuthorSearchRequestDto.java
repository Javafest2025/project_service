package org.solace.scholar_ai.project_service.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to search for author information")
public record AuthorSearchRequestDto(
        @NotBlank(message = "Author name is required")
                @Schema(description = "Name of the author to search for", example = "Dr. Jane Smith", required = true)
                String name,
        @Schema(description = "Institution for additional context", example = "Stanford University") String institution,
        @Schema(description = "Field of study for additional context", example = "Computer Science")
                String fieldOfStudy,
        @Schema(description = "Email for additional context") String email) {}
