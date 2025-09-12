package org.solace.scholar_ai.project_service.model.paper;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.solace.scholar_ai.project_service.model.author.Author;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "papers")
public class Paper {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Correlation ID reference - each paper belongs to a search operation
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    // Core Fields
    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String abstractText;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(length = 100)
    private String doi;

    // Identifiers and Source Information
    @Column(name = "semantic_scholar_id", length = 100)
    private String semanticScholarId;

    @Column(length = 50)
    private String source;

    // PDF and Access Information
    @Column(name = "pdf_content_url", length = 500)
    private String pdfContentUrl;

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "is_open_access")
    private Boolean isOpenAccess;

    @Column(name = "paper_url", length = 500)
    private String paperUrl;

    // Publication Types (stored as comma-separated values for simplicity)
    @Column(name = "publication_types", length = 200)
    private String publicationTypes;

    // Fields of Study (stored as comma-separated values for simplicity)
    @Column(name = "fields_of_study", columnDefinition = "TEXT")
    private String fieldsOfStudy;

    // Relationships
    @OneToMany(mappedBy = "paper", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JsonManagedReference("paper-authors")
    @Builder.Default
    private List<PaperAuthor> paperAuthors = new ArrayList<>();

    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExternalId> externalIds = new ArrayList<>();

    @OneToOne(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PublicationVenue venue;

    @OneToOne(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaperMetrics metrics;

    // Extraction-related fields
    @Column(name = "is_extracted")
    @Builder.Default
    private Boolean isExtracted = false;

    @Column(name = "extraction_status", length = 50)
    private String extractionStatus; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "extraction_job_id", length = 100)
    private String extractionJobId;

    @Column(name = "extraction_started_at")
    private java.time.Instant extractionStartedAt;

    @Column(name = "extraction_completed_at")
    private java.time.Instant extractionCompletedAt;

    @Column(name = "extraction_error", columnDefinition = "TEXT")
    private String extractionError;

    @Column(name = "extraction_coverage")
    private Double extractionCoverage; // 0-100%

    // Summarization-related fields
    @Column(name = "is_summarized")
    @Builder.Default
    private Boolean isSummarized = false;

    @Column(name = "summarization_status", length = 50)
    private String summarizationStatus; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "summarization_started_at")
    private java.time.Instant summarizationStartedAt;

    @Column(name = "summarization_completed_at")
    private java.time.Instant summarizationCompletedAt;

    @Column(name = "summarization_error", columnDefinition = "TEXT")
    private String summarizationError;

    // One-to-one relationship with paper extraction details
    @OneToOne(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaperExtraction paperExtraction;

    // One-to-many relationship with gap analyses
    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<GapAnalysis> gapAnalyses = new ArrayList<>();

    // LaTeX Context field - indicates if paper is added to LaTeX context for a
    // project
    @Column(name = "is_latex_context")
    @Builder.Default
    private Boolean isLatexContext = false;

    public void addExternalId(ExternalId externalId) {
        externalIds.add(externalId);
        externalId.setPaper(this);
    }

    public void removeExternalId(ExternalId externalId) {
        externalIds.remove(externalId);
        externalId.setPaper(null);
    }

    // Helper methods for managing authors
    public void addAuthor(Author author) {
        addAuthor(author, null);
    }

    public void addAuthor(Author author, Integer authorOrder) {
        PaperAuthor paperAuthor = new PaperAuthor(this, author, authorOrder);
        paperAuthors.add(paperAuthor);
    }

    public void removeAuthor(Author author) {
        paperAuthors.removeIf(pa -> pa.getAuthor().equals(author));
    }

    public List<Author> getAuthors() {
        return paperAuthors.stream().map(PaperAuthor::getAuthor).toList();
    }
}
