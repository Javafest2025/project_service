package org.solace.scholar_ai.project_service.dto.response.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response from paper contextual chat containing AI answer and context metadata")
public class PaperChatResponse {

    @Schema(description = "Chat session ID")
    private UUID sessionId;

    @Schema(description = "AI generated response based on paper content")
    private String response;

    @Schema(description = "Context metadata showing what content was used")
    private ContextMetadata context;

    @Schema(description = "Response timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    @Schema(description = "Whether the response was generated successfully")
    private boolean success;

    @Schema(description = "Error message if response generation failed")
    private String error;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Metadata about the content used to generate the response")
    public static class ContextMetadata {

        @Schema(description = "Sections from the paper that were used")
        private List<String> sectionsUsed;

        @Schema(description = "Figures referenced in the response")
        private List<String> figuresReferenced;

        @Schema(description = "Tables referenced in the response")
        private List<String> tablesReferenced;

        @Schema(description = "Equations referenced in the response")
        private List<String> equationsUsed;

        @Schema(description = "Page numbers referenced")
        private List<Integer> pagesReferenced;

        @Schema(description = "Confidence score of the response (0-1)")
        private Double confidenceScore;

        @Schema(description = "Number of content chunks used")
        private Integer chunksUsed;

        @Schema(description = "Whether specific references were found in the question")
        private Boolean hadSpecificReferences;

        @Schema(description = "Source descriptions for the content used")
        private List<String> contentSources;
    }
}
