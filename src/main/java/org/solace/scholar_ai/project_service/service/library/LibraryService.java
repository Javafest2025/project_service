package org.solace.scholar_ai.project_service.service.library;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.library.LibraryResponseDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation;
import org.solace.scholar_ai.project_service.model.project.Project;
import org.solace.scholar_ai.project_service.repository.papersearch.WebSearchOperationRepository;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.solace.scholar_ai.project_service.service.paper.PaperPersistenceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private final PaperPersistenceService paperPersistenceService;
    private final WebSearchOperationRepository webSearchOperationRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true)
    public LibraryResponseDto getProjectLibrary(UUID projectId, UUID userId) {
        log.info("Retrieving library for project: {} by user: {}", projectId, userId);

        // Validate project access
        validateProjectAccess(projectId, userId);

        // Get all web search operations for this project
        List<WebSearchOperation> searchOperations =
                webSearchOperationRepository.findByProjectIdOrderBySubmittedAtDesc(projectId);

        if (searchOperations.isEmpty()) {
            log.debug("No search operations found for project {}", projectId);
            return createEmptyLibraryResponse(projectId);
        }

        // Extract correlation IDs
        List<String> correlationIds = searchOperations.stream()
                .map(WebSearchOperation::getCorrelationId)
                .toList();

        // Get papers for all correlation IDs
        List<PaperMetadataDto> papers = paperPersistenceService.findPaperDtosByProjectId(projectId);

        // Count completed search operations
        int completedOperations = (int) searchOperations.stream()
                .filter(op -> op.getStatus() == WebSearchOperation.SearchStatus.COMPLETED)
                .count();

        String message = generateLibraryMessage(papers.size(), completedOperations, searchOperations.size());

        log.info(
                "Retrieved library for project {}: {} papers from {} completed operations out of {} total operations",
                projectId,
                papers.size(),
                completedOperations,
                searchOperations.size());

        return new LibraryResponseDto(
                projectId.toString(),
                correlationIds,
                papers.size(),
                completedOperations,
                LocalDateTime.now(),
                message,
                papers);
    }

    @Transactional(readOnly = true)
    public List<PaperMetadataDto> getLatestProjectPapers(UUID projectId, UUID userId) {
        log.info("Retrieving latest papers for project: {} by user: {}", projectId, userId);

        // Validate project access
        validateProjectAccess(projectId, userId);

        // Get all web search operations for this project, ordered by submission date
        // (latest first)
        List<WebSearchOperation> searchOperations =
                webSearchOperationRepository.findByProjectIdOrderBySubmittedAtDesc(projectId);

        if (searchOperations.isEmpty()) {
            log.debug("No search operations found for project {}", projectId);
            return List.of();
        }

        // Get the latest correlation ID (first in the list since it's ordered by
        // submittedAt DESC)
        String latestCorrelationId = searchOperations.get(0).getCorrelationId();
        log.debug("Latest correlation ID for project {}: {}", projectId, latestCorrelationId);

        // Get papers for the latest correlation ID
        List<PaperMetadataDto> latestPapers = paperPersistenceService.findPaperDtosByCorrelationId(latestCorrelationId);

        log.info(
                "Retrieved {} papers from latest correlation ID {} for project {}",
                latestPapers.size(),
                latestCorrelationId,
                projectId);

        return latestPapers;
    }

    private LibraryResponseDto createEmptyLibraryResponse(UUID projectId) {
        return new LibraryResponseDto(
                projectId.toString(),
                List.of(),
                0,
                0,
                LocalDateTime.now(),
                "No search operations found for this project",
                List.of());
    }

    private String generateLibraryMessage(int totalPapers, int completedOperations, int totalOperations) {
        if (totalPapers == 0) {
            return "No papers found in project library";
        }

        if (completedOperations == totalOperations) {
            return String.format(
                    "Project library contains %d papers from %d completed search operations",
                    totalPapers, completedOperations);
        } else {
            return String.format(
                    "Project library contains %d papers from %d completed operations (%d operations still in progress)",
                    totalPapers, completedOperations, totalOperations - completedOperations);
        }
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
    }
}
