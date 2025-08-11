package org.solace.scholar_ai.project_service.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Project data transfer object with all project information")
public record ProjectDto(
        @Schema(description = "Unique project identifier", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Project title/name", example = "AI in Healthcare Research")
                @NotBlank(message = "Project name is required")
                @Size(max = 500, message = "Project name must not exceed 500 characters")
                String name,
        @Schema(
                        description = "Detailed project description",
                        example = "A comprehensive research project on AI applications in healthcare")
                @Size(max = 5000, message = "Project description must not exceed 5000 characters")
                String description,
        @Schema(description = "Research domain", example = "Computer Vision")
                @Size(max = 255, message = "Domain must not exceed 255 characters")
                String domain,
        @Schema(
                        description = "Research topics/keywords",
                        example = "[\"machine learning\", \"neural networks\", \"deep learning\"]")
                List<String> topics,
        @Schema(description = "Categorization tags", example = "[\"healthcare\", \"AI\", \"research\"]")
                List<String> tags,
        @Schema(description = "Owner/creator user ID", example = "550e8400-e29b-41d4-a716-446655440001")
                @NotNull(message = "User ID is required") UUID userId,
        @Schema(
                        description = "Project status",
                        example = "ACTIVE",
                        allowableValues = {"ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED"})
                @NotNull(message = "Project status is required") String status,
        @Schema(description = "Progress percentage (0-100)", example = "75")
                @Min(value = 0, message = "Progress must be at least 0")
                @Max(value = 100, message = "Progress must not exceed 100")
                @NotNull(message = "Progress is required") Integer progress,
        @Schema(description = "Total number of papers in project", example = "25")
                @Min(value = 0, message = "Total papers must be non-negative")
                @NotNull(message = "Total papers count is required") Integer totalPapers,
        @Schema(description = "Number of active processing tasks", example = "3")
                @Min(value = 0, message = "Active tasks must be non-negative")
                @NotNull(message = "Active tasks count is required") Integer activeTasks,
        @Schema(description = "Project creation timestamp", example = "2024-01-15T10:30:00Z") Instant createdAt,
        @Schema(description = "Last modification timestamp", example = "2024-01-20T14:45:00Z") Instant updatedAt,
        @Schema(description = "Human-readable last activity time", example = "2 hours ago") String lastActivity,
        @Schema(description = "Whether the project is starred by the user", example = "true")
                @NotNull(message = "Starred status is required") Boolean isStarred) {}
