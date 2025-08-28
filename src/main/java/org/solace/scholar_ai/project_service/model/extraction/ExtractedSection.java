package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted document sections with hierarchical structure
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_sections")
public class ExtractedSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "section_id", length = 100)
    private String sectionId; // ID from extractor

    @Column(name = "label", length = 50)
    private String label; // e.g., "1.1", "A.1"

    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "section_type", length = 50)
    private String sectionType; // introduction, methods, results, etc.

    @Column(name = "level")
    private Integer level; // heading level

    @Column(name = "page_start")
    private Integer pageStart;

    @Column(name = "page_end")
    private Integer pageEnd;

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining section order

    // Self-referencing for hierarchical structure
    @ManyToOne
    @JoinColumn(name = "parent_section_id")
    private ExtractedSection parentSection;

    @OneToMany(mappedBy = "parentSection", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedSection> subsections = new ArrayList<>();

    // Paragraphs within this section
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExtractedParagraph> paragraphs = new ArrayList<>();

    // Helper methods
    public void addSubsection(ExtractedSection subsection) {
        subsections.add(subsection);
        subsection.setParentSection(this);
    }

    public void addParagraph(ExtractedParagraph paragraph) {
        paragraphs.add(paragraph);
        paragraph.setSection(this);
    }
}
