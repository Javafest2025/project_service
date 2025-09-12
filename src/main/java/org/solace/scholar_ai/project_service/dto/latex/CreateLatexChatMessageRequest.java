package org.solace.scholar_ai.project_service.dto.latex;

import lombok.*;
import org.solace.scholar_ai.project_service.model.latex.LatexAiChatMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLatexChatMessageRequest {
    private String content;
    private LatexAiChatMessage.MessageType messageType;
    private String latexSuggestion;
    private LatexAiChatMessage.ActionType actionType;
    private Integer selectionRangeFrom;
    private Integer selectionRangeTo;
    private Integer cursorPosition;

    // Context information for AI processing
    private String selectedText;
    private String fullDocument;
    private String userRequest;
}
