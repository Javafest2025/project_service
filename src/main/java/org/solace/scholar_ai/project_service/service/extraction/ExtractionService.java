package org.solace.scholar_ai.project_service.service.extraction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.event.extraction.ExtractionCompletedEvent;
import org.solace.scholar_ai.project_service.dto.request.extraction.ExtractionRequest;
import org.solace.scholar_ai.project_service.dto.request.extraction.ExtractorMessageRequest;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractedFigureResponse;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractedTableResponse;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractionResponse;
import org.solace.scholar_ai.project_service.exception.CustomException;
import org.solace.scholar_ai.project_service.exception.ErrorCode;
import org.solace.scholar_ai.project_service.messaging.publisher.extraction.ExtractionRequestSender;
import org.solace.scholar_ai.project_service.model.extraction.PaperExtraction;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.extraction.PaperExtractionRepository;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.service.extraction.persistence.ExtractionPersistenceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing paper extraction operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractionService {

    private final PaperRepository paperRepository;
    private final PaperExtractionRepository paperExtractionRepository;
    private final ExtractionRequestSender extractionRequestSender;
    private final ExtractionPersistenceService extractionPersistenceService;

    /**
     * Trigger extraction for a paper
     *
     * @param request The extraction request
     * @return ExtractionResponse with job details
     */
    @Transactional
    public ExtractionResponse triggerExtraction(ExtractionRequest request) {
        log.info("Triggering extraction for paper ID: {}", request.paperId());

        // Find the paper
        Paper paper = paperRepository
                .findById(UUID.fromString(request.paperId()))
                .orElseThrow(() -> new CustomException(
                        "Paper not found with ID: " + request.paperId(),
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND));

        // Check if paper has a PDF URL
        if (paper.getPdfContentUrl() == null || paper.getPdfContentUrl().trim().isEmpty()) {
            throw new CustomException(
                    "Paper does not have a PDF content URL available for extraction",
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_REQUEST);
        }

        // Check if extraction is already in progress
        String currentStatus = paper.getExtractionStatus();
        if ("PROCESSING".equals(currentStatus)) {
            return new ExtractionResponse(
                    paper.getExtractionJobId(),
                    request.paperId(),
                    currentStatus,
                    "Extraction already in progress",
                    paper.getPdfContentUrl(),
                    paper.getExtractionStartedAt(),
                    paper.getExtractionCompletedAt(),
                    null,
                    null);
        }

        // Generate job ID
        String jobId = UUID.randomUUID().toString();

        // Update paper extraction status
        paper.setExtractionJobId(jobId);
        paper.setExtractionStatus("PENDING");
        paper.setExtractionStartedAt(Instant.now());
        paper.setExtractionCompletedAt(null);
        paper.setExtractionError(null);
        paper.setIsExtracted(false);

        paperRepository.save(paper);

        // Create extractor message
        ExtractorMessageRequest extractorRequest = new ExtractorMessageRequest(
                jobId,
                request.paperId(),
                paper.getCorrelationId(),
                paper.getPdfContentUrl(),
                request.extractText(),
                request.extractFigures(),
                request.extractTables(),
                request.extractEquations(),
                request.extractCode(),
                request.extractReferences(),
                request.useOcr(),
                request.detectEntities());

        // Send message to extractor service
        try {
            extractionRequestSender.send(extractorRequest);

            // Update status to processing
            paper.setExtractionStatus("PROCESSING");
            paperRepository.save(paper);

            log.info("Successfully triggered extraction for paper {} with job ID: {}", request.paperId(), jobId);

        } catch (Exception e) {
            log.error("Failed to send extraction request for paper {}: {}", request.paperId(), e.getMessage(), e);

            // Update status to failed
            paper.setExtractionStatus("FAILED");
            paper.setExtractionError("Failed to send extraction request: " + e.getMessage());
            paper.setExtractionCompletedAt(Instant.now());
            paperRepository.save(paper);

            throw new CustomException(
                    "Failed to trigger extraction: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.INTERNAL_ERROR);
        }

        return new ExtractionResponse(
                jobId,
                request.paperId(),
                "PROCESSING",
                "Extraction job started successfully",
                paper.getPdfContentUrl(),
                paper.getExtractionStartedAt(),
                null,
                null,
                null);
    }

    /**
     * Get extraction status for a paper
     *
     * @param paperId The paper ID
     * @return ExtractionResponse with current status
     */
    @Transactional(readOnly = true)
    public ExtractionResponse getExtractionStatus(String paperId) {
        log.info("Getting extraction status for paper ID: {}", paperId);

        Paper paper = paperRepository
                .findById(UUID.fromString(paperId))
                .orElseThrow(() -> new CustomException(
                        "Paper not found with ID: " + paperId, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND));

        return new ExtractionResponse(
                paper.getExtractionJobId(),
                paperId,
                paper.getExtractionStatus(),
                getStatusMessage(paper.getExtractionStatus()),
                paper.getPdfContentUrl(),
                paper.getExtractionStartedAt(),
                paper.getExtractionCompletedAt(),
                calculateProgress(paper),
                paper.getExtractionError());
    }

    /**
     * Handle extraction completion event
     *
     * @param event The extraction completion event
     */
    @Transactional
    public void handleExtractionCompleted(ExtractionCompletedEvent event) {
        log.info("Handling extraction completion for job ID: {} (paper: {})", event.jobId(), event.paperId());

        // Find the paper
        Paper paper = paperRepository
                .findById(UUID.fromString(event.paperId()))
                .orElseThrow(() -> new CustomException(
                        "Paper not found with ID: " + event.paperId(),
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND));

        // Update paper extraction status
        paper.setExtractionStatus(event.status().toUpperCase());
        paper.setExtractionCompletedAt(event.completedAt());
        paper.setExtractionCoverage(event.extractionCoverage());

        if ("COMPLETED".equals(event.status().toUpperCase())) {
            paper.setIsExtracted(true);
            paper.setExtractionError(null);

            // Persist extraction results to database
            if (event.extractionResult() != null
                    && !event.extractionResult().trim().isEmpty()) {
                try {
                    extractionPersistenceService.persistExtractionResult(paper, event);
                    log.info("Successfully persisted extraction results for paper: {}", event.paperId());
                } catch (Exception e) {
                    log.error(
                            "Failed to persist extraction results for paper {}: {}",
                            event.paperId(),
                            e.getMessage(),
                            e);
                    // Don't fail the whole process, just log the error
                }
            }
        } else {
            paper.setIsExtracted(false);
            paper.setExtractionError(event.message());
        }

        paperRepository.save(paper);

        log.info("Successfully updated extraction status for paper {} to {}", event.paperId(), event.status());
    }

    private String getStatusMessage(String status) {
        if (status == null) {
            return "No extraction status available";
        }
        return switch (status) {
            case "PENDING" -> "Extraction request is pending";
            case "PROCESSING" -> "Extraction is in progress";
            case "COMPLETED" -> "Extraction completed successfully";
            case "FAILED" -> "Extraction failed";
            default -> "Unknown status";
        };
    }

    private Double calculateProgress(Paper paper) {
        String status = paper.getExtractionStatus();
        if (status == null) {
            return null;
        }

        if ("COMPLETED".equals(status)) {
            return 100.0;
        } else if ("PROCESSING".equals(status)) {
            // Simple progress estimation based on time elapsed
            if (paper.getExtractionStartedAt() != null) {
                long elapsed = Instant.now().getEpochSecond()
                        - paper.getExtractionStartedAt().getEpochSecond();
                // Assume average extraction takes 5 minutes (300 seconds)
                double estimatedProgress = Math.min(95.0, (elapsed / 300.0) * 100.0);
                return estimatedProgress;
            }
        }
        return null;
    }

    /**
     * Get extraction status only for a paper
     *
     * @param paperId The paper ID
     * @return String with current extraction status
     */
    @Transactional(readOnly = true)
    public String getExtractionStatusOnly(String paperId) {
        log.info("Getting extraction status only for paper ID: {}", paperId);

        Paper paper = paperRepository
                .findById(UUID.fromString(paperId))
                .orElseThrow(() -> new CustomException(
                        "Paper not found with ID: " + paperId, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND));

        return paper.getExtractionStatus() != null ? paper.getExtractionStatus() : "UNKNOWN";
    }

    /**
     * Check if a paper has been successfully extracted
     *
     * @param paperId The paper ID
     * @return Boolean indicating if the paper is extracted
     */
    @Transactional(readOnly = true)
    public Boolean isPaperExtracted(String paperId) {
        log.info("Checking if paper is extracted for paper ID: {}", paperId);

        Paper paper = paperRepository
                .findById(UUID.fromString(paperId))
                .orElseThrow(() -> new CustomException(
                        "Paper not found with ID: " + paperId, HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND));

        return paper.getIsExtracted() != null ? paper.getIsExtracted() : false;
    }

    /**
     * Get all extracted figures for a paper
     *
     * @param paperId The paper ID
     * @return List of extracted figures
     */
    @Transactional(readOnly = true)
    public List<ExtractedFigureResponse> getExtractedFigures(String paperId) {
        log.info("Getting extracted figures for paper ID: {}", paperId);

        // Find the paper extraction
        PaperExtraction paperExtraction = paperExtractionRepository
                .findByPaperId(UUID.fromString(paperId))
                .orElseThrow(() -> new CustomException(
                        "Paper extraction not found for paper ID: " + paperId,
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND));

        // Convert figures to response DTOs
        return paperExtraction.getFigures().stream()
                .map(figure -> new ExtractedFigureResponse(
                        figure.getFigureId(),
                        figure.getLabel(),
                        figure.getCaption(),
                        figure.getPage(),
                        figure.getImagePath(),
                        figure.getFigureType(),
                        figure.getOrderIndex()))
                .toList();
    }

    /**
     * Get all extracted tables for a paper
     *
     * @param paperId The paper ID
     * @return List of extracted tables
     */
    @Transactional(readOnly = true)
    public List<ExtractedTableResponse> getExtractedTables(String paperId) {
        log.info("Getting extracted tables for paper ID: {}", paperId);

        // Find the paper extraction
        PaperExtraction paperExtraction = paperExtractionRepository
                .findByPaperId(UUID.fromString(paperId))
                .orElseThrow(() -> new CustomException(
                        "Paper extraction not found for paper ID: " + paperId,
                        HttpStatus.NOT_FOUND,
                        ErrorCode.RESOURCE_NOT_FOUND));

        // Convert tables to response DTOs
        return paperExtraction.getTables().stream()
                .map(table -> new ExtractedTableResponse(
                        table.getTableId(),
                        table.getLabel(),
                        table.getCaption(),
                        table.getPage(),
                        table.getCsvPath(),
                        table.getOrderIndex()))
                .toList();
    }
}
