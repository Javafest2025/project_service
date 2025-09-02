package org.solace.scholar_ai.project_service.dto.paper;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;

@Schema(description = "Data transfer object for updating an existing paper")
public record UpdatePaperDto(
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
