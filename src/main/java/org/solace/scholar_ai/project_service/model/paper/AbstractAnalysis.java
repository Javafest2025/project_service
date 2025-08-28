package org.solace.scholar_ai.project_service.model.paper;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "abstract_analyses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbstractAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "paper_id", nullable = false)
    private String paperId;

    @Column(name = "abstract_text_hash", nullable = false)
    private String abstractTextHash;

    @Column(name = "focus", length = 1000)
    private String focus;

    @Column(name = "approach", length = 1000)
    private String approach;

    @Column(name = "emphasis", length = 1000)
    private String emphasis;

    @Column(name = "methodology", length = 1000)
    private String methodology;

    @Column(name = "impact", length = 1000)
    private String impact;

    @Column(name = "challenges", length = 1000)
    private String challenges;

    @OneToMany(mappedBy = "abstractAnalysis", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AbstractHighlight> highlights;

    @Column(name = "analysis_version")
    private String analysisVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
