package org.solace.scholar_ai.project_service.repository.latex;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.latex.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(UUID documentId, Integer versionNumber);

    @Query("SELECT MAX(dv.versionNumber) FROM DocumentVersion dv WHERE dv.documentId = :documentId")
    Optional<Integer> findMaxVersionNumberByDocumentId(@Param("documentId") UUID documentId);

    @Query("SELECT COUNT(dv) FROM DocumentVersion dv WHERE dv.documentId = :documentId")
    long countByDocumentId(@Param("documentId") UUID documentId);

    List<DocumentVersion> findByDocumentIdAndIsAutoSaveFalseOrderByVersionNumberDesc(UUID documentId);

    /**
     * Delete document versions by document IDs
     */
    void deleteByDocumentIdIn(List<UUID> documentIds);
}
