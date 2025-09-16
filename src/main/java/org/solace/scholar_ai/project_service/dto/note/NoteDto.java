package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Note data transfer object with all note information")
public record NoteDto(
        @Schema(description = "Unique note identifier", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Note title", example = "Research Ideas")
                @NotBlank(message = "Note title is required")
                @Size(max = 255, message = "Note title must not exceed 255 characters")
                String title,
        @Schema(
                        description = "Note content in markdown format",
                        example = "# Research Ideas\n\n## Key Concepts\n- **Machine Learning** approaches")
                @NotBlank(message = "Note content is required")
                String content,
        @Schema(description = "Note creation timestamp", example = "2024-01-15T10:30:00Z") Instant createdAt,
        @Schema(description = "Last modification timestamp", example = "2024-01-20T14:45:00Z") Instant updatedAt,
        @Schema(description = "Whether the note is marked as favorite", example = "true") Boolean isFavorite,
        @Schema(description = "Tags associated with the note", example = "[\"ideas\", \"planning\"]")
                List<String> tags) {}
