package org.solace.scholar_ai.project_service.dto.library;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;

@Schema(description = "Response from library operation containing papers for a project")
public record LibraryResponseDto(
        @Schema(description = "Unique identifier for the project", example = "123e4567-e89b-12d3-a456-426614174000")
                String projectId,
        @Schema(description = "List of correlation IDs found for this project") List<String> correlationIds,
        @Schema(description = "Total number of papers found across all search operations") Integer totalPapers,
        @Schema(description = "Number of completed search operations for this project")
                Integer completedSearchOperations,
        @Schema(description = "Timestamp when the library data was retrieved") LocalDateTime retrievedAt,
        @Schema(description = "Summary message about the library contents") String message,
        @Schema(description = "List of all papers from the project's search operations")
                List<PaperMetadataDto> papers) {}
