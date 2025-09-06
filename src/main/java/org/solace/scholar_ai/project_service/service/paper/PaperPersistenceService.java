package org.solace.scholar_ai.project_service.service.paper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.mapping.paper.PaperMapper;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.author.AuthorRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.repository.papersearch.WebSearchOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperPersistenceService {

    private final PaperRepository paperRepository;
    private final PaperMapper paperMapper;
    private final WebSearchOperationRepository webSearchOperationRepository;
    private final PaperDeduplicationService paperDeduplicationService;
    private final AuthorRepository authorRepository;

    @Transactional
    public List<Paper> savePapers(List<PaperMetadataDto> paperDtos, String correlationId) {
        log.info("Persisting {} papers for correlation ID {}", paperDtos.size(), correlationId);

        // Filter out duplicate papers before processing
        List<PaperMetadataDto> newPapers = paperDeduplicationService.filterNewPapers(paperDtos);

        int duplicateCount = paperDtos.size() - newPapers.size();
        if (duplicateCount > 0) {
            log.info(
                    "Filtered out {} duplicate papers. Processing {} new papers for correlation ID {}",
                    duplicateCount,
                    newPapers.size(),
                    correlationId);
        }

        if (newPapers.isEmpty()) {
            log.info("No new papers to save for correlation ID {}", correlationId);
            return Collections.emptyList();
        }

        return newPapers.stream()
                .map(dto -> {
                    try {
                        Paper paper = paperMapper.fromMetadataDto(dto);
                        paper.setCorrelationId(correlationId);

                        // Handle author deduplication and relationships
                        if (paper.getPaperAuthors() != null) {
                            paper.getPaperAuthors().forEach(paperAuthor -> {
                                Author author = paperAuthor.getAuthor();
                                if (author != null && StringUtils.hasText(author.getName())) {
                                    // Try to find existing author by name
                                    Optional<Author> existingAuthor =
                                            authorRepository.findByNameContainingIgnoreCase(author.getName()).stream()
                                                    .filter(a -> a.getName().equalsIgnoreCase(author.getName()))
                                                    .findFirst();

                                    if (existingAuthor.isPresent()) {
                                        // Use existing author
                                        paperAuthor.setAuthor(existingAuthor.get());
                                        log.debug(
                                                "Using existing author: {}",
                                                existingAuthor.get().getName());
                                    } else {
                                        // Save new author first
                                        Author savedAuthor = authorRepository.save(author);
                                        paperAuthor.setAuthor(savedAuthor);
                                        log.debug("Created new author: {}", savedAuthor.getName());
                                    }
                                }
                                paperAuthor.setPaper(paper);
                            });
                        }

                        // Set bidirectional relationships for externalIds
                        if (paper.getExternalIds() != null) {
                            paper.getExternalIds().forEach(externalId -> externalId.setPaper(paper));
                        }

                        // Set bidirectional relationships for venue
                        if (paper.getVenue() != null) {
                            paper.getVenue().setPaper(paper);
                        }

                        // Set bidirectional relationships for metrics
                        if (paper.getMetrics() != null) {
                            paper.getMetrics().setPaper(paper);
                        }

                        Paper savedPaper = paperRepository.save(paper);
                        log.debug(
                                "Successfully saved new paper: {} (DOI: {})",
                                savedPaper.getTitle(),
                                savedPaper.getDoi());
                        return savedPaper;

                    } catch (Exception e) {
                        log.error(
                                "Failed to save paper: {} (DOI: {}). Error: {}",
                                dto.title(),
                                dto.doi(),
                                e.getMessage(),
                                e);
                        throw new RuntimeException("Failed to save paper: " + dto.title(), e);
                    }
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Paper> findPapersByCorrelationId(String correlationId) {
        log.debug("Finding papers for correlation ID {}", correlationId);
        return paperRepository.findByCorrelationId(correlationId);
    }

    @Transactional(readOnly = true)
    public List<PaperMetadataDto> findPaperDtosByCorrelationId(String correlationId) {
        log.debug("Finding paper DTOs for correlation ID {}", correlationId);
        List<Paper> papers = paperRepository.findByCorrelationId(correlationId);
        return papers.stream().map(paperMapper::toMetadataDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Paper> findPapersByProjectId(UUID projectId) {
        log.debug("Finding papers for project {}", projectId);

        // Get all correlation IDs for this project from WebSearchOperations
        List<String> correlationIds =
                webSearchOperationRepository.findByProjectIdOrderBySubmittedAtDesc(projectId).stream()
                        .map(operation -> operation.getCorrelationId())
                        .toList();

        if (correlationIds.isEmpty()) {
            log.debug("No web search operations found for project {}", projectId);
            return Collections.emptyList();
        }

        return paperRepository.findByCorrelationIdIn(correlationIds);
    }

    @Transactional(readOnly = true)
    public List<PaperMetadataDto> findPaperDtosByProjectId(UUID projectId) {
        log.debug("Finding paper DTOs for project {}", projectId);
        List<Paper> papers = findPapersByProjectId(projectId);
        return papers.stream().map(paperMapper::toMetadataDto).toList();
    }

    /**
     * Find papers marked as LaTeX context for a specific project
     */
    @Transactional(readOnly = true)
    public List<PaperMetadataDto> findLatexContextPapersByProjectId(UUID projectId) {
        log.debug("Finding LaTeX context papers for project {}", projectId);

        // Get all correlation IDs for this project from WebSearchOperations
        List<String> correlationIds =
                webSearchOperationRepository.findByProjectIdOrderBySubmittedAtDesc(projectId).stream()
                        .map(operation -> operation.getCorrelationId())
                        .toList();

        if (correlationIds.isEmpty()) {
            log.debug("No web search operations found for project {}", projectId);
            return Collections.emptyList();
        }

        List<Paper> latexContextPapers = paperRepository.findByCorrelationIdInAndIsLatexContext(correlationIds, true);
        return latexContextPapers.stream().map(paperMapper::toMetadataDto).toList();
    }

    /**
     * Toggle LaTeX context status for a paper
     */
    @Transactional
    public PaperMetadataDto toggleLatexContext(UUID paperId, boolean isLatexContext) {
        log.info("Toggling LaTeX context status for paper {} to {}", paperId, isLatexContext);

        Paper paper = paperRepository
                .findById(paperId)
                .orElseThrow(() -> new RuntimeException("Paper not found with id: " + paperId));

        paper.setIsLatexContext(isLatexContext);
        Paper savedPaper = paperRepository.save(paper);

        log.info("Updated LaTeX context status for paper {}: {}", paperId, isLatexContext);
        return paperMapper.toMetadataDto(savedPaper);
    }
}
