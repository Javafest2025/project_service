package org.solace.scholar_ai.project_service.service.gap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisRequest;
import org.solace.scholar_ai.project_service.dto.response.gap.GapAnalysisResponse;
import org.solace.scholar_ai.project_service.exception.CustomException;
import org.solace.scholar_ai.project_service.exception.ErrorCode;
import org.solace.scholar_ai.project_service.messaging.publisher.gap.GapAnalysisRequestSender;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.extraction.PaperExtractionRepository;
import org.solace.scholar_ai.project_service.repository.gap.GapAnalysisRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing gap analysis operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GapAnalysisService {

    private final GapAnalysisRepository gapAnalysisRepository;
    private final PaperRepository paperRepository;
    private final PaperExtractionRepository paperExtractionRepository;
    private final GapAnalysisRequestSender gapAnalysisRequestSender;
    private final GapAnalysisMapper gapAnalysisMapper;
    private final ObjectMapper objectMapper;

    /**
     * Initiate gap analysis for a paper.
     */
    @Transactional
    public GapAnalysisResponse initiateGapAnalysis(GapAnalysisRequest request) {
        log.info("Initiating gap analysis for paper: {}", request.getPaperId());

        // Validate paper exists and is extracted
        Paper paper = paperRepository
                .findById(request.getPaperId())
                .orElseThrow(() -> new CustomException(
                        "Paper not found: " + request.getPaperId(), HttpStatus.NOT_FOUND, ErrorCode.PAPER_NOT_FOUND));

        if (!paper.getIsExtracted()) {
            throw new CustomException(
                    "Paper has not been extracted yet. Please extract the paper first before initiating gap analysis.",
                    HttpStatus.CONFLICT,
                    ErrorCode.PAPER_NOT_EXTRACTED);
        }

        if (!"COMPLETED".equals(paper.getExtractionStatus())) {
            throw new CustomException(
                    "Paper extraction is not completed. Current status: " + paper.getExtractionStatus(),
                    HttpStatus.CONFLICT,
                    ErrorCode.PAPER_NOT_EXTRACTED);
        }

        // Get paper extraction ID from the extraction table
        UUID paperExtractionId = paperExtractionRepository
                .findByPaperId(request.getPaperId())
                .map(extraction -> extraction.getId())
                .orElseThrow(() -> new CustomException(
                        "Paper extraction not found for paper: " + request.getPaperId(),
                        HttpStatus.NOT_FOUND,
                        ErrorCode.PAPER_NOT_EXTRACTED));

        // Generate unique IDs
        String correlationId = generateCorrelationId();
        String requestId = generateRequestId();

        // Create gap analysis record
        GapAnalysis gapAnalysis = GapAnalysis.builder()
                .paper(paper)
                .paperExtractionId(paperExtractionId)
                .correlationId(correlationId)
                .requestId(requestId)
                .status(GapAnalysis.GapStatus.PENDING)
                .startedAt(Instant.now())
                .config(request.getConfig() != null ? convertConfigToJson(request.getConfig()) : null)
                .build();

        gapAnalysis = gapAnalysisRepository.save(gapAnalysis);

        // Send message to gap analyzer service
        try {
            // Create message request with generated IDs
            GapAnalysisRequest messageRequest = GapAnalysisRequest.builder()
                    .paperId(request.getPaperId())
                    .config(request.getConfig())
                    .build();

            gapAnalysisRequestSender.sendGapAnalysisRequest(
                    messageRequest, paperExtractionId, correlationId, requestId);
            log.info(
                    "Gap analysis request sent successfully for paper: {} with requestId: {}",
                    request.getPaperId(),
                    requestId);
        } catch (Exception e) {
            log.error("Failed to send gap analysis request for paper: {}", request.getPaperId(), e);
            gapAnalysis.setStatus(GapAnalysis.GapStatus.FAILED);
            gapAnalysis.setErrorMessage("Failed to send request to gap analyzer: " + e.getMessage());
            gapAnalysisRepository.save(gapAnalysis);
            throw new CustomException(
                    "Failed to initiate gap analysis: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.GAP_ANALYSIS_REQUEST_FAILED);
        }

        return gapAnalysisMapper.toResponse(gapAnalysis);
    }

    /**
     * Get gap analysis by ID.
     */
    @Transactional(readOnly = true)
    public GapAnalysisResponse getGapAnalysis(UUID gapAnalysisId) {
        GapAnalysis gapAnalysis = gapAnalysisRepository
                .findById(gapAnalysisId)
                .orElseThrow(() -> new CustomException(
                        "Gap analysis not found: " + gapAnalysisId,
                        HttpStatus.NOT_FOUND,
                        ErrorCode.GAP_ANALYSIS_NOT_FOUND));

        return gapAnalysisMapper.toResponse(gapAnalysis);
    }

    /**
     * Get gap analyses by paper ID.
     */
    @Transactional(readOnly = true)
    public List<GapAnalysisResponse> getGapAnalysesByPaperId(UUID paperId) {
        List<GapAnalysis> gapAnalyses = gapAnalysisRepository.findByPaperIdOrderByCreatedAtDesc(paperId);
        return gapAnalyses.stream().map(gapAnalysisMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Get gap analyses by paper ID with pagination.
     */
    @Transactional(readOnly = true)
    public Page<GapAnalysisResponse> getGapAnalysesByPaperId(UUID paperId, Pageable pageable) {
        Page<GapAnalysis> gapAnalyses = gapAnalysisRepository.findByPaperIdOrderByCreatedAtDesc(paperId, pageable);
        return gapAnalyses.map(gapAnalysisMapper::toResponse);
    }

    /**
     * Get gap analyses by status.
     */
    @Transactional(readOnly = true)
    public List<GapAnalysisResponse> getGapAnalysesByStatus(GapAnalysis.GapStatus status) {
        List<GapAnalysis> gapAnalyses = gapAnalysisRepository.findByStatusOrderByCreatedAtDesc(status);
        return gapAnalyses.stream().map(gapAnalysisMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Get gap analyses by status with pagination.
     */
    @Transactional(readOnly = true)
    public Page<GapAnalysisResponse> getGapAnalysesByStatus(GapAnalysis.GapStatus status, Pageable pageable) {
        Page<GapAnalysis> gapAnalyses = gapAnalysisRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return gapAnalyses.map(gapAnalysisMapper::toResponse);
    }

    /**
     * Get the latest gap analysis for a paper.
     */
    @Transactional(readOnly = true)
    public GapAnalysisResponse getLatestGapAnalysisByPaperId(UUID paperId) {
        GapAnalysis gapAnalysis = gapAnalysisRepository
                .findLatestByPaperId(paperId)
                .orElseThrow(() -> new CustomException(
                        "No gap analysis found for paper: " + paperId,
                        HttpStatus.NOT_FOUND,
                        ErrorCode.GAP_ANALYSIS_NOT_FOUND));

        return gapAnalysisMapper.toResponse(gapAnalysis);
    }

    /**
     * Retry failed gap analysis.
     */
    @Transactional
    public GapAnalysisResponse retryGapAnalysis(UUID gapAnalysisId) {
        GapAnalysis gapAnalysis = gapAnalysisRepository
                .findById(gapAnalysisId)
                .orElseThrow(() -> new CustomException(
                        "Gap analysis not found: " + gapAnalysisId,
                        HttpStatus.NOT_FOUND,
                        ErrorCode.GAP_ANALYSIS_NOT_FOUND));

        if (gapAnalysis.getStatus() != GapAnalysis.GapStatus.FAILED) {
            throw new CustomException(
                    "Can only retry failed gap analyses. Current status: " + gapAnalysis.getStatus(),
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_OPERATION);
        }

        // Reset status and send new request
        gapAnalysis.setStatus(GapAnalysis.GapStatus.PENDING);
        gapAnalysis.setStartedAt(Instant.now());
        gapAnalysis.setCompletedAt(null);
        gapAnalysis.setErrorMessage(null);
        gapAnalysis = gapAnalysisRepository.save(gapAnalysis);

        // Create new request with existing IDs
        GapAnalysisRequest request = GapAnalysisRequest.builder()
                .paperId(gapAnalysis.getPaper().getId())
                .build();

        try {
            gapAnalysisRequestSender.sendGapAnalysisRequest(
                    request,
                    gapAnalysis.getPaperExtractionId(),
                    gapAnalysis.getCorrelationId(),
                    gapAnalysis.getRequestId());
            log.info(
                    "Gap analysis retry request sent successfully for paper: {}",
                    gapAnalysis.getPaper().getId());
        } catch (Exception e) {
            log.error(
                    "Failed to send gap analysis retry request for paper: {}",
                    gapAnalysis.getPaper().getId(),
                    e);
            gapAnalysis.setStatus(GapAnalysis.GapStatus.FAILED);
            gapAnalysis.setErrorMessage("Failed to send retry request to gap analyzer: " + e.getMessage());
            gapAnalysisRepository.save(gapAnalysis);
            throw new CustomException(
                    "Failed to retry gap analysis: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.GAP_ANALYSIS_REQUEST_FAILED);
        }

        return gapAnalysisMapper.toResponse(gapAnalysis);
    }

    /**
     * Get gap analysis request data by paper ID.
     */
    @Transactional(readOnly = true)
    public List<GapAnalysisRequestData> getGapAnalysisRequestDataByPaperId(UUID paperId) {
        List<GapAnalysis> gapAnalyses = gapAnalysisRepository.findByPaperIdOrderByCreatedAtDesc(paperId);

        return gapAnalyses.stream().map(this::convertToRequestData).collect(Collectors.toList());
    }

    /**
     * Get gap analysis statistics.
     */
    @Transactional(readOnly = true)
    public GapAnalysisStats getGapAnalysisStats() {
        long totalAnalyses = gapAnalysisRepository.count();
        long pendingAnalyses = gapAnalysisRepository.countByStatus(GapAnalysis.GapStatus.PENDING);
        long processingAnalyses = gapAnalysisRepository.countByStatus(GapAnalysis.GapStatus.PROCESSING);
        long completedAnalyses = gapAnalysisRepository.countByStatus(GapAnalysis.GapStatus.COMPLETED);
        long failedAnalyses = gapAnalysisRepository.countByStatus(GapAnalysis.GapStatus.FAILED);

        return GapAnalysisStats.builder()
                .totalAnalyses(totalAnalyses)
                .pendingAnalyses(pendingAnalyses)
                .processingAnalyses(processingAnalyses)
                .completedAnalyses(completedAnalyses)
                .failedAnalyses(failedAnalyses)
                .build();
    }

    /**
     * Convert GapAnalysis entity to GapAnalysisRequestData DTO.
     */
    private GapAnalysisRequestData convertToRequestData(GapAnalysis gapAnalysis) {
        org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisConfigDto config = null;

        if (gapAnalysis.getConfig() != null) {
            try {
                config = objectMapper.readValue(
                        gapAnalysis.getConfig(),
                        org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisConfigDto.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse config JSON for gap analysis: {}", gapAnalysis.getId(), e);
            }
        }

        return GapAnalysisRequestData.builder()
                .gapAnalysisId(gapAnalysis.getId())
                .paperId(gapAnalysis.getPaper().getId())
                .paperExtractionId(gapAnalysis.getPaperExtractionId())
                .correlationId(gapAnalysis.getCorrelationId())
                .requestId(gapAnalysis.getRequestId())
                .status(gapAnalysis.getStatus())
                .config(config)
                .startedAt(gapAnalysis.getStartedAt())
                .completedAt(gapAnalysis.getCompletedAt())
                .errorMessage(gapAnalysis.getErrorMessage())
                .createdAt(gapAnalysis.getCreatedAt())
                .updatedAt(gapAnalysis.getUpdatedAt())
                .totalGapsIdentified(gapAnalysis.getTotalGapsIdentified())
                .validGapsCount(gapAnalysis.getValidGapsCount())
                .invalidGapsCount(gapAnalysis.getInvalidGapsCount())
                .modifiedGapsCount(gapAnalysis.getModifiedGapsCount())
                .build();
    }

    /**
     * Convert GapAnalysisConfigDto to JSON string for storage.
     */
    private String convertConfigToJson(
            org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisConfigDto config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert config to JSON", e);
            return null;
        }
    }

    /**
     * Generate a unique correlation ID for distributed tracing.
     */
    private String generateCorrelationId() {
        return "gap-analysis-" + Instant.now().toEpochMilli() + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate a unique request ID for this gap analysis request.
     */
    private String generateRequestId() {
        return "req-" + Instant.now().toEpochMilli() + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Statistics DTO for gap analysis.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GapAnalysisStats {
        private long totalAnalyses;
        private long pendingAnalyses;
        private long processingAnalyses;
        private long completedAnalyses;
        private long failedAnalyses;
    }

    /**
     * Request data DTO for gap analysis.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GapAnalysisRequestData {
        private UUID gapAnalysisId;
        private UUID paperId;
        private UUID paperExtractionId;
        private String correlationId;
        private String requestId;
        private GapAnalysis.GapStatus status;
        private org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisConfigDto config;
        private Instant startedAt;
        private Instant completedAt;
        private String errorMessage;
        private Instant createdAt;
        private Instant updatedAt;

        // Gap count statistics
        private Integer totalGapsIdentified;
        private Integer validGapsCount;
        private Integer invalidGapsCount;
        private Integer modifiedGapsCount;
    }
}
