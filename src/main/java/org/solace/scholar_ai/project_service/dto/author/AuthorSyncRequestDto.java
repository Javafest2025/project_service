package org.solace.scholar_ai.project_service.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to sync author information from external multi-source API")
public record AuthorSyncRequestDto(
        @Schema(description = "ID of the user making the request") String userId,
        @NotBlank(message = "Author name is required")
                @Schema(description = "Name of the author to sync", example = "Andrew Y. Ng", required = true)
                String name,
        @Schema(
                        description = "Search strategy to use",
                        example = "comprehensive",
                        allowableValues = {"semantic_scholar_only", "fast", "comprehensive"},
                        defaultValue = "fast")
                String strategy,
        @Schema(description = "Force refresh even if recently synced", defaultValue = "false") Boolean forceRefresh) {}
