package org.solace.scholar_ai.project_service.model.gap;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Papers analyzed during gap validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gap_validation_papers")
public class GapValidationPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Research gap reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "research_gap_id", nullable = false)
    @JsonBackReference("research-gap-validation-papers")
    private ResearchGap researchGap;

    // Paper information
    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "doi", columnDefinition = "TEXT")
    private String doi;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "publication_date")
    private Instant publicationDate;

    // Extraction status
    @Column(name = "extraction_status", columnDefinition = "TEXT")
    private String extractionStatus;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "extraction_error", columnDefinition = "TEXT")
    private String extractionError;

    // Relevance analysis
    @Column(name = "relevance_score")
    private Double relevanceScore; // 0-1

    @Column(name = "relevance_reasoning", columnDefinition = "TEXT")
    private String relevanceReasoning;

    @Column(name = "supports_gap")
    private Boolean supportsGap;

    @Column(name = "conflicts_with_gap")
    private Boolean conflictsWithGap;

    @Column(name = "key_findings", columnDefinition = "TEXT")
    private String keyFindings;

    // Timestamps
    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
