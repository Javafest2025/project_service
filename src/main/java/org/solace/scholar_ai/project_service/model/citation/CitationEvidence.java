package org.solace.scholar_ai.project_service.model.citation;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "citation_evidence")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "citationIssue")
public class CitationEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private CitationIssue citationIssue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source", nullable = false)
    private Map<String, Object>
            source; // JSON: { kind: 'local'|'web', paperId?, url?, paperTitle?, sectionId?, paragraphId?, page?,
    // domain? }

    @Column(name = "matched_text", nullable = false, columnDefinition = "TEXT")
    private String matchedText;

    @Column(name = "similarity")
    private Double similarity; // 0-1 range

    @Column(name = "support_score")
    private Double supportScore; // 0-1 range, NLI-style confidence

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra")
    private Map<String, Object> extra; // Additional metadata

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Helper methods
    public boolean isHighSimilarity() {
        return similarity != null && similarity >= 0.7;
    }

    public boolean isStrongSupport() {
        return supportScore != null && supportScore >= 0.66;
    }

    public boolean isLocalSource() {
        return source != null && "local".equals(source.get("kind"));
    }

    public boolean isWebSource() {
        return source != null && "web".equals(source.get("kind"));
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
