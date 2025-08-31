package org.solace.scholar_ai.project_service.dto.paper;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;

@Schema(description = "Comprehensive paper metadata with all required fields")
public record PaperMetadataDto(
        @Schema(description = "Unique identifier for the paper", example = "b1a2c3d4-e5f6-7890-abcd-1234567890ef")
                String id,

        // Core Fields (present for almost all papers)
        @Schema(description = "Paper title", example = "Deep Learning for Natural Language Processing") String title,
        @JsonProperty("abstract")
                @Schema(name = "abstract", description = "Paper abstract. Ensured by MetadataEnrichmentService")
                String abstractText,
        @Schema(description = "List of authors. Ensured by MetadataEnrichmentService") List<AuthorDto> authors,
        @Schema(description = "Publication date (YYYY-MM-DD format). Ensured by MetadataEnrichmentService")
                LocalDate publicationDate,
        @Schema(description = "Digital Object Identifier. Ensured by MetadataEnrichmentService") String doi,

        // Identifiers and Source Information
        @Schema(description = "Unique ID from Semantic Scholar") String semanticScholarId,
        @Schema(description = "A dictionary of IDs from various sources") Map<String, Object> externalIds,
        @Schema(
                        description =
                                "The primary academic API source (e.g., 'Semantic Scholar', 'arXiv'). Added by SearchOrchestrator")
                String source,

        // PDF and Access Information
        @Schema(description = "The permanent URL to the PDF stored in B2 storage. Added by PDFProcessorService")
                String pdfContentUrl,
        @Schema(description = "The original open access PDF URL found from the source API") String pdfUrl,
        @Schema(description = "Flag indicating if the paper is open access") Boolean isOpenAccess,
        @Schema(description = "URL to the paper's landing page on the publisher's site") String paperUrl,

        // Publication and Venue Details
        @Schema(description = "Name of the journal or conference venue") String venueName,
        @Schema(description = "The publisher of the paper") String publisher,
        @Schema(description = "Type of publication (e.g., 'JournalArticle', 'Conference')")
                List<String> publicationTypes,
        @Schema(description = "Journal volume") String volume,
        @Schema(description = "Journal issue") String issue,
        @Schema(description = "Page numbers") String pages,

        // Metrics and Classification
        @Schema(description = "Total number of citations") Integer citationCount,
        @Schema(description = "Total number of references") Integer referenceCount,
        @Schema(description = "Number of influential citations") Integer influentialCitationCount,
        @Schema(description = "List of research fields associated with the paper") List<String> fieldsOfStudy,
        @Schema(description = "Whether this paper is added to LaTeX context") Boolean isLatexContext) {}
