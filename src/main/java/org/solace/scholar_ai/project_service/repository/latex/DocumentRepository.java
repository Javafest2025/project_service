package org.solace.scholar_ai.project_service.repository.latex;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.latex.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    List<Document> findByProjectIdAndTitleContainingIgnoreCaseOrderByUpdatedAtDesc(UUID projectId, String title);

    Optional<Document> findByProjectIdAndTitle(UUID projectId, String title);

    @Modifying
    @Query("UPDATE Document d SET d.lastAccessed = :accessTime WHERE d.id = :documentId")
    void updateLastAccessed(@Param("documentId") UUID documentId, @Param("accessTime") Instant accessTime);

    @Query("SELECT d FROM Document d WHERE d.projectId = :projectId AND d.isAutoSaved = true ORDER BY d.updatedAt DESC")
    List<Document> findAutoSavedDocumentsByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.projectId = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find document IDs by project ID
     */
    @Query("SELECT d.id FROM Document d WHERE d.projectId = :projectId")
    List<UUID> findIdsByProjectId(@Param("projectId") UUID projectId);

    /**
     * Delete documents by project ID
     */
    void deleteByProjectId(UUID projectId);
}
