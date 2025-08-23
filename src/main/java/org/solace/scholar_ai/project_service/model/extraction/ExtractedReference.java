package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted bibliographic references
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_references")
public class ExtractedReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "reference_id", length = 100)
    private String referenceId; // ID from extractor

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText; // original citation text

    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "authors", columnDefinition = "TEXT")
    private String authors; // JSON array of author names

    @Column(name = "year")
    private Integer year;

    @Column(name = "venue", length = 500)
    private String venue; // journal, conference, etc.

    @Column(name = "doi", length = 200)
    private String doi;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "arxiv_id", length = 50)
    private String arxivId;

    // Enrichment data from external APIs (stored as JSON)
    @Column(name = "crossref_data", columnDefinition = "TEXT")
    private String crossrefData;

    @Column(name = "openalex_data", columnDefinition = "TEXT")
    private String openalexData;

    @Column(name = "unpaywall_data", columnDefinition = "TEXT")
    private String unpaywallData;

    // Citation context
    @Column(name = "cited_by_sections", columnDefinition = "TEXT")
    private String citedBySections; // JSON array of section IDs

    @Column(name = "citation_count")
    @Builder.Default
    private Integer citationCount = 0;

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining reference order
}
