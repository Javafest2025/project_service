package org.solace.scholar_ai.project_service.dto.paper;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;

@Schema(description = "Basic paper data transfer object")
public record PaperDto(
        @Schema(description = "Unique identifier for the paper") UUID id,
        @Schema(description = "Correlation ID for search operations") String correlationId,
        @Schema(description = "Paper title") String title,
        @Schema(description = "Paper abstract") String abstractText,
        @Schema(description = "List of authors") List<AuthorDto> authors,
        @Schema(description = "Publication date") LocalDate publicationDate,
        @Schema(description = "Digital Object Identifier") String doi,
        @Schema(description = "Semantic Scholar ID") String semanticScholarId,
        @Schema(description = "Source platform") String source,
        @Schema(description = "PDF content URL") String pdfContentUrl,
        @Schema(description = "Original PDF URL") String pdfUrl,
        @Schema(description = "Open access flag") Boolean isOpenAccess,
        @Schema(description = "Paper URL") String paperUrl,
        @Schema(description = "Publication types") List<String> publicationTypes,
        @Schema(description = "Fields of study") List<String> fieldsOfStudy,
        @Schema(description = "External identifiers") Map<String, Object> externalIds,
        @Schema(description = "Whether this paper is added to LaTeX context") Boolean isLatexContext) {}
