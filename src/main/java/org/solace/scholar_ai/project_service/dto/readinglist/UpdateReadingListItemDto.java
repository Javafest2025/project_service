package org.solace.scholar_ai.project_service.dto.readinglist;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Data transfer object for updating a reading list item")
public record UpdateReadingListItemDto(
        @Schema(
                        description = "Reading status",
                        example = "in-progress",
                        allowableValues = {"pending", "in-progress", "completed", "skipped"})
                String status,
        @Schema(
                        description = "Priority level",
                        example = "high",
                        allowableValues = {"low", "medium", "high", "critical"})
                String priority,
        @Schema(description = "Estimated reading time in minutes", example = "60")
                @Positive(message = "Estimated time must be positive") Integer estimatedTime,
        @Schema(description = "Actual time spent reading in minutes", example = "30")
                @Positive(message = "Actual time must be positive") Integer actualTime,
        @Schema(description = "User notes about the paper", example = "Updated notes with additional insights")
                @Size(max = 1000, message = "Notes must not exceed 1000 characters")
                String notes,
        @Schema(
                        description = "Tags for categorization",
                        example = "[\"transformer\", \"attention\", \"nlp\", \"deep-learning\"]")
                @Size(max = 10, message = "Maximum 10 tags allowed")
                List<@Size(max = 50, message = "Tag must not exceed 50 characters") String> tags,
        @Schema(description = "User rating (1-5 stars) for completed items", example = "4")
                @Min(value = 1, message = "Rating must be at least 1")
                @Max(value = 5, message = "Rating must be at most 5")
                Integer rating,
        @Schema(
                        description = "Perceived difficulty",
                        example = "expert",
                        allowableValues = {"easy", "medium", "hard", "expert"})
                String difficulty,
        @Schema(
                        description = "Relevance to project",
                        example = "critical",
                        allowableValues = {"low", "medium", "high", "critical"})
                String relevance,
        @Schema(description = "Reading progress percentage (0-100)", example = "75")
                @Min(value = 0, message = "Progress must be at least 0")
                @Max(value = 100, message = "Progress must be at most 100")
                Integer readingProgress,
        @Schema(description = "Whether the item is bookmarked", example = "true") Boolean isBookmarked) {}
