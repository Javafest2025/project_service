package org.solace.scholar_ai.project_service.dto.readinglist;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Reading list item data transfer object with all item information")
public record ReadingListItemDto(
        @Schema(description = "Unique reading list item identifier", example = "550e8400-e29b-41d4-a716-446655440000")
                UUID id,
        @Schema(description = "Paper ID reference", example = "550e8400-e29b-41d4-a716-446655440001") UUID paperId,
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
        @Schema(description = "When the item was added to reading list", example = "2023-12-01T10:00:00Z")
                Instant addedAt,
        @Schema(description = "When reading was started", example = "2023-12-01T14:30:00Z") Instant startedAt,
        @Schema(description = "When reading was completed", example = "2023-12-01T16:00:00Z") Instant completedAt,
        @Schema(description = "Estimated reading time in minutes", example = "45") Integer estimatedTime,
        @Schema(description = "Actual time spent reading in minutes", example = "30") Integer actualTime,
        @Schema(
                        description = "User notes about the paper",
                        example = "Key paper for transformer architecture understanding")
                String notes,
        @Schema(description = "Array of tags for categorization", example = "[\"transformer\", \"attention\", \"nlp\"]")
                List<String> tags,
        @Schema(description = "User rating (1-5 stars) for completed items", example = "4") Integer rating,
        @Schema(
                        description = "Perceived difficulty",
                        example = "hard",
                        allowableValues = {"easy", "medium", "hard", "expert"})
                String difficulty,
        @Schema(
                        description = "Relevance to project",
                        example = "high",
                        allowableValues = {"low", "medium", "high", "critical"})
                String relevance,
        @Schema(description = "Reading progress percentage (0-100)", example = "75") Integer readingProgress,
        @Schema(description = "Last time the paper was read", example = "2023-12-01T15:00:00Z") Instant lastReadAt,
        @Schema(description = "Number of times the paper has been read", example = "1") Integer readCount,
        @Schema(description = "Whether the item is bookmarked", example = "true") Boolean isBookmarked,
        @Schema(description = "Whether the item was recommended", example = "false") Boolean isRecommended,
        @Schema(description = "Source of recommendation", example = "ai-system") String recommendedBy,
        @Schema(description = "Reason for recommendation", example = "Based on your reading history")
                String recommendedReason) {}
