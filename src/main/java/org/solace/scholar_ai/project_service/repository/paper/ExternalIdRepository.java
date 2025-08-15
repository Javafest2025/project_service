package org.solace.scholar_ai.project_service.repository.paper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.paper.ExternalId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExternalIdRepository extends JpaRepository<ExternalId, UUID> {

    // Find all external IDs for a specific paper
    List<ExternalId> findByPaperId(UUID paperId);

    // Find external ID by source and value
    Optional<ExternalId> findBySourceAndValue(String source, String value);

    // Find external IDs by source
    List<ExternalId> findBySource(String source);

    // Find external IDs by value
    List<ExternalId> findByValue(String value);

    // Find external IDs by paper and source
    List<ExternalId> findByPaperIdAndSource(UUID paperId, String source);

    // Check if external ID exists
    boolean existsBySourceAndValue(String source, String value);

    // Find paper by external ID
    @Query("SELECT e.paper FROM ExternalId e WHERE e.source = :source AND e.value = :value")
    Optional<ExternalId> findPaperByExternalId(@Param("source") String source, @Param("value") String value);

    // Find all papers by external ID source
    @Query("SELECT e.paper FROM ExternalId e WHERE e.source = :source")
    List<ExternalId> findPapersByExternalIdSource(@Param("source") String source);

    // Delete all external IDs for a paper
    void deleteByPaperId(UUID paperId);

    // Delete external IDs by source
    void deleteBySource(String source);
}
