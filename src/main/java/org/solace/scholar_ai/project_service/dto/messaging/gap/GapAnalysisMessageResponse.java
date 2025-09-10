package org.solace.scholar_ai.project_service.dto.messaging.gap;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Message DTO for receiving gap analysis responses from RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisMessageResponse {

    private String requestId;
    private String correlationId;
    private String status;
    private String message;
    private UUID gapAnalysisId;
    private Integer totalGaps;
    private Integer validGaps;
    private List<GapData> gaps;
    private Instant completedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapData {
        private String gapId;
        private String name;
        private String description;
        private String category;
        private String validationStatus;
        private Double confidenceScore;
        private String potentialImpact;
        private String researchHints;
        private String implementationSuggestions;
        private String risksAndChallenges;
        private String requiredResources;
        private String estimatedDifficulty;
        private String estimatedTimeline;
        private List<EvidenceAnchor> evidenceAnchors;
        private List<ResearchTopic> suggestedTopics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceAnchor {
        private String title;
        private String url;
        private String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResearchTopic {
        private String title;
        private String description;
        private List<String> researchQuestions;
        private String methodologySuggestions;
        private String expectedOutcomes;
        private Double relevanceScore;
    }
}
