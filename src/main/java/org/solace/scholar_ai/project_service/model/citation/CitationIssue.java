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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "citation_issues")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"citationCheck", "evidence"})
public class CitationIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private CitationCheck citationCheck;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Convert(converter = IssueTypeConverter.class)
    @Column(name = "type", nullable = false)
    private IssueType type;

    @Convert(converter = SeverityConverter.class)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "from_pos", nullable = false)
    private Integer fromPos;

    @Column(name = "to_pos", nullable = false)
    private Integer toPos;

    @Column(name = "line_start", nullable = false)
    private Integer lineStart;

    @Column(name = "line_end", nullable = false)
    private Integer lineEnd;

    @Column(name = "snippet", nullable = false, columnDefinition = "TEXT")
    private String snippet;

    @Column(name = "cited_keys", columnDefinition = "text[]")
    private String[] citedKeys;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggestions")
    private List<Map<String, Object>> suggestions; // JSON array of suggestion objects

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false; // Whether the issue has been marked as resolved

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "citationIssue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CitationEvidence> evidence = new ArrayList<>();

    // Enums
    public enum IssueType {
        MISSING_CITATION("missing-citation"),
        WEAK_CITATION("weak-citation"),
        ORPHAN_REFERENCE("orphan-reference"),
        INCORRECT_METADATA("incorrect-metadata"),
        PLAUSIBLE_CLAIM_NO_SOURCE("plausible-claim-no-source"),
        POSSIBLE_PLAGIARISM("possible-plagiarism");

        private final String value;

        IssueType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static IssueType fromValue(String value) {
            for (IssueType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown issue type: " + value);
        }
    }

    public enum Severity {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high");

        private final String value;

        Severity(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Severity fromValue(String value) {
            for (Severity severity : values()) {
                if (severity.value.equals(value)) {
                    return severity;
                }
            }
            throw new IllegalArgumentException("Unknown severity: " + value);
        }
    }

    // Helper methods
    public void addEvidence(CitationEvidence evidenceItem) {
        if (evidence == null) {
            evidence = new ArrayList<>();
        }
        evidence.add(evidenceItem);
        evidenceItem.setCitationIssue(this);
    }

    public boolean isHighSeverity() {
        return Severity.HIGH.equals(severity);
    }

    public boolean hasCitations() {
        return citedKeys != null && citedKeys.length > 0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
