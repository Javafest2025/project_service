package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted tables
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_tables")
public class ExtractedTable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "table_id", length = 100)
    private String tableId; // ID from extractor

    @Column(name = "label", length = 100)
    private String label; // e.g., "Table 1", "Tab. 2"

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "page")
    private Integer page;

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

    // Table structure (stored as JSON)
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers; // JSON array of header rows

    @Column(name = "rows", columnDefinition = "TEXT")
    private String rows; // JSON array of data rows

    @Column(name = "structure", columnDefinition = "TEXT")
    private String structure; // detailed structure information as JSON

    // Export formats
    @Column(name = "csv_path", length = 500)
    private String csvPath;

    @Column(name = "html", columnDefinition = "TEXT")
    private String html; // HTML representation

    // References to this table (stored as JSON array)
    @Column(name = "table_references", columnDefinition = "TEXT")
    private String references; // section IDs that reference this table

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining table order
}
