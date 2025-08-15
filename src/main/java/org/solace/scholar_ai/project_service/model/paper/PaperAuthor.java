package org.solace.scholar_ai.project_service.model.paper;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.solace.scholar_ai.project_service.model.author.Author;

@Getter
@Setter
@Entity
@Table(name = "paper_authors")
public class PaperAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "author_order")
    private Integer authorOrder;

    @Column(name = "is_corresponding_author")
    private Boolean isCorrespondingAuthor;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public PaperAuthor() {}

    public PaperAuthor(Paper paper, Author author) {
        this.paper = paper;
        this.author = author;
    }

    public PaperAuthor(Paper paper, Author author, Integer authorOrder) {
        this(paper, author);
        this.authorOrder = authorOrder;
    }
}
