package org.solace.scholar_ai.project_service.model.gap;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.paper.Paper;

/**
 * Main gap analysis entity that holds the analysis process and results.
 * Represents a one-to-many relationship with Paper (one paper can have multiple
 * gap analyses).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gap_analyses")
public class GapAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Paper reference - Many-to-One relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    // Paper extraction reference
    @Column(name = "paper_extraction_id", nullable = false)
    private UUID paperExtractionId;

    // Request tracking
    @Column(name = "correlation_id", columnDefinition = "TEXT", unique = true, nullable = false)
    private String correlationId;

    @Column(name = "request_id", columnDefinition = "TEXT", unique = true, nullable = false)
    private String requestId;

    // Analysis metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "TEXT")
    @Builder.Default
    private GapStatus status = GapStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Analysis configuration (stored as JSON)
    @Column(name = "config", columnDefinition = "TEXT")
    private String config;

    // Summary statistics
    @Column(name = "total_gaps_identified")
    @Builder.Default
    private Integer totalGapsIdentified = 0;

    @Column(name = "valid_gaps_count")
    @Builder.Default
    private Integer validGapsCount = 0;

    @Column(name = "invalid_gaps_count")
    @Builder.Default
    private Integer invalidGapsCount = 0;

    @Column(name = "modified_gaps_count")
    @Builder.Default
    private Integer modifiedGapsCount = 0;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // Relationships
    @OneToMany(mappedBy = "gapAnalysis", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("gap-analysis-gaps")
    @Builder.Default
    private List<ResearchGap> gaps = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public enum GapStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }
}
