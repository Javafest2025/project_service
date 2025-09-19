package org.solace.scholar_ai.project_service.model.latex;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "latex_ai_chat_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"document", "messages", "checkpoints"})
public class LatexAiChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "session_title", nullable = false)
    @Builder.Default
    private String sessionTitle = "LaTeX AI Chat";

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // Relationships
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<LatexAiChatMessage> messages = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<LatexDocumentCheckpoint> checkpoints = new ArrayList<>();

    // Helper methods
    public void addMessage(LatexAiChatMessage message) {
        messages.add(message);
        message.setSession(this);
        this.updatedAt = LocalDateTime.now();
    }

    public void addCheckpoint(LatexDocumentCheckpoint checkpoint) {
        checkpoints.add(checkpoint);
        checkpoint.setSession(this);
    }

    public LatexDocumentCheckpoint getCurrentCheckpoint() {
        return checkpoints.stream()
                .filter(LatexDocumentCheckpoint::getIsCurrent)
                .findFirst()
                .orElse(null);
    }

    public long getMessageCount() {
        return messages.size();
    }

    public LocalDateTime getLastMessageTime() {
        return messages.stream()
                .map(LatexAiChatMessage::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(createdAt);
    }
}
