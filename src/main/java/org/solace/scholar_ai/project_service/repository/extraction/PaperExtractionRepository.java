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
     * Find extraction by paper ID with sections for citation analysis
     *
     * @param paperId The paper ID
     * @return Optional PaperExtraction with sections loaded
     */
    @Query("SELECT pe FROM PaperExtraction pe " + "LEFT JOIN FETCH pe.sections " + "WHERE pe.paper.id = :paperId")
    Optional<PaperExtraction> findByPaperIdWithSections(@Param("paperId") UUID paperId);

    /**
     * Find extraction by paper ID with references for citation analysis
     *
     * @param paperId The paper ID
     * @return Optional PaperExtraction with references loaded
     */
    @Query("SELECT pe FROM PaperExtraction pe " + "LEFT JOIN FETCH pe.references " + "WHERE pe.paper.id = :paperId")
    Optional<PaperExtraction> findByPaperIdWithReferences(@Param("paperId") UUID paperId);

    /**
     * Find extraction by paper ID with all related data eagerly loaded
     *
     * @param paperId The paper ID
     * @return Optional PaperExtraction with all related entities loaded
     */
    @Query("SELECT pe FROM PaperExtraction pe " + "LEFT JOIN FETCH pe.sections s "
            + "LEFT JOIN FETCH s.paragraphs "
            + "LEFT JOIN FETCH pe.figures "
            + "LEFT JOIN FETCH pe.tables "
            + "LEFT JOIN FETCH pe.equations "
            + "LEFT JOIN FETCH pe.codeBlocks "
            + "LEFT JOIN FETCH pe.references "
            + "LEFT JOIN FETCH pe.entities "
            + "WHERE pe.paper.id = :paperId")
    Optional<PaperExtraction> findByPaperIdWithAllData(@Param("paperId") UUID paperId);

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
