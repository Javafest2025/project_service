package org.solace.scholar_ai.project_service.model.latex;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "latex_ai_chat_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "session")
public class LatexAiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private LatexAiChatSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "latex_suggestion", columnDefinition = "TEXT")
    private String latexSuggestion;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type")
    private ActionType actionType;

    @Column(name = "selection_range_from")
    private Integer selectionRangeFrom;

    @Column(name = "selection_range_to")
    private Integer selectionRangeTo;

    @Column(name = "cursor_position")
    private Integer cursorPosition;

    @Column(name = "is_applied")
    @Builder.Default
    private Boolean isApplied = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Enums
    public enum MessageType {
        USER,
        AI
    }

    public enum ActionType {
        ADD,
        REPLACE,
        DELETE,
        MODIFY
    }

    // Helper methods
    public boolean isUserMessage() {
        return MessageType.USER.equals(messageType);
    }

    public boolean isAiMessage() {
        return MessageType.AI.equals(messageType);
    }

    public boolean hasLatexSuggestion() {
        return latexSuggestion != null && !latexSuggestion.trim().isEmpty();
    }

    public boolean hasSelectionRange() {
        return selectionRangeFrom != null && selectionRangeTo != null;
    }
}
