package org.solace.scholar_ai.project_service.model.chat;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a content chunk for RAG (Retrieval Augmented Generation)
 * Used for organizing and prioritizing extracted paper content for AI responses
 */
@Data
@Builder
public class ContentChunk {
    private String content;
    private String source;
    private String type;
    private double relevanceScore;
    private Integer pageNumber;
}
