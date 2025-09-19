package org.solace.scholar_ai.project_service.repository.citation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.citation.CitationCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitationCheckRepository extends JpaRepository<CitationCheck, UUID> {

    /**
     * Find the latest completed citation check for a document
     */
    @Query(
            "SELECT c FROM CitationCheck c WHERE c.documentId = :documentId AND c.status = 'DONE' ORDER BY c.createdAt DESC")
    Optional<CitationCheck> findLatestCompletedByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Find citation check by document ID and status
     */
    Optional<CitationCheck> findByDocumentIdAndStatus(UUID documentId, CitationCheck.Status status);

    /**
     * Find all citation checks for a project
     */
    List<CitationCheck> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    /**
     * Find all citation checks for a document
     */
    List<CitationCheck> findByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    /**
     * Find currently running jobs
     */
    @Query("SELECT c FROM CitationCheck c WHERE c.status IN ('QUEUED', 'RUNNING') ORDER BY c.createdAt ASC")
    List<CitationCheck> findRunningJobs();

    /**
     * Count completed checks for a document
     */
    @Query("SELECT COUNT(c) FROM CitationCheck c WHERE c.documentId = :documentId AND c.status = 'DONE'")
    Long countCompletedByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Delete old completed checks for a document (keep only the latest)
     */
    @Query("DELETE FROM CitationCheck c WHERE c.documentId = :documentId AND c.status = 'DONE' AND c.id != :keepId")
    void deleteOldCompletedChecks(@Param("documentId") UUID documentId, @Param("keepId") UUID keepId);

    /**
     * Delete all completed checks for a document
     */
    @Modifying
    @Query("DELETE FROM CitationCheck c WHERE c.documentId = :documentId AND c.status = 'DONE'")
    void deleteCompletedByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Find citation check with issues loaded
     */
    @Query("SELECT c FROM CitationCheck c LEFT JOIN FETCH c.issues WHERE c.id = :id")
    Optional<CitationCheck> findByIdWithIssues(@Param("id") UUID id);

    /**
     * Find latest citation check for document with issues loaded
     */
    @Query(
            "SELECT c FROM CitationCheck c LEFT JOIN FETCH c.issues WHERE c.documentId = :documentId AND c.status = 'DONE' ORDER BY c.createdAt DESC")
    Optional<CitationCheck> findLatestByDocumentIdWithIssues(@Param("documentId") UUID documentId);

    /**
     * Find completed citation check by document and content hash for reuse
     */
    @Query(
            "SELECT c FROM CitationCheck c LEFT JOIN FETCH c.issues WHERE c.documentId = :documentId AND c.contentHash = :contentHash AND c.status = 'DONE' ORDER BY c.createdAt DESC")
    Optional<CitationCheck> findByDocumentIdAndContentHashWithIssues(
            @Param("documentId") UUID documentId, @Param("contentHash") String contentHash);

    /**
     * Find latest completed citation check by document and content hash (for cache reuse)
     */
    @Query(
            "SELECT c FROM CitationCheck c WHERE c.documentId = :documentId AND c.contentHash = :contentHash AND c.status = 'DONE' ORDER BY c.createdAt DESC")
    Optional<CitationCheck> findLatestCompletedByDocumentIdAndContentHash(
            @Param("documentId") UUID documentId, @Param("contentHash") String contentHash);

    /**
     * Check if a completed check exists for this document and content hash
     */
    @Query(
            "SELECT COUNT(c) > 0 FROM CitationCheck c WHERE c.documentId = :documentId AND c.contentHash = :contentHash AND c.status = 'DONE'")
    boolean existsByDocumentIdAndContentHashAndCompleted(
            @Param("documentId") UUID documentId, @Param("contentHash") String contentHash);
}
