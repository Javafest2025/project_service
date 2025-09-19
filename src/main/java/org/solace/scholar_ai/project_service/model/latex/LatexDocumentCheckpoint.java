package org.solace.scholar_ai.project_service.model.latex;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "latex_document_checkpoints")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"document", "session", "message"})
public class LatexDocumentCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LatexAiChatSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private LatexAiChatMessage message;

    @Column(name = "checkpoint_name", nullable = false)
    private String checkpointName;

    @Column(name = "content_before", nullable = false, columnDefinition = "TEXT")
    private String contentBefore;

    @Column(name = "content_after", columnDefinition = "TEXT")
    private String contentAfter;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    // Helper methods
    public boolean hasContentAfter() {
        return contentAfter != null && !contentAfter.trim().isEmpty();
    }

    public String getDisplayName() {
        return String.format("%s (%s)", checkpointName, createdAt.toString().substring(0, 16));
    }

    public long getContentSizeDifference() {
        if (contentAfter == null) return 0;
        return (long) contentAfter.length() - contentBefore.length();
    }
}
