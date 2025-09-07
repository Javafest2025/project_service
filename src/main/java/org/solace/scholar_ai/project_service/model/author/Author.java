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

    // Core author information
    @Column(nullable = false, length = 500, unique = true)
    private String name;

    @Column(name = "primary_affiliation", length = 500)
    private String primaryAffiliation;

    @Column(name = "all_affiliations", columnDefinition = "TEXT")
    private String allAffiliations; // JSON array

    // External identifiers
    @Column(name = "semantic_scholar_id", length = 255)
    private String semanticScholarId;

    @Column(name = "orcid_id", length = 255)
    private String orcidId;

    @Column(name = "google_scholar_id", length = 255)
    private String googleScholarId;

    @Column(name = "openalex_id", length = 255)
    private String openalexId;

    // Metrics
    @Column(name = "citation_count")
    private Integer citationCount;

    @Column(name = "h_index")
    private Integer hIndex;

    @Column(name = "i10_index")
    private Integer i10Index;

    @Column(name = "paper_count")
    private Integer paperCount;

    // Publication timeline
    @Column(name = "first_publication_year")
    private Integer firstPublicationYear;

    @Column(name = "last_publication_year")
    private Integer lastPublicationYear;

    // Research information
    @Column(name = "research_areas", columnDefinition = "TEXT")
    private String researchAreas; // JSON array

    @Column(name = "recent_publications", columnDefinition = "TEXT")
    private String recentPublications; // JSON array

    // Data source tracking
    @Column(name = "data_sources", columnDefinition = "TEXT")
    private String dataSources; // JSON array

    @Column(name = "data_quality_score")
    private Double dataQualityScore;

    @Column(name = "search_strategy", length = 50)
    private String searchStrategy;

    @Column(name = "sources_attempted", columnDefinition = "TEXT")
    private String sourcesAttempted; // JSON array

    @Column(name = "sources_successful", columnDefinition = "TEXT")
    private String sourcesSuccessful; // JSON array

    // Sync status
    @Column(name = "is_synced")
    private Boolean isSynced = false;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "sync_error", columnDefinition = "TEXT")
    private String syncError;

    // Legacy fields for compatibility
    @Column(name = "homepage_url", length = 1000)
    private String homepageUrl;

    @Column(length = 255)
    private String email;

    @Column(name = "profile_image_url", length = 1000)
    private String profileImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Many-to-many relationship with papers through PaperAuthor
    @OneToMany(mappedBy = "author", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<PaperAuthor> paperAuthors = new ArrayList<>();

    @Override
    public String toString() {
        return name != null ? name : "Unknown Author";
    }
}
