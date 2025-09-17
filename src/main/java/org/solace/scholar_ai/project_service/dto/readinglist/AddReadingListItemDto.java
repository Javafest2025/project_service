package org.solace.scholar_ai.project_service.dto.readinglist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Data transfer object for adding a new paper to reading list")
public record AddReadingListItemDto(
        @Schema(description = "Paper ID to add to reading list", example = "550e8400-e29b-41d4-a716-446655440001")
                @NotNull(message = "Paper ID is required") UUID paperId,
        @Schema(
                        description = "Priority level",
                        example = "high",
                        allowableValues = {"low", "medium", "high", "critical"})
                String priority,
        @Schema(description = "Estimated reading time in minutes", example = "45")
                @Positive(message = "Estimated time must be positive") Integer estimatedTime,
        @Schema(
                        description = "User notes about the paper",
                        example = "Important paper for understanding transformer architecture")
                @Size(max = 1000, message = "Notes must not exceed 1000 characters")
                String notes,
        @Schema(description = "Tags for categorization", example = "[\"transformer\", \"attention\", \"nlp\"]")
                @Size(max = 10, message = "Maximum 10 tags allowed")
                List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags,
        @Schema(
                        description = "Perceived difficulty",
                        example = "hard",
                        allowableValues = {"easy", "medium", "hard", "expert"})
                String difficulty,
        @Schema(
                        description = "Relevance to project",
                        example = "high",
                        allowableValues = {"low", "medium", "high", "critical"})
                String relevance) {}
