package org.solace.scholar_ai.project_service.model.note;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.solace.scholar_ai.project_service.model.paper.Paper;

/**
 * Entity mapping for paper_mentions table.
 * Represents paper references in notes using @ mentions.
 */
@Getter
@Setter
@Entity
@Table(name = "paper_mentions")
public class PaperMention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "project_id", nullable = false, columnDefinition = "uuid")
    private UUID projectId;

    @Column(name = "note_id", nullable = false, columnDefinition = "uuid")
    private UUID noteId;

    @Column(name = "paper_id", nullable = false, columnDefinition = "uuid")
    private UUID paperId;

    @Column(name = "mention_text", nullable = false)
    private String mentionText; // The text used in the @ mention (e.g., "Deep Learning for NLP")

    @Column(name = "start_position", nullable = false)
    private Integer startPosition; // Position in note content where mention starts

    @Column(name = "end_position", nullable = false)
    private Integer endPosition; // Position in note content where mention ends

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    // Relationship to Paper entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", insertable = false, updatable = false)
    private Paper paper;

    // Relationship to ProjectNote entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", insertable = false, updatable = false)
    private ProjectNote note;
}
