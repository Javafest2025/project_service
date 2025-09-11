package org.solace.scholar_ai.project_service.repository.latex;

import org.solace.scholar_ai.project_service.model.latex.LatexDocumentCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LatexDocumentCheckpointRepository extends JpaRepository<LatexDocumentCheckpoint, Long> {

    /**
     * Find all checkpoints for a document, ordered by creation time (newest first)
     */
    List<LatexDocumentCheckpoint> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    /**
     * Find all checkpoints for a session, ordered by creation time (newest first)
     */
    List<LatexDocumentCheckpoint> findBySessionIdOrderByCreatedAtDesc(Long sessionId);

    /**
     * Find the current checkpoint for a document
     */
    Optional<LatexDocumentCheckpoint> findByDocumentIdAndIsCurrentTrue(Long documentId);

    /**
     * Find checkpoints by document and session
     */
    List<LatexDocumentCheckpoint> findByDocumentIdAndSessionIdOrderByCreatedAtDesc(
            Long documentId, Long sessionId);

    /**
     * Clear current checkpoint flag for a document (before setting a new one)
     */
    @Modifying
    @Query("UPDATE LatexDocumentCheckpoint c SET c.isCurrent = false WHERE c.documentId = :documentId")
    void clearCurrentCheckpointForDocument(@Param("documentId") Long documentId);

    /**
     * Set a checkpoint as current
     */
    @Modifying
    @Query("UPDATE LatexDocumentCheckpoint c SET c.isCurrent = true WHERE c.id = :checkpointId")
    void setCheckpointAsCurrent(@Param("checkpointId") Long checkpointId);

    /**
     * Count checkpoints for a document
     */
    long countByDocumentId(Long documentId);

    /**
     * Find recent checkpoints (last N checkpoints)
     */
    @Query("SELECT c FROM LatexDocumentCheckpoint c WHERE c.documentId = :documentId " +
           "ORDER BY c.createdAt DESC LIMIT :limit")
    List<LatexDocumentCheckpoint> findRecentCheckpoints(@Param("documentId") Long documentId, @Param("limit") int limit);

    /**
     * Delete old checkpoints, keeping only the most recent N
     */
    @Modifying
    @Query("DELETE FROM LatexDocumentCheckpoint c WHERE c.documentId = :documentId " +
           "AND c.id NOT IN (SELECT c2.id FROM LatexDocumentCheckpoint c2 " +
           "WHERE c2.documentId = :documentId ORDER BY c2.createdAt DESC LIMIT :keepCount)")
    void deleteOldCheckpoints(@Param("documentId") Long documentId, @Param("keepCount") int keepCount);
}
