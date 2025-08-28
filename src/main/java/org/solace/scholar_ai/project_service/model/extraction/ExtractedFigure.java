package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted figures and images
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_figures")
public class ExtractedFigure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "figure_id", length = 100)
    private String figureId; // ID from extractor

    @Column(name = "label", length = 100)
    private String label; // e.g., "Figure 1", "Fig. 2"

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "page")
    private Integer page;

    @Column(name = "figure_type", length = 50)
    private String figureType; // figure, chart, diagram, etc.

    // Bounding box
    @Column(name = "bbox_x1")
    private Double bboxX1;

    @Column(name = "bbox_y1")
    private Double bboxY1;

    @Column(name = "bbox_x2")
    private Double bboxX2;

    @Column(name = "bbox_y2")
    private Double bboxY2;

    @Column(name = "bbox_confidence")
    private Double bboxConfidence;

    // File paths
    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    // References to this figure (stored as JSON array)
    @Column(name = "figure_references", columnDefinition = "TEXT")
    private String references; // section IDs that reference this figure

    // OCR extracted text for LLM processing
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText; // text extracted from the figure image

    @Column(name = "ocr_confidence")
    private Double ocrConfidence; // OCR confidence score (0-1)

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining figure order
}
