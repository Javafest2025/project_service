package org.solace.scholar_ai.project_service.repository.project;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.solace.scholar_ai.project_service.model.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByUserId(UUID userId);

    List<Project> findByUserIdAndStatus(UUID userId, Project.Status status);

    List<Project> findByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<Project> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT p FROM Project p WHERE p.userId = :userId AND p.isStarred = true ORDER BY p.updatedAt DESC")
    List<Project> findStarredProjectsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.userId = :userId AND p.status = :status")
    long countByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") Project.Status status);
}
