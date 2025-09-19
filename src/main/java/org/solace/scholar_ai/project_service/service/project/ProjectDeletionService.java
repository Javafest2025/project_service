package org.solace.scholar_ai.project_service.service.project;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.client.UserNotificationClient;
import org.solace.scholar_ai.project_service.model.project.Project;
import org.solace.scholar_ai.project_service.repository.chat.ChatMessageRepository;
import org.solace.scholar_ai.project_service.repository.chat.ChatSessionRepository;
import org.solace.scholar_ai.project_service.repository.gap.GapAnalysisRepository;
import org.solace.scholar_ai.project_service.repository.gap.GapValidationPaperRepository;
import org.solace.scholar_ai.project_service.repository.gap.ResearchGapRepository;
import org.solace.scholar_ai.project_service.repository.latex.DocumentRepository;
import org.solace.scholar_ai.project_service.repository.latex.DocumentVersionRepository;
import org.solace.scholar_ai.project_service.repository.latex.LatexAiChatMessageRepository;
import org.solace.scholar_ai.project_service.repository.latex.LatexAiChatSessionRepository;
import org.solace.scholar_ai.project_service.repository.latex.LatexDocumentCheckpointRepository;
import org.solace.scholar_ai.project_service.repository.note.NoteImageRepository;
import org.solace.scholar_ai.project_service.repository.note.PaperMentionRepository;
import org.solace.scholar_ai.project_service.repository.note.ProjectNoteRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperAuthorRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.repository.paper.ProjectPaperFavoriteRepository;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.solace.scholar_ai.project_service.repository.readinglist.ReadingListItemRepository;
import org.solace.scholar_ai.project_service.repository.summary.PaperSummaryRepository;
import org.solace.scholar_ai.project_service.repository.todo.TodoRepository;
import org.solace.scholar_ai.project_service.repository.todo.TodoSubtaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling comprehensive project deletion with cascade operations.
 * This service ensures that all related data is properly deleted when a project
 * is removed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(transactionManager = "transactionManager")
public class ProjectDeletionService {

    // Core repositories
    private final ProjectRepository projectRepository;
    private final PaperRepository paperRepository;

    // Note-related repositories
    private final ProjectNoteRepository projectNoteRepository;
    private final PaperMentionRepository paperMentionRepository;
    private final NoteImageRepository noteImageRepository;

    // Reading list repository
    private final ReadingListItemRepository readingListItemRepository;

    // Todo-related repositories
    private final TodoRepository todoRepository;
    private final TodoSubtaskRepository todoSubtaskRepository;

    // LaTeX-related repositories
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final LatexDocumentCheckpointRepository latexDocumentCheckpointRepository;
    private final LatexAiChatSessionRepository latexAiChatSessionRepository;
    private final LatexAiChatMessageRepository latexAiChatMessageRepository;

    // Chat-related repositories
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    // Paper-related repositories
    private final PaperAuthorRepository paperAuthorRepository;
    private final ProjectPaperFavoriteRepository projectPaperFavoriteRepository;
    private final PaperSummaryRepository paperSummaryRepository;

    // Gap analysis repositories
    private final GapAnalysisRepository gapAnalysisRepository;
    private final ResearchGapRepository researchGapRepository;
    private final GapValidationPaperRepository gapValidationPaperRepository;
    private final UserNotificationClient notificationClient;

    /**
     * Delete a project and all its related data in a comprehensive manner.
     * This method handles cascade deletion for all entities that reference the
     * project.
     */
    public void deleteProjectCompletely(UUID projectId, UUID userId) {
        log.info("Starting comprehensive deletion of project {} for user {}", projectId, userId);

        // Verify project exists and user has access
        Project project = projectRepository
                .findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new RuntimeException("Project not found or access denied"));

        log.info("Project found: {} - {}", project.getName(), project.getDescription());

        try {
            // Collect stats before deletion for notification
            long readingListCount = readingListItemRepository.countByProjectId(projectId);
            long mentionCount = paperMentionRepository.countByProjectId(projectId);
            long noteCount = projectNoteRepository.countByProjectId(projectId);
            long gapAnalysisCount =
                    gapAnalysisRepository.countByPaperIdIn(paperRepository.findIdsByProjectId(projectId));
            long summaryCount = paperSummaryRepository.countByPaperIdIn(paperRepository.findIdsByProjectId(projectId));
            int paperCount = paperRepository.findIdsByProjectId(projectId).size();

            // Step 1: Delete project-specific data (these have direct project_id
            // references)
            deleteProjectSpecificData(projectId);

            // Step 2: Delete papers and their related data
            deleteProjectPapers(projectId);

            // Step 3: Delete the project itself
            projectRepository.delete(project);

            log.info("Project {} deleted successfully with all related data", projectId);

            // Send notification (best-effort)
            try {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("projectName", project.getName());
                data.put("papersCount", paperCount);
                data.put("notesCount", noteCount);
                data.put("readingListCount", readingListCount);
                data.put("gapAnalysesCount", gapAnalysisCount);
                data.put("summariesCount", summaryCount);
                data.put("appUrl", "https://scholarai.me");
                notificationClient.send(userId, "PROJECT_DELETED", data);
            } catch (Exception ignore) {
            }

        } catch (Exception e) {
            log.error("Error during project deletion: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all project-specific data that directly references the project_id
     */
    private void deleteProjectSpecificData(UUID projectId) {
        log.info("Deleting project-specific data for project {}", projectId);

        // Delete reading list items
        long readingListCount = readingListItemRepository.countByProjectId(projectId);
        if (readingListCount > 0) {
            log.info("Deleting {} reading list items", readingListCount);
            readingListItemRepository.deleteByProjectId(projectId);
        }

        // Delete paper mentions in notes
        long mentionCount = paperMentionRepository.countByProjectId(projectId);
        if (mentionCount > 0) {
            log.info("Deleting {} paper mentions", mentionCount);
            paperMentionRepository.deleteByProjectId(projectId);
        }

        // Delete note images
        long imageCount = noteImageRepository.countByProjectId(projectId);
        if (imageCount > 0) {
            log.info("Deleting {} note images", imageCount);
            noteImageRepository.deleteByProjectId(projectId);
        }

        // Delete project notes
        long noteCount = projectNoteRepository.countByProjectId(projectId);
        if (noteCount > 0) {
            log.info("Deleting {} project notes", noteCount);
            projectNoteRepository.deleteByProjectId(projectId);
        }

        // Delete todos and subtasks
        List<String> todoIds = todoRepository.findIdsByProjectId(projectId.toString());
        if (!todoIds.isEmpty()) {
            log.info("Deleting {} todos and their subtasks", todoIds.size());
            todoSubtaskRepository.deleteByTodoIdIn(todoIds);
            todoRepository.deleteByRelatedProjectId(projectId.toString());
        }

        // Delete LaTeX documents and related data
        List<UUID> documentIds = documentRepository.findIdsByProjectId(projectId);
        if (!documentIds.isEmpty()) {
            log.info("Deleting {} LaTeX documents and related data", documentIds.size());

            // Delete LaTeX AI chat messages
            latexAiChatMessageRepository.deleteByDocumentIdIn(documentIds);

            // Delete LaTeX AI chat sessions
            latexAiChatSessionRepository.deleteByDocumentIdIn(documentIds);

            // Delete LaTeX document checkpoints
            latexDocumentCheckpointRepository.deleteByDocumentIdIn(documentIds);

            // Delete document versions
            documentVersionRepository.deleteByDocumentIdIn(documentIds);

            // Delete documents
            documentRepository.deleteByProjectId(projectId);
        }

        // Delete chat sessions and messages
        List<UUID> chatSessionIds = chatSessionRepository.findIdsByProjectId(projectId);
        if (!chatSessionIds.isEmpty()) {
            log.info("Deleting {} chat sessions and messages", chatSessionIds.size());
            chatMessageRepository.deleteBySessionIdIn(chatSessionIds);
            chatSessionRepository.deleteByProjectId(projectId);
        }

        // Delete project paper favorites
        long favoriteCount = projectPaperFavoriteRepository.countByProjectId(projectId);
        if (favoriteCount > 0) {
            log.info("Deleting {} project paper favorites", favoriteCount);
            projectPaperFavoriteRepository.deleteByProjectId(projectId);
        }

        log.info("Completed deletion of project-specific data for project {}", projectId);
    }

    /**
     * Delete all papers associated with the project and their related data.
     * This includes summaries, extractions, gap analyses, etc.
     */
    private void deleteProjectPapers(UUID projectId) {
        log.info("Deleting papers and related data for project {}", projectId);

        // Get all papers for this project
        List<UUID> paperIds = paperRepository.findIdsByProjectId(projectId);

        if (paperIds.isEmpty()) {
            log.info("No papers found for project {}", projectId);
            return;
        }

        log.info("Found {} papers to delete for project {}", paperIds.size(), projectId);

        // Delete gap validation papers
        long gapValidationCount = gapValidationPaperRepository.countByPaperIdIn(paperIds);
        if (gapValidationCount > 0) {
            log.info("Deleting {} gap validation papers", gapValidationCount);
            gapValidationPaperRepository.deleteByPaperIdIn(paperIds);
        }

        // Delete research gaps
        List<UUID> gapAnalysisIds = gapAnalysisRepository.findIdsByPaperIdIn(paperIds);
        if (!gapAnalysisIds.isEmpty()) {
            log.info("Deleting {} research gaps", gapAnalysisIds.size());
            researchGapRepository.deleteByGapAnalysisIdIn(gapAnalysisIds);
        }

        // Delete gap analyses
        long gapAnalysisCount = gapAnalysisRepository.countByPaperIdIn(paperIds);
        if (gapAnalysisCount > 0) {
            log.info("Deleting {} gap analyses", gapAnalysisCount);
            gapAnalysisRepository.deleteByPaperIdIn(paperIds);
        }

        // Delete paper summaries
        long summaryCount = paperSummaryRepository.countByPaperIdIn(paperIds);
        if (summaryCount > 0) {
            log.info("Deleting {} paper summaries", summaryCount);
            paperSummaryRepository.deleteByPaperIdIn(paperIds);
        }

        // Delete paper authors (must be deleted before papers due to foreign key
        // constraint)
        log.info("Deleting paper authors for {} papers", paperIds.size());
        for (UUID paperId : paperIds) {
            paperAuthorRepository.deleteByPaperId(paperId);
        }

        // Delete papers (this will cascade to related entities with proper JPA cascade
        // settings)
        log.info("Deleting {} papers", paperIds.size());
        paperRepository.deleteByIdIn(paperIds);

        log.info("Completed deletion of papers and related data for project {}", projectId);
    }
}
