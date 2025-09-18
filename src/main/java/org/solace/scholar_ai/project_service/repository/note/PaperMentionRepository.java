package org.solace.scholar_ai.project_service.repository.note;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.note.PaperMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaperMentionRepository extends JpaRepository<PaperMention, UUID> {

    /**
     * Find all paper mentions for a specific note
     */
    List<PaperMention> findByNoteIdOrderByStartPosition(UUID noteId);

    /**
     * Find all paper mentions for a specific project
     */
    List<PaperMention> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Find paper mentions by paper ID
     */
    List<PaperMention> findByPaperIdOrderByCreatedAtDesc(UUID paperId);

    /**
     * Find paper mentions by project ID and paper ID
     */
    List<PaperMention> findByProjectIdAndPaperIdOrderByCreatedAtDesc(UUID projectId, UUID paperId);

    /**
     * Delete paper mentions by note ID
     */
    void deleteByNoteId(UUID noteId);

    /**
     * Delete paper mentions by project ID
     */
    void deleteByProjectId(UUID projectId);

    /**
     * Delete paper mentions by paper ID
     */
    void deleteByPaperId(UUID paperId);

    /**
     * Check if a paper is mentioned in any note for a project
     */
    @Query("SELECT COUNT(pm) > 0 FROM PaperMention pm WHERE pm.projectId = :projectId AND pm.paperId = :paperId")
    boolean existsByProjectIdAndPaperId(@Param("projectId") UUID projectId, @Param("paperId") UUID paperId);

    /**
     * Count paper mentions by project ID
     */
    long countByProjectId(UUID projectId);
}
