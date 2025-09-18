package org.solace.scholar_ai.project_service.repository.gap;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.gap.GapValidationPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for GapValidationPaper entities.
 */
@Repository
public interface GapValidationPaperRepository extends JpaRepository<GapValidationPaper, UUID> {

    /**
     * Find validation papers by research gap ID.
     */
    List<GapValidationPaper> findByResearchGapIdOrderByRelevanceScoreDesc(UUID researchGapId);

    /**
     * Find validation papers by DOI.
     */
    List<GapValidationPaper> findByDoi(String doi);

    /**
     * Find validation papers by title.
     */
    List<GapValidationPaper> findByTitleContainingIgnoreCase(String title);

    /**
     * Find validation papers that support gaps.
     */
    List<GapValidationPaper> findBySupportsGapTrueOrderByRelevanceScoreDesc();

    /**
     * Find validation papers that conflict with gaps.
     */
    List<GapValidationPaper> findByConflictsWithGapTrueOrderByRelevanceScoreDesc();

    /**
     * Find validation papers with high relevance scores.
     */
    @Query(
            "SELECT gvp FROM GapValidationPaper gvp WHERE gvp.relevanceScore >= :minRelevance ORDER BY gvp.relevanceScore DESC")
    List<GapValidationPaper> findHighRelevancePapers(@Param("minRelevance") Double minRelevance);

    /**
     * Find validation papers by research gap ID and relevance threshold.
     */
    @Query(
            "SELECT gvp FROM GapValidationPaper gvp WHERE gvp.researchGap.id = :researchGapId AND gvp.relevanceScore >= :minRelevance ORDER BY gvp.relevanceScore DESC")
    List<GapValidationPaper> findByResearchGapIdAndMinRelevance(
            @Param("researchGapId") UUID researchGapId, @Param("minRelevance") Double minRelevance);

    /**
     * Count validation papers by research gap ID.
     */
    long countByResearchGapId(UUID researchGapId);

    /**
     * Count validation papers that support gaps.
     */
    long countBySupportsGapTrue();

    /**
     * Count validation papers that conflict with gaps.
     */
    long countByConflictsWithGapTrue();

    /**
     * Count gap validation papers by paper IDs
     */
    @Query("SELECT COUNT(gvp) FROM GapValidationPaper gvp WHERE gvp.researchGap.gapAnalysis.paper.id IN :paperIds")
    long countByPaperIdIn(@Param("paperIds") List<UUID> paperIds);

    /**
     * Delete gap validation papers by paper IDs
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GapValidationPaper gvp WHERE gvp.researchGap.gapAnalysis.paper.id IN :paperIds")
    void deleteByPaperIdIn(@Param("paperIds") List<UUID> paperIds);
}
