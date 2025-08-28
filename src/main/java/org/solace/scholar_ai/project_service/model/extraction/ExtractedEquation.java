package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted mathematical equations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_equations")
public class ExtractedEquation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "equation_id", length = 100)
    private String equationId; // ID from extractor

    @Column(name = "label", length = 100)
    private String label; // e.g., "Equation 1", "Eq. (2)"

    @Column(name = "latex", columnDefinition = "TEXT")
    private String latex; // LaTeX representation

    @Column(name = "mathml", columnDefinition = "TEXT")
    private String mathml; // MathML representation (optional)

    @Column(name = "page")
    private Integer page;

    @Column(name = "is_inline")
    @Builder.Default
    private Boolean isInline = false; // inline vs display equation

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
    private Integer orderIndex; // for maintaining equation order
}
