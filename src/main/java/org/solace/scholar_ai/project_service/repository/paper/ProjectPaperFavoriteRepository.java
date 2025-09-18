package org.solace.scholar_ai.project_service.repository.paper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.paper.ProjectPaperFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectPaperFavoriteRepository extends JpaRepository<ProjectPaperFavorite, UUID> {

    // Find all favorites for a project
    List<ProjectPaperFavorite> findByProjectId(UUID projectId);

    // Find all favorites for a project with pagination
    Page<ProjectPaperFavorite> findByProjectId(UUID projectId, Pageable pageable);

    // Find all favorites for a user in a project
    List<ProjectPaperFavorite> findByProjectIdAndUserId(UUID projectId, UUID userId);

    // Find all favorites for a user in a project with pagination
    Page<ProjectPaperFavorite> findByProjectIdAndUserId(UUID projectId, UUID userId, Pageable pageable);

    // Check if a paper is favorited in a project
    boolean existsByProjectIdAndPaperId(UUID projectId, UUID paperId);

    // Check if a paper is favorited by a user in a project
    boolean existsByProjectIdAndPaperIdAndUserId(UUID projectId, UUID paperId, UUID userId);

    // Find specific favorite relationship
    Optional<ProjectPaperFavorite> findByProjectIdAndPaperId(UUID projectId, UUID paperId);

    // Find specific favorite relationship by user
    Optional<ProjectPaperFavorite> findByProjectIdAndPaperIdAndUserId(UUID projectId, UUID paperId, UUID userId);

    // Delete favorite relationship
    void deleteByProjectIdAndPaperId(UUID projectId, UUID paperId);

    // Delete favorite relationship by user
    void deleteByProjectIdAndPaperIdAndUserId(UUID projectId, UUID paperId, UUID userId);

    // Count favorites for a project
    long countByProjectId(UUID projectId);

    // Count favorites for a user in a project
    long countByProjectIdAndUserId(UUID projectId, UUID userId);

    // Find favorites by priority
    List<ProjectPaperFavorite> findByProjectIdAndPriority(UUID projectId, String priority);

    // Find favorites by priority for a user
    List<ProjectPaperFavorite> findByProjectIdAndUserIdAndPriority(UUID projectId, UUID userId, String priority);

    // Search favorites by paper title
    @Query(
            "SELECT f FROM ProjectPaperFavorite f JOIN f.paper p WHERE f.project.id = :projectId AND p.title LIKE %:title%")
    List<ProjectPaperFavorite> findByProjectIdAndPaperTitleContaining(
            @Param("projectId") UUID projectId, @Param("title") String title);

    // Search favorites by paper title for a user
    @Query(
            "SELECT f FROM ProjectPaperFavorite f JOIN f.paper p WHERE f.project.id = :projectId AND f.userId = :userId AND p.title LIKE %:title%")
    List<ProjectPaperFavorite> findByProjectIdAndUserIdAndPaperTitleContaining(
            @Param("projectId") UUID projectId, @Param("userId") UUID userId, @Param("title") String title);

    // Delete all favorites for a project
    void deleteByProjectId(UUID projectId);
}
