package org.solace.scholar_ai.project_service.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Project statistics data transfer object")
public record ProjectStatsDto(
        @Schema(description = "Number of active projects", example = "5") long active,
        @Schema(description = "Number of paused projects", example = "2") long paused,
        @Schema(description = "Number of completed projects", example = "10") long completed,
        @Schema(description = "Number of archived projects", example = "3") long archived,
        @Schema(description = "Total number of projects", example = "20") long total) {}
