package org.solace.scholar_ai.project_service.dto.library.upload;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;

@Schema(description = "Request DTO for uploading a paper to a project library")
public record UploadedPaperRequest(
        @Schema(description = "User ID for authentication", example = "123e4567-e89b-12d3-a456-426614174000")
                @NotNull(message = "User ID is required") UUID userId,
        @Schema(description = "Project ID to upload the paper to", example = "123e4567-e89b-12d3-a456-426614174000")
                @NotNull(message = "Project ID is required") UUID projectId,
        @Schema(description = "Paper title", example = "Deep Learning for Natural Language Processing")
                @NotBlank(message = "Title is required")
                String title,
        @Schema(description = "Paper abstract", example = "This paper presents a novel approach...")
                String abstractText,
        @Schema(description = "List of authors") List<AuthorDto> authors,
        @Schema(description = "Publication date in YYYY-MM-DD format", example = "2024-01-15")
                LocalDate publicationDate,
        @Schema(description = "Digital Object Identifier", example = "10.1000/182") String doi,
        @Schema(description = "Semantic Scholar ID", example = "123456789") String semanticScholarId,
        @Schema(description = "External identifiers from various sources") Map<String, Object> externalIds,
        @Schema(description = "Source of the paper", example = "Uploaded") @NotBlank(message = "Source is required")
                String source,
        @Schema(description = "B2 download URL for PDF content")
                @NotBlank(message = "PDF content URL is required")
                @JsonProperty("pdfContentUrl")
                String pdfContentUrl,
        @Schema(description = "B2 download URL for PDF") @JsonProperty("pdfUrl") String pdfUrl,
        @Schema(description = "Whether the paper is open access", example = "true") Boolean isOpenAccess,
        @Schema(description = "URL to the paper's landing page") String paperUrl,
        @Schema(description = "Name of the journal or conference venue") String venueName,
        @Schema(description = "Publisher name") String publisher,
        @Schema(description = "Types of publication") List<String> publicationTypes,
        @Schema(description = "Journal volume") String volume,
        @Schema(description = "Journal issue") String issue,
        @Schema(description = "Page numbers") String pages,
        @Schema(description = "Number of citations") Integer citationCount,
        @Schema(description = "Number of references") Integer referenceCount,
        @Schema(description = "Number of influential citations") Integer influentialCitationCount,
        @Schema(description = "Research fields associated with the paper") List<String> fieldsOfStudy,
        @Schema(description = "Upload timestamp", example = "2024-01-15T10:30:00.000Z") String uploadedAt,
        @Schema(description = "File size in bytes", example = "2987520") Long fileSize,
        @Schema(description = "Original file name", example = "The-CLRS-Algorithmic-Reasoning-Benchmark.pdf")
                String fileName) {}
