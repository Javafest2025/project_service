package org.solace.scholar_ai.project_service.model.papersearch;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "web_search_operations")
public class WebSearchOperation {

    @Id
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SearchStatus status = SearchStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "query_terms", columnDefinition = "TEXT", nullable = false)
    private String queryTerms; // JSON string of List<String>

    @Column(nullable = false, length = 100)
    private String domain;

    @Column(name = "batch_size", nullable = false)
    private Integer batchSize;

    @Column(name = "total_papers_found")
    private Integer totalPapersFound;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "search_duration_ms")
    private Long searchDurationMs;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum SearchStatus {
        SUBMITTED,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    // Helper methods
    public void markAsInProgress() {
        this.status = SearchStatus.IN_PROGRESS;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCompleted(int totalPapersFound) {
        this.status = SearchStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.totalPapersFound = totalPapersFound;

        if (this.submittedAt != null && this.completedAt != null) {
            this.searchDurationMs = java.time.Duration.between(this.submittedAt, this.completedAt)
                    .toMillis();
        }
    }

    public void markAsFailed(String errorMessage) {
        this.status = SearchStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;

        if (this.submittedAt != null && this.completedAt != null) {
            this.searchDurationMs = java.time.Duration.between(this.submittedAt, this.completedAt)
                    .toMillis();
        }
    }

    public boolean isCompleted() {
        return status == SearchStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == SearchStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == SearchStatus.IN_PROGRESS || status == SearchStatus.SUBMITTED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
