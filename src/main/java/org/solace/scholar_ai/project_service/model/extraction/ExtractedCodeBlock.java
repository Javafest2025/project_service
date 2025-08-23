package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted code blocks and algorithms
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_code_blocks")
public class ExtractedCodeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "code_id", length = 100)
    private String codeId; // ID from extractor

    @Column(name = "language", length = 50)
    private String language; // programming language

    @Column(name = "code", columnDefinition = "TEXT")
    private String code; // the actual code content

    @Column(name = "page")
    private Integer page;

    @Column(name = "context", columnDefinition = "TEXT")
    private String context; // surrounding text for context

    @Column(name = "has_line_numbers")
    @Builder.Default
    private Boolean hasLineNumbers = false;

    // Bounding box (optional)
    @Column(name = "bbox_x1")
    private Double bboxX1;

    @Column(name = "bbox_y1")
    private Double bboxY1;

    @Column(name = "bbox_x2")
    private Double bboxX2;

    @Column(name = "bbox_y2")
    private Double bboxY2;

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining code block order
}
