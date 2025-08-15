package org.solace.scholar_ai.project_service.model.author;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.solace.scholar_ai.project_service.model.paper.PaperAuthor;

/** Entity mapping for author table. */
@Getter
@Setter
@Entity
@DynamicUpdate
@Table(name = "authors")
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "author_id", length = 255)
    private String authorId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(length = 255)
    private String orcid;

    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(length = 255)
    private String email;

    @Column(name = "h_index")
    private Integer hIndex;

    @Column(name = "paper_count")
    private Integer paperCount;

    @Column(name = "citation_count")
    private Integer citationCount;

    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;

    @Column(name = "fields_of_study", columnDefinition = "TEXT")
    private String fieldsOfStudy;

    @Column(name = "affiliations", columnDefinition = "TEXT")
    private String affiliations;

    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    // Many-to-many relationship with papers through PaperAuthor
    @OneToMany(mappedBy = "author", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<PaperAuthor> paperAuthors = new ArrayList<>();
}
