package org.solace.scholar_ai.project_service.model.citation;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "citation_checks")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "issues")
public class CitationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "tex_file_name", nullable = false)
    private String texFileName;

    @Column(name = "content_hash", length = 64)
    private String contentHash; // SHA256 hash of LaTeX content

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "step", nullable = false)
    private Step step;

    @Column(name = "progress_pct", nullable = false)
    @Builder.Default
    private Integer progressPct = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary", columnDefinition = "jsonb")
    private Map<String, Object> summary; // JSON object: { total: number, byType: Record<CitationIssueType, number> }

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "citationCheck", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CitationIssue> issues = new ArrayList<>();

    // Enums
    public enum Status {
        QUEUED,
        RUNNING,
        DONE,
        ERROR
    }

    public enum Step {
        PARSING,
        LOCAL_RETRIEVAL,
        LOCAL_VERIFICATION,
        WEB_RETRIEVAL,
        WEB_VERIFICATION,
        SAVING,
        DONE,
        ERROR
    }

    // Helper methods
    public boolean isCompleted() {
        return Status.DONE.equals(status) || Status.ERROR.equals(status);
    }

    public boolean isRunning() {
        return Status.RUNNING.equals(status) || Status.QUEUED.equals(status);
    }

    public void addIssue(CitationIssue issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
        issue.setCitationCheck(this);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
