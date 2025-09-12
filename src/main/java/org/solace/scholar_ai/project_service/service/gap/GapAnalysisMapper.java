package org.solace.scholar_ai.project_service.service.gap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.response.gap.GapAnalysisResponse;
import org.solace.scholar_ai.project_service.dto.response.gap.ResearchGapResponse;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;
import org.solace.scholar_ai.project_service.model.gap.ResearchGap;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting gap analysis entities to DTOs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GapAnalysisMapper {

    private final ObjectMapper objectMapper;

    /**
     * Convert GapAnalysis entity to response DTO.
     */
    public GapAnalysisResponse toResponse(GapAnalysis gapAnalysis) {
        if (gapAnalysis == null) {
            return null;
        }

        List<ResearchGapResponse> gaps = gapAnalysis.getGaps() != null
                ? gapAnalysis.getGaps().stream().map(this::toResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return GapAnalysisResponse.builder()
                .id(gapAnalysis.getId())
                .paperId(gapAnalysis.getPaper().getId())
                .correlationId(gapAnalysis.getCorrelationId())
                .requestId(gapAnalysis.getRequestId())
                .status(gapAnalysis.getStatus())
                .startedAt(gapAnalysis.getStartedAt())
                .completedAt(gapAnalysis.getCompletedAt())
                .errorMessage(gapAnalysis.getErrorMessage())
                .totalGapsIdentified(gapAnalysis.getTotalGapsIdentified())
                .validGapsCount(gapAnalysis.getValidGapsCount())
                .invalidGapsCount(gapAnalysis.getInvalidGapsCount())
                .modifiedGapsCount(gapAnalysis.getModifiedGapsCount())
                .gaps(gaps)
                .createdAt(gapAnalysis.getCreatedAt())
                .updatedAt(gapAnalysis.getUpdatedAt())
                .build();
    }

    /**
     * Convert ResearchGap entity to response DTO.
     */
    public ResearchGapResponse toResponse(ResearchGap researchGap) {
        if (researchGap == null) {
            return null;
        }

        return ResearchGapResponse.builder()
                .id(researchGap.getId())
                .gapId(researchGap.getGapId())
                .orderIndex(researchGap.getOrderIndex())
                .name(researchGap.getName())
                .description(researchGap.getDescription())
                .category(researchGap.getCategory())
                .validationStatus(researchGap.getValidationStatus())
                .validationConfidence(researchGap.getValidationConfidence())
                .initialReasoning(researchGap.getInitialReasoning())
                .initialEvidence(researchGap.getInitialEvidence())
                .validationQuery(researchGap.getValidationQuery())
                .papersAnalyzedCount(researchGap.getPapersAnalyzedCount())
                .validationReasoning(researchGap.getValidationReasoning())
                .potentialImpact(researchGap.getPotentialImpact())
                .researchHints(researchGap.getResearchHints())
                .implementationSuggestions(researchGap.getImplementationSuggestions())
                .risksAndChallenges(researchGap.getRisksAndChallenges())
                .requiredResources(researchGap.getRequiredResources())
                .estimatedDifficulty(researchGap.getEstimatedDifficulty())
                .estimatedTimeline(researchGap.getEstimatedTimeline())
                .evidenceAnchors(parseEvidenceAnchors(researchGap.getEvidenceAnchors()))
                .supportingPapers(parsePaperReferences(researchGap.getSupportingPapers()))
                .conflictingPapers(parsePaperReferences(researchGap.getConflictingPapers()))
                .suggestedTopics(parseResearchTopics(researchGap.getSuggestedTopics()))
                .createdAt(researchGap.getCreatedAt())
                .validatedAt(researchGap.getValidatedAt())
                .build();
    }

    /**
     * Parse evidence anchors from JSON string.
     */
    private List<ResearchGapResponse.EvidenceAnchor> parseEvidenceAnchors(String evidenceAnchorsJson) {
        if (evidenceAnchorsJson == null || evidenceAnchorsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    evidenceAnchorsJson, new TypeReference<List<ResearchGapResponse.EvidenceAnchor>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse evidence anchors JSON: {}", evidenceAnchorsJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse paper references from JSON string.
     */
    private List<ResearchGapResponse.PaperReference> parsePaperReferences(String paperReferencesJson) {
        if (paperReferencesJson == null || paperReferencesJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    paperReferencesJson, new TypeReference<List<ResearchGapResponse.PaperReference>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse paper references JSON: {}", paperReferencesJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * Parse research topics from JSON string.
     */
    private List<ResearchGapResponse.ResearchTopic> parseResearchTopics(String researchTopicsJson) {
        if (researchTopicsJson == null || researchTopicsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(
                    researchTopicsJson, new TypeReference<List<ResearchGapResponse.ResearchTopic>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse research topics JSON: {}", researchTopicsJson, e);
            return Collections.emptyList();
        }
    }
}
