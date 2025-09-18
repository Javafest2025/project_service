package org.solace.scholar_ai.project_service.repository.gap;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for GapAnalysis entities.
 */
@Repository
public interface GapAnalysisRepository extends JpaRepository<GapAnalysis, UUID> {

    /**
     * Find gap analyses by paper ID.
     */
    List<GapAnalysis> findByPaperIdOrderByCreatedAtDesc(UUID paperId);

    /**
     * Find gap analyses by paper ID with pagination.
     */
    Page<GapAnalysis> findByPaperIdOrderByCreatedAtDesc(UUID paperId, Pageable pageable);

    /**
     * Find gap analysis by correlation ID.
     */
    Optional<GapAnalysis> findByCorrelationId(String correlationId);

    /**
     * Find gap analysis by request ID.
     */
    Optional<GapAnalysis> findByRequestId(String requestId);

    /**
     * Find gap analyses by status.
     */
    List<GapAnalysis> findByStatusOrderByCreatedAtDesc(GapAnalysis.GapStatus status);

    /**
     * Find gap analyses by status with pagination.
     */
    Page<GapAnalysis> findByStatusOrderByCreatedAtDesc(GapAnalysis.GapStatus status, Pageable pageable);

    /**
     * Count gap analyses by paper ID.
     */
    long countByPaperId(UUID paperId);

    /**
     * Count gap analyses by status.
     */
    long countByStatus(GapAnalysis.GapStatus status);

    /**
     * Find the latest gap analysis for a paper.
     */
    @Query("SELECT ga FROM GapAnalysis ga WHERE ga.paper.id = :paperId ORDER BY ga.createdAt DESC")
    Optional<GapAnalysis> findLatestByPaperId(@Param("paperId") UUID paperId);

    /**
     * Find gap analyses that are currently processing.
     */
    @Query("SELECT ga FROM GapAnalysis ga WHERE ga.status = 'PROCESSING' AND ga.startedAt IS NOT NULL")
    List<GapAnalysis> findProcessingAnalyses();

    /**
     * Find gap analyses that have been running for too long (stuck).
     */
    @Query("SELECT ga FROM GapAnalysis ga WHERE ga.status = 'PROCESSING' AND ga.startedAt < :cutoffTime")
    List<GapAnalysis> findStuckAnalyses(@Param("cutoffTime") java.time.Instant cutoffTime);

    /**
     * Find gap analysis IDs by paper IDs
     */
    @Query("SELECT ga.id FROM GapAnalysis ga WHERE ga.paper.id IN :paperIds")
    List<UUID> findIdsByPaperIdIn(@Param("paperIds") List<UUID> paperIds);

    /**
     * Count gap analyses by paper IDs
     */
    @Query("SELECT COUNT(ga) FROM GapAnalysis ga WHERE ga.paper.id IN :paperIds")
    long countByPaperIdIn(@Param("paperIds") List<UUID> paperIds);

    /**
     * Delete gap analyses by paper IDs
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM GapAnalysis ga WHERE ga.paper.id IN :paperIds")
    void deleteByPaperIdIn(@Param("paperIds") List<UUID> paperIds);
}
