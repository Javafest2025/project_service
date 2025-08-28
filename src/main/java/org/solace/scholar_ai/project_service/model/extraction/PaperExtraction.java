package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.solace.scholar_ai.project_service.model.paper.Paper;

/**
 * Main extraction entity that holds metadata about the extraction process
 * and links to all extracted content
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "paper_extractions")
public class PaperExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(name = "extraction_id", length = 100, unique = true)
    private String extractionId; // UUID from extractor service

    @Column(name = "pdf_hash", length = 255)
    private String pdfHash;

    @Column(name = "extraction_timestamp")
    private Instant extractionTimestamp;

    // Core metadata
    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "abstract_text", columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "page_count")
    private Integer pageCount;

    // Processing metadata
    @Column(name = "extraction_methods", columnDefinition = "TEXT")
    private String extractionMethods; // JSON array of methods used

    @Column(name = "processing_time")
    private Double processingTime; // in seconds

    @Column(name = "errors", columnDefinition = "TEXT")
    private String errors; // JSON array of errors

    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings; // JSON array of warnings

    // Quality metrics
    @Column(name = "extraction_coverage")
    private Double extractionCoverage; // 0-100%

    @Column(name = "confidence_scores", columnDefinition = "TEXT")
    private String confidenceScores; // JSON object with confidence scores

    // Relationships to extracted content
    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedFigure> figures = new ArrayList<>();

    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedTable> tables = new ArrayList<>();

    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedEquation> equations = new ArrayList<>();

    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedCodeBlock> codeBlocks = new ArrayList<>();

    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedReference> references = new ArrayList<>();

    @OneToMany(mappedBy = "paperExtraction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedEntity> entities = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Helper methods
    public void addSection(ExtractedSection section) {
        sections.add(section);
        section.setPaperExtraction(this);
    }

    public void addFigure(ExtractedFigure figure) {
        figures.add(figure);
        figure.setPaperExtraction(this);
    }

    public void addTable(ExtractedTable table) {
        tables.add(table);
        table.setPaperExtraction(this);
    }

    public void addEquation(ExtractedEquation equation) {
        equations.add(equation);
        equation.setPaperExtraction(this);
    }

    public void addCodeBlock(ExtractedCodeBlock codeBlock) {
        codeBlocks.add(codeBlock);
        codeBlock.setPaperExtraction(this);
    }

    public void addReference(ExtractedReference reference) {
        references.add(reference);
        reference.setPaperExtraction(this);
    }

    public void addEntity(ExtractedEntity entity) {
        entities.add(entity);
        entity.setPaperExtraction(this);
    }
}
