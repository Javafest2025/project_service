package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Data transfer object for updating an existing note")
public record UpdateNoteDto(
        @Schema(description = "Note title", example = "Updated Note Title")
                @Size(max = 255, message = "Note title must not exceed 255 characters")
                String title,
        @Schema(
                        description = "Note content in markdown format",
                        example = "# Updated Content\n\nNew markdown content...")
                String content,
        @Schema(description = "Tags associated with the note", example = "[\"updated\", \"tags\"]")
                List<String> tags) {}
