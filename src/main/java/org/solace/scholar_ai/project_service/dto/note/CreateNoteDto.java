package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Data transfer object for creating a new note")
public record CreateNoteDto(
        @Schema(description = "Note title", example = "New Note Title")
                @NotBlank(message = "Note title is required")
                @Size(max = 255, message = "Note title must not exceed 255 characters")
                String title,
        @Schema(description = "Note content in markdown format", example = "# Note Content\n\nMarkdown content here...")
                @NotBlank(message = "Note content is required")
                String content,
        @Schema(description = "Tags associated with the note", example = "[\"tag1\", \"tag2\"]") List<String> tags) {}
