package org.solace.scholar_ai.project_service.dto.note;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Paper suggestion for @ mentions in notes")
public record PaperSuggestionDto(
        @Schema(description = "Unique paper identifier", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Paper title", example = "Deep Learning for Natural Language Processing") String title,
        @Schema(
                        description = "Paper abstract (truncated)",
                        example = "This paper presents a comprehensive survey of deep learning techniques...")
                String abstractText,
        @Schema(description = "List of authors", example = "[\"John Doe\", \"Jane Smith\"]") List<String> authors,
        @Schema(description = "Publication date", example = "2024-01-15") LocalDate publicationDate,
        @Schema(description = "Venue name", example = "Nature Machine Intelligence") String venueName,
        @Schema(description = "Citation count", example = "150") Integer citationCount,
        @Schema(description = "Display text for the mention", example = "Deep Learning for NLP (2024)")
                String displayText) {}
