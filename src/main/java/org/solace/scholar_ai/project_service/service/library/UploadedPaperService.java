package org.solace.scholar_ai.project_service.service.library;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.library.upload.UploadedPaperRequest;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.mapping.paper.PaperMapper;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation;
import org.solace.scholar_ai.project_service.model.project.Project;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.repository.papersearch.WebSearchOperationRepository;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.solace.scholar_ai.project_service.service.author.AuthorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadedPaperService {

    private final PaperRepository paperRepository;
    private final PaperMapper paperMapper;
    private final ProjectRepository projectRepository;
    private final WebSearchOperationRepository webSearchOperationRepository;
    private final AuthorService authorService;

    /**
     * Process and save an uploaded paper to a project's library
     */
    @Transactional
    public PaperMetadataDto saveUploadedPaper(UploadedPaperRequest request, UUID userId) {
        UUID projectId = request.projectId();
        log.info("Processing uploaded paper for project: {} by user: {}", projectId, userId);

        // Validate project access
        validateProjectAccess(projectId, userId);

        // Generate a unique correlation ID for this uploaded paper
        String correlationId = "uploaded-" + UUID.randomUUID().toString();

        try {
            // Create web search operation entry for consistency
            createWebSearchOperationEntry(projectId, correlationId, request);

            // Convert UploadedPaperRequest to PaperMetadataDto
            PaperMetadataDto paperDto = convertToPaperMetadataDto(request, correlationId);

            // Convert to Paper entity WITHOUT authors (to avoid transient entity issues)
            PaperMetadataDto paperDtoWithoutAuthors = new PaperMetadataDto(
                    paperDto.id(),
                    paperDto.title(),
                    paperDto.abstractText(),
                    new ArrayList<>(), // Empty authors list
                    paperDto.publicationDate(),
                    paperDto.doi(),
                    paperDto.semanticScholarId(),
                    paperDto.externalIds(),
                    paperDto.source(),
                    paperDto.pdfContentUrl(),
                    paperDto.pdfUrl(),
                    paperDto.isOpenAccess(),
                    paperDto.paperUrl(),
                    paperDto.venueName(),
                    paperDto.publisher(),
                    paperDto.publicationTypes(),
                    paperDto.volume(),
                    paperDto.issue(),
                    paperDto.pages(),
                    paperDto.citationCount(),
                    paperDto.referenceCount(),
                    paperDto.influentialCitationCount(),
                    paperDto.fieldsOfStudy(),
                    paperDto.isLatexContext());

            Paper paper = paperMapper.fromMetadataDto(paperDtoWithoutAuthors);
            paper.setCorrelationId(correlationId);

            // Set bidirectional relationships
            if (paper.getExternalIds() != null) {
                paper.getExternalIds().forEach(externalId -> externalId.setPaper(paper));
            }

            if (paper.getVenue() != null) {
                paper.getVenue().setPaper(paper);
            }

            if (paper.getMetrics() != null) {
                paper.getMetrics().setPaper(paper);
            }

            // Save the paper first (without authors)
            Paper savedPaper = paperRepository.save(paper);

            // Now handle authors properly - create/find them and link to the paper
            if (request.authors() != null && !request.authors().isEmpty()) {
                for (int i = 0; i < request.authors().size(); i++) {
                    String authorName = request.authors().get(i).name(); // Extract name from AuthorDto
                    if (authorName != null && !authorName.trim().isEmpty()) {
                        // Use AuthorService to properly create/find author and link to paper
                        authorService.getOrCreateAuthorForPaper(
                                authorName.trim(),
                                savedPaper,
                                i + 1, // authorOrder (1-based)
                                false // isCorrespondingAuthor
                                );
                    }
                }
                log.info(
                        "Created {} author relationships for paper: {}",
                        request.authors().size(),
                        savedPaper.getId());
            }

            log.info(
                    "Successfully saved uploaded paper: {} (ID: {}) with correlation ID: {}",
                    savedPaper.getTitle(),
                    savedPaper.getId(),
                    correlationId);

            // Return the saved paper as DTO
            return paperMapper.toMetadataDto(savedPaper);

        } catch (Exception e) {
            log.error("Failed to save uploaded paper for project {}: {}", projectId, e.getMessage(), e);
            throw new RuntimeException("Failed to save uploaded paper: " + e.getMessage(), e);
        }
    }

    /**
     * Create a web search operation entry for uploaded paper consistency
     */
    private void createWebSearchOperationEntry(UUID projectId, String correlationId, UploadedPaperRequest request) {
        try {
            WebSearchOperation operation = WebSearchOperation.builder()
                    .correlationId(correlationId)
                    .projectId(projectId)
                    .queryTerms("[\"Uploaded Paper: " + request.title() + "\"]") // JSON string format
                    .domain("Uploaded")
                    .batchSize(1) // Single paper upload
                    .status(WebSearchOperation.SearchStatus.COMPLETED) // Already completed since it's uploaded
                    .submittedAt(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .totalPapersFound(1)
                    .searchDurationMs(0L) // Instant upload
                    .build();

            webSearchOperationRepository.save(operation);
            log.debug("Created web search operation entry for uploaded paper: {}", correlationId);

        } catch (Exception e) {
            log.error(
                    "Failed to create web search operation entry for correlation ID {}: {}",
                    correlationId,
                    e.getMessage(),
                    e);
            // Don't throw here - paper upload should still succeed even if web search
            // operation creation fails
        }
    }

    /**
     * Convert UploadedPaperRequest to PaperMetadataDto with proper defaults
     */
    private PaperMetadataDto convertToPaperMetadataDto(UploadedPaperRequest request, String correlationId) {
        // Set defaults for optional fields
        LocalDate publicationDate = request.publicationDate() != null ? request.publicationDate() : LocalDate.now();
        Boolean isOpenAccess = request.isOpenAccess() != null ? request.isOpenAccess() : true;
        List<String> publicationTypes =
                request.publicationTypes() != null ? request.publicationTypes() : List.of("Uploaded Document");
        List<String> fieldsOfStudy = request.fieldsOfStudy() != null ? request.fieldsOfStudy() : new ArrayList<>();

        // Ensure pdfUrl is set to pdfContentUrl if not provided
        String pdfUrl = request.pdfUrl() != null ? request.pdfUrl() : request.pdfContentUrl();

        return new PaperMetadataDto(
                null, // ID will be generated
                request.title(),
                request.abstractText(),
                request.authors() != null ? request.authors() : new ArrayList<>(),
                publicationDate,
                request.doi(),
                request.semanticScholarId(),
                request.externalIds() != null ? request.externalIds() : new java.util.HashMap<>(),
                request.source(),
                request.pdfContentUrl(),
                pdfUrl,
                isOpenAccess,
                request.paperUrl(),
                request.venueName(),
                request.publisher(),
                publicationTypes,
                request.volume(),
                request.issue(),
                request.pages(),
                request.citationCount(),
                request.referenceCount(),
                request.influentialCitationCount(),
                fieldsOfStudy,
                false); // isLatexContext - default to false for uploaded papers
    }

    /**
     * Validate that the user has access to the project
     */
    private void validateProjectAccess(UUID projectId, UUID userId) {
        // Check if user is the owner of the project
        Project project = projectRepository.findByIdAndUserId(projectId, userId).orElse(null);

        if (project == null) {
            throw new RuntimeException("Project not found or access denied");
        }

        log.debug("User {} is the owner of project {}", userId, projectId);
    }
}
