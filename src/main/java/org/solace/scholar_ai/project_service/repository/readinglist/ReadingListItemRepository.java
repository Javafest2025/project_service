package org.solace.scholar_ai.project_service.repository.readinglist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.readinglist.ReadingListItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReadingListItemRepository extends JpaRepository<ReadingListItem, UUID> {

    // Basic queries
    List<ReadingListItem> findByProjectIdOrderByAddedAtDesc(UUID projectId);

    Optional<ReadingListItem> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndPaperId(UUID projectId, UUID paperId);

    // Filtered queries
    List<ReadingListItem> findByProjectIdAndStatusOrderByAddedAtDesc(UUID projectId, ReadingListItem.Status status);

    List<ReadingListItem> findByProjectIdAndPriorityOrderByAddedAtDesc(
            UUID projectId, ReadingListItem.Priority priority);

    List<ReadingListItem> findByProjectIdAndDifficultyOrderByAddedAtDesc(
            UUID projectId, ReadingListItem.Difficulty difficulty);

    List<ReadingListItem> findByProjectIdAndRelevanceOrderByAddedAtDesc(
            UUID projectId, ReadingListItem.Relevance relevance);

    List<ReadingListItem> findByProjectIdAndIsBookmarkedOrderByAddedAtDesc(UUID projectId, Boolean isBookmarked);

    List<ReadingListItem> findByProjectIdAndIsRecommendedOrderByAddedAtDesc(UUID projectId, Boolean isRecommended);

    // Tag-based search
    @Query(
            value =
                    "SELECT * FROM reading_list WHERE project_id = :projectId AND :tag = ANY(tags) ORDER BY added_at DESC",
            nativeQuery = true)
    List<ReadingListItem> findByProjectIdAndTag(@Param("projectId") UUID projectId, @Param("tag") String tag);

    // Full-text search
    @Query(
            value =
                    "SELECT * FROM reading_list WHERE project_id = :projectId AND (LOWER(notes) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY added_at DESC",
            nativeQuery = true)
    List<ReadingListItem> findByProjectIdAndSearchTerm(
            @Param("projectId") UUID projectId, @Param("searchTerm") String searchTerm);

    // Complex filtered queries - simplified to avoid SQL syntax issues
    @Query(
            value = "SELECT * FROM reading_list WHERE project_id = :projectId "
                    + "AND (:status IS NULL OR status = :status) "
                    + "AND (:priority IS NULL OR priority = :priority) "
                    + "AND (:difficulty IS NULL OR difficulty = :difficulty) "
                    + "AND (:relevance IS NULL OR relevance = :relevance) "
                    + "AND (:isBookmarked IS NULL OR is_bookmarked = :isBookmarked) "
                    + "AND (:isRecommended IS NULL OR is_recommended = :isRecommended) "
                    + "ORDER BY added_at DESC",
            nativeQuery = true)
    Page<ReadingListItem> findByProjectIdWithFilters(
            @Param("projectId") UUID projectId,
            @Param("status") String status,
            @Param("priority") String priority,
            @Param("difficulty") String difficulty,
            @Param("relevance") String relevance,
            @Param("isBookmarked") Boolean isBookmarked,
            @Param("isRecommended") Boolean isRecommended,
            @Param("sortBy") String sortBy,
            @Param("sortOrder") String sortOrder,
            Pageable pageable);

    // Statistics queries
    @Query("SELECT COUNT(r) FROM ReadingListItem r WHERE r.projectId = :projectId")
    long countByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT COUNT(r) FROM ReadingListItem r WHERE r.projectId = :projectId AND r.status = :status")
    long countByProjectIdAndStatus(@Param("projectId") UUID projectId, @Param("status") ReadingListItem.Status status);

    @Query("SELECT AVG(r.rating) FROM ReadingListItem r WHERE r.projectId = :projectId AND r.rating IS NOT NULL")
    Double getAverageRatingByProjectId(@Param("projectId") UUID projectId);

    @Query(
            "SELECT SUM(r.estimatedTime) FROM ReadingListItem r WHERE r.projectId = :projectId AND r.estimatedTime IS NOT NULL")
    Long getTotalEstimatedTimeByProjectId(@Param("projectId") UUID projectId);

    @Query(
            "SELECT SUM(r.actualTime) FROM ReadingListItem r WHERE r.projectId = :projectId AND r.actualTime IS NOT NULL")
    Long getTotalActualTimeByProjectId(@Param("projectId") UUID projectId);

    // Recent activity
    @Query("SELECT r FROM ReadingListItem r WHERE r.projectId = :projectId ORDER BY r.lastReadAt DESC NULLS LAST")
    List<ReadingListItem> findRecentActivityByProjectId(@Param("projectId") UUID projectId, Pageable pageable);

    // Recommendations (placeholder for AI-based recommendations)
    @Query(
            "SELECT r FROM ReadingListItem r WHERE r.projectId = :projectId AND r.isRecommended = true ORDER BY r.addedAt DESC")
    List<ReadingListItem> findRecommendedItemsByProjectId(@Param("projectId") UUID projectId);

    // Delete operations
    void deleteByProjectId(UUID projectId);
}
