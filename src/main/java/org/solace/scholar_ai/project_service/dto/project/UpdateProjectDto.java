package org.solace.scholar_ai.project_service.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Data transfer object for updating an existing project")
public record UpdateProjectDto(
        @Schema(description = "Owner/creator user ID", example = "550e8400-e29b-41d4-a716-446655440001")
                @NotNull(message = "User ID is required") UUID userId,
        @Schema(description = "Project title/name", example = "AI in Healthcare Research")
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
        @Schema(
                        description = "Project status",
                        example = "ACTIVE",
                        allowableValues = {"ACTIVE", "PAUSED", "COMPLETED", "ARCHIVED"})
                String status,
        @Schema(description = "Progress percentage (0-100)", example = "75")
                @Min(value = 0, message = "Progress must be at least 0")
                @Max(value = 100, message = "Progress must not exceed 100")
                Integer progress,
        @Schema(description = "Human-readable last activity time", example = "2 hours ago") String lastActivity,
        @Schema(description = "Whether the project is starred by the user", example = "true") Boolean isStarred) {}
