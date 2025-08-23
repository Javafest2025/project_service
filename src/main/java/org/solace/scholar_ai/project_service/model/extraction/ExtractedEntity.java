package org.solace.scholar_ai.project_service.model.extraction;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing extracted named entities (organizations, locations, etc.)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "extracted_entities")
public class ExtractedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "paper_extraction_id", nullable = false)
    private PaperExtraction paperExtraction;

    @Column(name = "entity_id", length = 100)
    private String entityId; // ID from extractor

    @Column(name = "entity_type", length = 50)
    private String entityType; // PERSON, ORGANIZATION, LOCATION, etc.

    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "uri", length = 1000)
    private String uri; // linked data URI (optional)

    @Column(name = "page")
    private Integer page;

    @Column(name = "context", columnDefinition = "TEXT")
    private String context; // surrounding text

    @Column(name = "confidence")
    @Builder.Default
    private Double confidence = 1.0; // confidence score 0-1

    @Column(name = "order_index")
    private Integer orderIndex; // for maintaining entity order
}
