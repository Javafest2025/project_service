package org.solace.scholar_ai.project_service.model.summary;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.solace.scholar_ai.project_service.model.paper.Paper;

/**
 * Entity representing AI-generated paper summaries with structured fields
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "paper_summaries")
public class PaperSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "paper_id", nullable = false, unique = true)
    private Paper paper;

    // Quick Take Section
    @Column(name = "one_liner", columnDefinition = "TEXT")
    private String oneLiner;

    @Column(name = "key_contributions", columnDefinition = "TEXT")
    private String keyContributions; // JSON array

    @Column(name = "method_overview", columnDefinition = "TEXT")
    private String methodOverview;

    @Column(name = "main_findings", columnDefinition = "TEXT")
    private String mainFindings; // JSON array of findings objects

    @Column(name = "limitations", columnDefinition = "TEXT")
    private String limitations; // JSON array

    @Column(name = "applicability", columnDefinition = "TEXT")
    private String applicability; // JSON array

    // Methods & Data Section
    @Column(name = "study_type", length = 50)
    @Enumerated(EnumType.STRING)
    private StudyType studyType;

    @Column(name = "research_questions", columnDefinition = "TEXT")
    private String researchQuestions; // JSON array

    @Column(name = "datasets", columnDefinition = "TEXT")
    private String datasets; // JSON array of dataset objects

    @Column(name = "participants", columnDefinition = "TEXT")
    private String participants; // JSON object

    @Column(name = "procedure_or_pipeline", columnDefinition = "TEXT")
    private String procedureOrPipeline;

    @Column(name = "baselines_or_controls", columnDefinition = "TEXT")
    private String baselinesOrControls; // JSON array

    @Column(name = "metrics", columnDefinition = "TEXT")
    private String metrics; // JSON array with definitions

    @Column(name = "statistical_analysis", columnDefinition = "TEXT")
    private String statisticalAnalysis; // JSON array

    @Column(name = "compute_resources", columnDefinition = "TEXT")
    private String computeResources; // JSON object

    @Column(name = "implementation_details", columnDefinition = "TEXT")
    private String implementationDetails; // JSON object

    // Reproducibility Section
    @Column(name = "artifacts", columnDefinition = "TEXT")
    private String artifacts; // JSON object with URLs

    @Column(name = "reproducibility_notes", columnDefinition = "TEXT")
    private String reproducibilityNotes;

    @Column(name = "repro_score")
    private Double reproScore; // 0-1 scale

    // Ethics & Compliance Section
    @Column(name = "ethics", columnDefinition = "TEXT")
    private String ethics; // JSON object

    @Column(name = "bias_and_fairness", columnDefinition = "TEXT")
    private String biasAndFairness; // JSON array

    @Column(name = "risks_and_misuse", columnDefinition = "TEXT")
    private String risksAndMisuse; // JSON array

    @Column(name = "data_rights", columnDefinition = "TEXT")
    private String dataRights;

    // Context & Impact Section
    @Column(name = "novelty_type", length = 50)
    @Enumerated(EnumType.STRING)
    private NoveltyType noveltyType;

    @Column(name = "positioning", columnDefinition = "TEXT")
    private String positioning; // JSON array

    @Column(name = "related_works_key", columnDefinition = "TEXT")
    private String relatedWorksKey; // JSON array of citation objects

    @Column(name = "impact_notes", columnDefinition = "TEXT")
    private String impactNotes;

    // Quality & Trust Section
    @Column(name = "confidence")
    private Double confidence; // 0-1 scale

    @Column(name = "evidence_anchors", columnDefinition = "TEXT")
    private String evidenceAnchors; // JSON array

    @Column(name = "threats_to_validity", columnDefinition = "TEXT")
    private String threatsToValidity; // JSON array

    // Additional fields for enhanced tracking
    @Column(name = "domain_classification", columnDefinition = "TEXT")
    private String domainClassification; // JSON array of domains

    @Column(name = "technical_depth", columnDefinition = "TEXT")
    private String technicalDepth; // introductory|intermediate|advanced|expert

    @Column(name = "interdisciplinary_connections", columnDefinition = "TEXT")
    private String interdisciplinaryConnections; // JSON array

    @Column(name = "future_work", columnDefinition = "TEXT")
    private String futureWork; // JSON array

    // Generation metadata
    @Column(name = "model_version", length = 50)
    private String modelVersion; // e.g., "gemini-pro-1.5"

    @Column(name = "response_source", length = 50)
    @Enumerated(EnumType.STRING)
    private ResponseSource responseSource;

    @Column(name = "fallback_reason", columnDefinition = "TEXT")
    private String fallbackReason;

    @Column(name = "generation_timestamp")
    private Instant generationTimestamp;

    @Column(name = "generation_time_seconds")
    private Double generationTimeSeconds;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "extraction_coverage_used")
    private Double extractionCoverageUsed; // % of extracted data utilized

    @Column(name = "validation_status", length = 50)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ValidationStatus validationStatus = ValidationStatus.PENDING;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Enums
    public enum StudyType {
        EMPIRICAL,
        THEORETICAL,
        SIMULATION,
        SURVEY,
        BENCHMARK,
        TOOLING,
        META_ANALYSIS,
        CASE_STUDY,
        MIXED_METHODS,
        UNKNOWN
    }

    public enum NoveltyType {
        NEW_TASK,
        NEW_METHOD,
        NEW_DATA,
        BETTER_RESULTS,
        SYNTHESIS,
        REPLICATION,
        NEGATIVE_RESULTS,
        POSITION_PAPER,
        UNKNOWN
    }

    public enum ValidationStatus {
        PENDING,
        VALIDATED,
        NEEDS_REVIEW,
        FAILED,
        PARTIAL
    }

    public enum ResponseSource {
        GEMINI_API,
        FALLBACK,
        CACHED,
        MANUAL
    }
}
