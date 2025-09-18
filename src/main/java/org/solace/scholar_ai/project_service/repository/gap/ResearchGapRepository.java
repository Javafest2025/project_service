package org.solace.scholar_ai.project_service.repository.gap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.gap.ResearchGap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ResearchGap entities.
 */
@Repository
public interface ResearchGapRepository extends JpaRepository<ResearchGap, UUID> {

    /**
     * Find research gaps by gap analysis ID.
     */
    List<ResearchGap> findByGapAnalysisIdOrderByOrderIndexAsc(UUID gapAnalysisId);

    /**
     * Find research gaps by gap analysis ID with pagination.
     */
    Page<ResearchGap> findByGapAnalysisIdOrderByOrderIndexAsc(UUID gapAnalysisId, Pageable pageable);

    /**
     * Find research gap by gap ID.
     */
    Optional<ResearchGap> findByGapId(String gapId);

    /**
     * Find research gaps by validation status.
     */
    List<ResearchGap> findByValidationStatusOrderByCreatedAtDesc(ResearchGap.GapValidationStatus validationStatus);

    /**
     * Find research gaps by validation status with pagination.
     */
    Page<ResearchGap> findByValidationStatusOrderByCreatedAtDesc(
            ResearchGap.GapValidationStatus validationStatus, Pageable pageable);

    /**
     * Find research gaps by category.
     */
    List<ResearchGap> findByCategoryOrderByCreatedAtDesc(String category);

    /**
     * Find research gaps by category with pagination.
     */
    Page<ResearchGap> findByCategoryOrderByCreatedAtDesc(String category, Pageable pageable);

    /**
     * Find research gaps by estimated difficulty.
     */
    List<ResearchGap> findByEstimatedDifficultyOrderByCreatedAtDesc(String estimatedDifficulty);

    /**
     * Find research gaps by estimated difficulty with pagination.
     */
    Page<ResearchGap> findByEstimatedDifficultyOrderByCreatedAtDesc(String estimatedDifficulty, Pageable pageable);

    /**
     * Count research gaps by gap analysis ID.
     */
    long countByGapAnalysisId(UUID gapAnalysisId);

    /**
     * Count research gaps by validation status.
     */
    long countByValidationStatus(ResearchGap.GapValidationStatus validationStatus);

    /**
     * Count research gaps by category.
     */
    long countByCategory(String category);

    /**
     * Find research gaps with high confidence scores.
     */
    @Query(
            "SELECT rg FROM ResearchGap rg WHERE rg.validationConfidence >= :minConfidence ORDER BY rg.validationConfidence DESC")
    List<ResearchGap> findHighConfidenceGaps(@Param("minConfidence") Double minConfidence);

    /**
     * Find research gaps by paper ID (through gap analysis).
     */
    @Query("SELECT rg FROM ResearchGap rg WHERE rg.gapAnalysis.paper.id = :paperId ORDER BY rg.createdAt DESC")
    List<ResearchGap> findByPaperId(@Param("paperId") UUID paperId);

    /**
     * Find research gaps by paper ID with pagination.
     */
    @Query("SELECT rg FROM ResearchGap rg WHERE rg.gapAnalysis.paper.id = :paperId ORDER BY rg.createdAt DESC")
    Page<ResearchGap> findByPaperId(@Param("paperId") UUID paperId, Pageable pageable);

    /**
     * Find valid research gaps by paper ID.
     */
    @Query(
            "SELECT rg FROM ResearchGap rg WHERE rg.gapAnalysis.paper.id = :paperId AND rg.validationStatus = 'VALID' ORDER BY rg.validationConfidence DESC")
    List<ResearchGap> findValidGapsByPaperId(@Param("paperId") UUID paperId);

    /**
     * Find research gaps by multiple categories.
     */
    @Query("SELECT rg FROM ResearchGap rg WHERE rg.category IN :categories ORDER BY rg.createdAt DESC")
    List<ResearchGap> findByCategories(@Param("categories") List<String> categories);

    /**
     * Delete research gaps by gap analysis IDs
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ResearchGap rg WHERE rg.gapAnalysis.id IN :gapAnalysisIds")
    void deleteByGapAnalysisIdIn(@Param("gapAnalysisIds") List<UUID> gapAnalysisIds);
}
