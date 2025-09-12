package org.solace.scholar_ai.project_service.model.gap;

import com.fasterxml.jackson.annotation.JsonBackReference;
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

/**
 * Individual research gap identified in the analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "research_gaps")
public class ResearchGap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Gap analysis reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gap_analysis_id", nullable = false)
    @JsonBackReference("gap-analysis-gaps")
    private GapAnalysis gapAnalysis;

    // Gap identification
    @Column(name = "gap_id", columnDefinition = "TEXT", unique = true, nullable = false)
    private String gapId;

    @Column(name = "order_index")
    private Integer orderIndex;

    // Core gap information
    @Column(name = "name", columnDefinition = "TEXT")
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", columnDefinition = "TEXT")
    private String category; // theoretical, methodological, empirical, etc.

    // Validation status
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", columnDefinition = "TEXT")
    @Builder.Default
    private GapValidationStatus validationStatus = GapValidationStatus.INITIAL;

    @Column(name = "validation_confidence")
    private Double validationConfidence; // 0-1 confidence score

    // Initial analysis
    @Column(name = "initial_reasoning", columnDefinition = "TEXT")
    private String initialReasoning;

    @Column(name = "initial_evidence", columnDefinition = "TEXT")
    private String initialEvidence;

    // Validation details
    @Column(name = "validation_query", columnDefinition = "TEXT")
    private String validationQuery; // Search query used for validation

    @Column(name = "papers_analyzed_count")
    @Builder.Default
    private Integer papersAnalyzedCount = 0;

    @Column(name = "validation_reasoning", columnDefinition = "TEXT")
    private String validationReasoning;

    @Column(name = "modification_history", columnDefinition = "TEXT")
    private String modificationHistory; // JSON string for tracking modifications

    // Expanded information (after validation)
    @Column(name = "potential_impact", columnDefinition = "TEXT")
    private String potentialImpact;

    @Column(name = "research_hints", columnDefinition = "TEXT")
    private String researchHints;

    @Column(name = "implementation_suggestions", columnDefinition = "TEXT")
    private String implementationSuggestions;

    @Column(name = "risks_and_challenges", columnDefinition = "TEXT")
    private String risksAndChallenges;

    @Column(name = "required_resources", columnDefinition = "TEXT")
    private String requiredResources;

    @Column(name = "estimated_difficulty", columnDefinition = "TEXT")
    private String estimatedDifficulty; // low, medium, high

    @Column(name = "estimated_timeline", columnDefinition = "TEXT")
    private String estimatedTimeline; // e.g., "6-12 months"

    // Evidence and references (stored as JSON)
    @Column(name = "evidence_anchors", columnDefinition = "TEXT")
    private String evidenceAnchors; // JSON string for links to papers analyzed

    @Column(name = "supporting_papers", columnDefinition = "TEXT")
    private String supportingPapers; // JSON string for papers that support this gap

    @Column(name = "conflicting_papers", columnDefinition = "TEXT")
    private String conflictingPapers; // JSON string for papers that conflict with this gap

    // Suggested research topics (stored as JSON)
    @Column(name = "suggested_topics", columnDefinition = "TEXT")
    private String suggestedTopics; // JSON string for topic suggestions

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "validated_at")
    private Instant validatedAt;

    // Relationships
    @OneToMany(mappedBy = "researchGap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("research-gap-validation-papers")
    @Builder.Default
    private List<GapValidationPaper> validationPapers = new ArrayList<>();

    public enum GapValidationStatus {
        INITIAL,
        VALIDATING,
        VALID,
        INVALID,
        MODIFIED
    }
}
