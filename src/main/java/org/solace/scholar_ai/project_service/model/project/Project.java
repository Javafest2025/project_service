package org.solace.scholar_ai.project_service.model.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;

/** Entity mapping for project table. */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "projects")
public class Project {

    public enum Status {
        ACTIVE,
        PAUSED,
        COMPLETED,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String domain;

    @Column(name = "topics", columnDefinition = "TEXT")
    private String topics;

    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(nullable = false)
    private Integer progress = 0;

    @Column(name = "total_papers", nullable = false)
    private Integer totalPapers = 0;

    @Column(name = "active_tasks", nullable = false)
    private Integer activeTasks = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_activity")
    private String lastActivity;

    @Column(name = "is_starred", nullable = false)
    private Boolean isStarred = false;
}
