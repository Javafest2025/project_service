package org.solace.scholar_ai.project_service.repository.extraction;

import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for PaperExtraction entities
 */
@Repository
public interface PaperExtractionRepository extends JpaRepository<PaperExtraction, UUID> {

    /**
     * Find extraction by paper ID
     *
     * @param paperId The paper ID
     * @return Optional PaperExtraction
     */
    @Query("SELECT pe FROM PaperExtraction pe WHERE pe.paper.id = :paperId")
    Optional<PaperExtraction> findByPaperId(@Param("paperId") UUID paperId);

    /**
     * Find extraction by extraction ID from extractor service
     *
     * @param extractionId The extraction ID
     * @return Optional PaperExtraction
     */
    Optional<PaperExtraction> findByExtractionId(String extractionId);

    /**
     * Check if extraction exists for a paper
     *
     * @param paperId The paper ID
     * @return true if extraction exists
     */
    @Query("SELECT COUNT(pe) > 0 FROM PaperExtraction pe WHERE pe.paper.id = :paperId")
    boolean existsByPaperId(@Param("paperId") UUID paperId);
}
