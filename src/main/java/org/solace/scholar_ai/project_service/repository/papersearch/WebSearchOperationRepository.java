package org.solace.scholar_ai.project_service.repository.papersearch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WebSearchOperationRepository extends JpaRepository<WebSearchOperation, String> {

    /**
     * Find all search operations for a specific project
     */
    List<WebSearchOperation> findByProjectIdOrderBySubmittedAtDesc(UUID projectId);

    /**
     * Find search operations by status
     */
    List<WebSearchOperation> findByStatusOrderBySubmittedAtDesc(WebSearchOperation.SearchStatus status);

    /**
     * Find search operations by project and status
     */
    List<WebSearchOperation> findByProjectIdAndStatusOrderBySubmittedAtDesc(
            UUID projectId, WebSearchOperation.SearchStatus status);

    /**
     * Find completed search operations for a project
     */
    @Query(
            "SELECT w FROM WebSearchOperation w WHERE w.projectId = :projectId AND w.status = org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation.SearchStatus.COMPLETED ORDER BY w.completedAt DESC")
    List<WebSearchOperation> findCompletedByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find search operations submitted within a time range
     */
    List<WebSearchOperation> findBySubmittedAtBetweenOrderBySubmittedAtDesc(
            LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find search operations that are still in progress (SUBMITTED or IN_PROGRESS)
     */
    @Query(
            "SELECT w FROM WebSearchOperation w WHERE w.status IN (org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation.SearchStatus.SUBMITTED, org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation.SearchStatus.IN_PROGRESS) ORDER BY w.submittedAt ASC")
    List<WebSearchOperation> findInProgressOperations();

    /**
     * Count operations by status for a project
     */
    @Query("SELECT COUNT(w) FROM WebSearchOperation w WHERE w.projectId = :projectId AND w.status = :status")
    long countByProjectIdAndStatus(
            @Param("projectId") UUID projectId, @Param("status") WebSearchOperation.SearchStatus status);

    /**
     * Find operations that have been running for more than specified minutes
     * (potential stuck operations)
     */
    @Query(
            "SELECT w FROM WebSearchOperation w WHERE w.status IN (org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation.SearchStatus.SUBMITTED, org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation.SearchStatus.IN_PROGRESS) AND w.submittedAt < :beforeTime")
    List<WebSearchOperation> findStuckOperations(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Get total papers found for a project across all completed searches
     */
    @Query(
            "SELECT COALESCE(SUM(w.totalPapersFound), 0) FROM WebSearchOperation w WHERE w.projectId = :projectId AND w.status = org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation.SearchStatus.COMPLETED")
    long getTotalPapersFoundByProject(@Param("projectId") UUID projectId);

    /**
     * Find recent operations (within last N days)
     */
    @Query("SELECT w FROM WebSearchOperation w WHERE w.submittedAt >= :since ORDER BY w.submittedAt DESC")
    List<WebSearchOperation> findRecentOperations(@Param("since") LocalDateTime since);

    /**
     * Check if a correlation ID exists
     */
    boolean existsByCorrelationId(String correlationId);

    /**
     * Find by correlation ID (alternative to findById for clarity)
     */
    Optional<WebSearchOperation> findByCorrelationId(String correlationId);
}
