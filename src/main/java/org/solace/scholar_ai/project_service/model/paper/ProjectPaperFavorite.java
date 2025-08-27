package org.solace.scholar_ai.project_service.model.paper;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.solace.scholar_ai.project_service.model.project.Project;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "project_paper_favorites",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"project_id", "paper_id"})})
public class ProjectPaperFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "priority")
    private String priority; // low, medium, high, critical

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags; // JSON array of tags

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Helper methods
    public ProjectPaperFavorite(Project project, Paper paper, UUID userId) {
        this.project = project;
        this.paper = paper;
        this.userId = userId;
    }
}
