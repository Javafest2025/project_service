package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing text paragraphs within sections
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_paragraphs")
public class ExtractedParagraph {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "section_id", nullable = false)
    private ExtractedSection section;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "page")
    private Integer page;

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining paragraph order within section

    // Bounding box information (optional)
    @Column(name = "bbox_x1")
    private Double bboxX1;

    @Column(name = "bbox_y1")
    private Double bboxY1;

    @Column(name = "bbox_x2")
    private Double bboxX2;

    @Column(name = "bbox_y2")
    private Double bboxY2;

    // Style information (stored as JSON)
    @Column(name = "style", columnDefinition = "TEXT")
    private String style; // font, size, etc. as JSON
}
