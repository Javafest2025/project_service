package org.solace.scholar_ai.project_service.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Author information for academic papers")
public record AuthorDto(
        @Schema(description = "Full name of the author", example = "Dr. Jane Smith") String name,
        @Schema(description = "Semantic Scholar author ID", example = "2109704538") String authorId,
        @Schema(description = "Author's ORCID ID", example = "0000-0002-1825-0097") String orcid,
        @Schema(description = "Author's affiliation", example = "Stanford University") String affiliation) {}
