package org.solace.scholar_ai.project_service.dto.latex;

import lombok.*;
import org.solace.scholar_ai.project_service.model.latex.LatexAiChatMessage;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LatexAiChatMessageDto {
    private Long id;
    private Long sessionId;
    private LatexAiChatMessage.MessageType messageType;
    private String content;
    private String latexSuggestion;
    private LatexAiChatMessage.ActionType actionType;
    private Integer selectionRangeFrom;
    private Integer selectionRangeTo;
    private Integer cursorPosition;
    private Boolean isApplied;
    private LocalDateTime createdAt;

    // Helper properties for frontend
    private String sender; // "user" or "ai"
    private String timestamp; // formatted timestamp
    private Boolean hasLatexSuggestion;
    private Boolean hasSelectionRange;

    public String getSender() {
        return messageType == LatexAiChatMessage.MessageType.USER ? "user" : "ai";
    }

    public String getTimestamp() {
        return createdAt != null ? createdAt.toString() : "";
    }

    public Boolean getHasLatexSuggestion() {
        return latexSuggestion != null && !latexSuggestion.trim().isEmpty();
    }

    public Boolean getHasSelectionRange() {
        return selectionRangeFrom != null && selectionRangeTo != null;
    }
}
