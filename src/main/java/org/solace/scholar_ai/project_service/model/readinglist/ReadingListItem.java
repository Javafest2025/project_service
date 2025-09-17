package org.solace.scholar_ai.project_service.model.readinglist;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.solace.scholar_ai.project_service.model.paper.Paper;

/**
 * Entity mapping for reading_list table.
 * Represents reading list items associated with projects.
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "reading_list")
public class ReadingListItem {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        SKIPPED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Difficulty {
        EASY,
        MEDIUM,
        HARD,
        EXPERT
    }

    public enum Relevance {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "paper_id", nullable = false, columnDefinition = "uuid")
    private UUID paperId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Relevance relevance = Relevance.MEDIUM;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Column(name = "actual_time")
    private Integer actualTime;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ElementCollection
    @CollectionTable(name = "reading_list_tags", joinColumns = @JoinColumn(name = "reading_list_id"))
    @Column(name = "tag", columnDefinition = "TEXT")
    private List<String> tags;

    @Column
    private Integer rating;

    @Column(name = "reading_progress", nullable = false)
    private Integer readingProgress = 0;

    @Column(name = "read_count", nullable = false)
    private Integer readCount = 0;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked = false;

    @Column(name = "is_recommended", nullable = false)
    private Boolean isRecommended = false;

    @Column(name = "recommended_by", length = 100)
    private String recommendedBy;

    @Column(name = "recommended_reason", columnDefinition = "TEXT")
    private String recommendedReason;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Instant addedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Relationship to Paper (not mapped to avoid circular references)
    @Transient
    private Paper paper;
}
