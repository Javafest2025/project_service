package org.solace.scholar_ai.project_service.dto.author;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Comprehensive author information from multi-source academic APIs")
public record AuthorDto(
        @Schema(description = "Author's full name", example = "Dr. Andrew Y. Ng") String name,
        @Schema(description = "Primary institutional affiliation", example = "Stanford University")
                String primaryAffiliation,
        @Schema(description = "All known institutional affiliations") List<String> allAffiliations,

        // External identifiers
        @Schema(description = "Semantic Scholar author ID", example = "144388777") String semanticScholarId,
        @Schema(description = "ORCID identifier", example = "0000-0001-9194-3909") String orcidId,
        @Schema(description = "Google Scholar profile ID") String googleScholarId,
        @Schema(description = "OpenAlex author ID", example = "https://openalex.org/A5112456378") String openalexId,

        // Metrics
        @Schema(description = "Total citation count", example = "148873") Integer citationCount,
        @Schema(description = "H-index score", example = "4") Integer hIndex,
        @Schema(description = "i10-index score", example = "0") Integer i10Index,
        @Schema(description = "Total number of published papers", example = "405") Integer paperCount,

        // Publication timeline
        @Schema(description = "Year of first publication", example = "2026") Integer firstPublicationYear,
        @Schema(description = "Year of most recent publication", example = "2030") Integer lastPublicationYear,

        // Research information
        @Schema(description = "Research areas and interests") List<String> researchAreas,
        @Schema(description = "Recent publications with metadata") List<Object> recentPublications,

        // Data source tracking
        @Schema(description = "APIs used to gather this data") List<String> dataSources,
        @Schema(description = "Data quality score (0.0 to 1.0)", example = "1.0") Double dataQualityScore,
        @Schema(description = "Search strategy used", example = "comprehensive") String searchStrategy,
        @Schema(description = "Data sources attempted") List<String> sourcesAttempted,
        @Schema(description = "Data sources that returned data") List<String> sourcesSuccessful,

        // Sync status
        @Schema(description = "Whether data has been synced from external APIs") Boolean isSynced,
        @Schema(description = "Last time data was synced") Instant lastSyncAt,
        @Schema(description = "Error message from last sync attempt") String syncError,

        // Legacy fields for compatibility
        @Schema(description = "Author's homepage URL") String homepageUrl,
        @Schema(description = "Author's email address") String email,
        @Schema(description = "Profile image URL") String profileImageUrl,
        @Schema(description = "Record creation timestamp") Instant createdAt,
        @Schema(description = "Record last update timestamp") Instant updatedAt) {}
