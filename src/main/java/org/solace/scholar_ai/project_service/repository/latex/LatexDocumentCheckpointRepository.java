package org.solace.scholar_ai.project_service.repository.latex;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.latex.LatexDocumentCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LatexDocumentCheckpointRepository extends JpaRepository<LatexDocumentCheckpoint, Long> {

    /**
     * Find all checkpoints for a document, ordered by creation time (newest first)
     */
    List<LatexDocumentCheckpoint> findByDocument_IdOrderByCreatedAtDesc(UUID documentId);

    /**
     * Find all checkpoints for a session, ordered by creation time (newest first)
     */
    List<LatexDocumentCheckpoint> findBySession_IdOrderByCreatedAtDesc(UUID sessionId);

    /**
     * Find the current checkpoint for a document
     */
    Optional<LatexDocumentCheckpoint> findByDocument_IdAndIsCurrentTrue(UUID documentId);

    /**
     * Find checkpoints by document and session
     */
    List<LatexDocumentCheckpoint> findByDocument_IdAndSession_IdOrderByCreatedAtDesc(UUID documentId, UUID sessionId);

    /**
     * Clear current checkpoint flag for a document (before setting a new one)
     */
    @Modifying
    @Query("UPDATE LatexDocumentCheckpoint c SET c.isCurrent = false WHERE c.document.id = :documentId")
    void clearCurrentCheckpointForDocument(@Param("documentId") UUID documentId);

    /**
     * Set a checkpoint as current
     */
    @Modifying
    @Query("UPDATE LatexDocumentCheckpoint c SET c.isCurrent = true WHERE c.id = :checkpointId")
    void setCheckpointAsCurrent(@Param("checkpointId") Long checkpointId);

    /**
     * Count checkpoints for a document
     */
    long countByDocument_Id(UUID documentId);

    /**
     * Find recent checkpoints (last N checkpoints)
     */
    @Query("SELECT c FROM LatexDocumentCheckpoint c WHERE c.document.id = :documentId ORDER BY c.createdAt DESC")
    List<LatexDocumentCheckpoint> findRecentCheckpoints(
            @Param("documentId") UUID documentId, org.springframework.data.domain.Pageable pageable);

    /**
     * Delete old checkpoints, keeping only the most recent N
     */
    @Modifying
    @Query(
            value =
                    "DELETE FROM latex_document_checkpoints c WHERE c.document_id = :documentId AND c.id NOT IN (SELECT id FROM latex_document_checkpoints WHERE document_id = :documentId ORDER BY created_at DESC LIMIT :keepCount)",
            nativeQuery = true)
    void deleteOldCheckpoints(@Param("documentId") UUID documentId, @Param("keepCount") int keepCount);

    /**
     * Delete checkpoints by document IDs
     */
    @Modifying
    @Query("DELETE FROM LatexDocumentCheckpoint c WHERE c.document.id IN :documentIds")
    void deleteByDocumentIdIn(@Param("documentIds") List<UUID> documentIds);
}
