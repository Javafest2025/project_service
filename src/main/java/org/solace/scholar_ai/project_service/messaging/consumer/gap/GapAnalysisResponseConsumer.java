package org.solace.scholar_ai.project_service.messaging.consumer.gap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.client.UserNotificationClient;
import org.solace.scholar_ai.project_service.dto.messaging.gap.GapAnalysisMessageResponse;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;
import org.solace.scholar_ai.project_service.model.gap.ResearchGap;
import org.solace.scholar_ai.project_service.model.papersearch.WebSearchOperation;
import org.solace.scholar_ai.project_service.repository.gap.GapAnalysisRepository;
import org.solace.scholar_ai.project_service.repository.gap.ResearchGapRepository;
import org.solace.scholar_ai.project_service.repository.papersearch.WebSearchOperationRepository;
import org.solace.scholar_ai.project_service.repository.project.ProjectRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer for receiving gap analysis responses from the gap analyzer service.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GapAnalysisResponseConsumer {

    private final GapAnalysisRepository gapAnalysisRepository;
    private final ResearchGapRepository researchGapRepository;
    private final ObjectMapper objectMapper;
    private final WebSearchOperationRepository webSearchOperationRepository;
    private final ProjectRepository projectRepository;
    private final UserNotificationClient notificationClient;

    /**
     * Process gap analysis response from the gap analyzer service.
     */
    @RabbitListener(queues = "${scholarai.rabbitmq.gap-analysis.response-queue:gap_analysis_responses}")
    @Transactional
    public void processGapAnalysisResponse(GapAnalysisMessageResponse response) {
        try {
            log.info(
                    "Processing gap analysis response for requestId: {}, status: {}",
                    response.getRequestId(),
                    response.getStatus());

            // Find the gap analysis record
            GapAnalysis gapAnalysis = gapAnalysisRepository
                    .findByRequestId(response.getRequestId())
                    .orElseThrow(() ->
                            new RuntimeException("Gap analysis not found for requestId: " + response.getRequestId()));

            // Update gap analysis status
            if ("SUCCESS".equals(response.getStatus())) {
                gapAnalysis.setStatus(GapAnalysis.GapStatus.COMPLETED);
                gapAnalysis.setCompletedAt(response.getCompletedAt());
                gapAnalysis.setTotalGapsIdentified(response.getTotalGaps());
                gapAnalysis.setValidGapsCount(response.getValidGaps());
                gapAnalysis.setInvalidGapsCount(response.getTotalGaps() - response.getValidGaps());
                gapAnalysis.setErrorMessage(null);

                // Save research gaps
                if (response.getGaps() != null && !response.getGaps().isEmpty()) {
                    saveResearchGaps(gapAnalysis, response.getGaps());
                }

                log.info(
                        "Gap analysis completed successfully for requestId: {}, totalGaps: {}, validGaps: {}",
                        response.getRequestId(),
                        response.getTotalGaps(),
                        response.getValidGaps());

            } else {
                gapAnalysis.setStatus(GapAnalysis.GapStatus.FAILED);
                gapAnalysis.setCompletedAt(Instant.now());
                gapAnalysis.setErrorMessage(response.getMessage());
                log.error(
                        "Gap analysis failed for requestId: {}, error: {}",
                        response.getRequestId(),
                        response.getMessage());
            }

            gapAnalysisRepository.save(gapAnalysis);

            // Send notification on success (best-effort)
            try {
                if (gapAnalysis.getStatus() == GapAnalysis.GapStatus.COMPLETED) {
                    // Get user ID through Paper -> correlationId -> WebSearchOperation -> Project
                    // -> userId
                    String paperCorrelationId = gapAnalysis.getPaper().getCorrelationId();
                    java.util.UUID userId = null;

                    log.debug(
                            "Looking for user ID for gap analysis notification, paperCorrelationId: {}",
                            paperCorrelationId);

                    if (org.springframework.util.StringUtils.hasText(paperCorrelationId)) {
                        WebSearchOperation op = webSearchOperationRepository
                                .findByCorrelationId(paperCorrelationId)
                                .orElse(null);
                        if (op != null) {
                            var project = projectRepository
                                    .findById(op.getProjectId())
                                    .orElse(null);
                            if (project != null) {
                                userId = project.getUserId();
                                java.util.Map<String, Object> data = new java.util.HashMap<>();
                                data.put("paperTitle", gapAnalysis.getPaper().getTitle());
                                data.put("gapsCount", response.getTotalGaps());
                                data.put("validGaps", response.getValidGaps());
                                data.put("projectName", project.getName());
                                data.put(
                                        "gapNames",
                                        response.getGaps() != null
                                                ? response.getGaps().stream()
                                                        .map(
                                                                org.solace.scholar_ai.project_service.dto.messaging.gap
                                                                                .GapAnalysisMessageResponse.GapData
                                                                        ::getName)
                                                        .toList()
                                                : java.util.List.of());
                                data.put("appUrl", "https://scholarai.me");

                                log.info(
                                        "Sending gap analysis notification to user: {}, paper: {}, gaps: {}",
                                        userId,
                                        gapAnalysis.getPaper().getTitle(),
                                        response.getTotalGaps());

                                notificationClient.send(userId, "GAP_ANALYSIS_COMPLETED", data);
                            } else {
                                log.warn(
                                        "Project not found for gap analysis notification, projectId: {}",
                                        op.getProjectId());
                            }
                        } else {
                            log.warn(
                                    "WebSearchOperation not found for gap analysis notification, correlationId: {}",
                                    paperCorrelationId);
                        }
                    } else {
                        log.warn(
                                "No correlationId found in paper for gap analysis notification, paperId: {}",
                                gapAnalysis.getPaper().getId());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to send gap analysis notification for requestId: {}", response.getRequestId(), e);
            }

        } catch (Exception e) {
            log.error("Failed to process gap analysis response for requestId: {}", response.getRequestId(), e);
            // TODO: Consider implementing dead letter queue or retry mechanism
        }
    }

    /**
     * Save research gaps from the response.
     */
    private void saveResearchGaps(GapAnalysis gapAnalysis, List<GapAnalysisMessageResponse.GapData> gapDataList) {
        for (int i = 0; i < gapDataList.size(); i++) {
            GapAnalysisMessageResponse.GapData gapData = gapDataList.get(i);

            ResearchGap researchGap = ResearchGap.builder()
                    .gapAnalysis(gapAnalysis)
                    .gapId(gapData.getGapId())
                    .orderIndex(i + 1)
                    .name(gapData.getName())
                    .description(gapData.getDescription())
                    .category(gapData.getCategory())
                    .validationStatus(ResearchGap.GapValidationStatus.valueOf(gapData.getValidationStatus()))
                    .validationConfidence(gapData.getConfidenceScore())
                    .potentialImpact(gapData.getPotentialImpact())
                    .researchHints(gapData.getResearchHints())
                    .implementationSuggestions(gapData.getImplementationSuggestions())
                    .risksAndChallenges(gapData.getRisksAndChallenges())
                    .requiredResources(gapData.getRequiredResources())
                    .estimatedDifficulty(gapData.getEstimatedDifficulty())
                    .estimatedTimeline(gapData.getEstimatedTimeline())
                    .evidenceAnchors(convertToJson(gapData.getEvidenceAnchors()))
                    .suggestedTopics(convertToJson(gapData.getSuggestedTopics()))
                    .validatedAt(Instant.now())
                    .build();

            researchGapRepository.save(researchGap);
        }
    }

    /**
     * Convert object to JSON string.
     */
    private String convertToJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Failed to convert object to JSON: {}", obj, e);
            return null;
        }
    }
}
