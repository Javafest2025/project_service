package org.solace.scholar_ai.project_service.dto.response.gap;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.model.gap.ResearchGap;

/**
 * Response DTO for individual research gaps.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchGapResponse {

    private UUID id;
    private String gapId;
    private Integer orderIndex;
    private String name;
    private String description;
    private String category;
    private ResearchGap.GapValidationStatus validationStatus;
    private Double validationConfidence;
    private String initialReasoning;
    private String initialEvidence;
    private String validationQuery;
    private Integer papersAnalyzedCount;
    private String validationReasoning;
    private String potentialImpact;
    private String researchHints;
    private String implementationSuggestions;
    private String risksAndChallenges;
    private String requiredResources;
    private String estimatedDifficulty;
    private String estimatedTimeline;

    // Evidence and references
    private List<EvidenceAnchor> evidenceAnchors;
    private List<PaperReference> supportingPapers;
    private List<PaperReference> conflictingPapers;

    // Suggested research topics
    private List<ResearchTopic> suggestedTopics;

    // Timestamps
    private Instant createdAt;
    private Instant validatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvidenceAnchor {
        private String title;
        private String url;
        private String type; // supporting, conflicting, neutral
        private Double relevanceScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaperReference {
        private String title;
        private String doi;
        private String url;
        private Instant publicationDate;
        private Double relevanceScore;
        private String keyFindings;
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
