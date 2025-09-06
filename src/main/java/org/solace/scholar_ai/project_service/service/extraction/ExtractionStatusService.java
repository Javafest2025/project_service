package org.solace.scholar_ai.project_service.service.extraction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractionStatusResponse;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for checking and managing PDF extraction status
 * Handles extraction status queries and progress calculations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExtractionStatusService {

    private final PaperRepository paperRepository;

    /**
     * Check the extraction status of a paper
     * @param paperId UUID of the paper to check
     * @return ExtractionStatusResponse with current status and progress
     */
    public ExtractionStatusResponse checkExtractionStatus(UUID paperId) {
        log.info("Checking extraction status for paper: {}", paperId);

        Paper paper = paperRepository
                .findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException("Paper not found with ID: " + paperId));

        return ExtractionStatusResponse.builder()
                .paperId(paperId)
                .isExtracted(Boolean.TRUE.equals(paper.getIsExtracted()))
                .status(determineStatus(paper))
                .progress(calculateProgress(paper))
                .extractionId(paper.getExtractionJobId())
                .startedAt(paper.getExtractionStartedAt())
                .completedAt(paper.getExtractionCompletedAt())
                .error(paper.getExtractionError())
                .estimatedTimeRemaining(estimateTimeRemaining(paper))
                .build();
    }

    /**
     * Determine the current extraction status
     */
    private String determineStatus(Paper paper) {
        if (Boolean.TRUE.equals(paper.getIsExtracted())) {
            return "COMPLETED";
        }

        String status = paper.getExtractionStatus();
        if (status == null) {
            return "NOT_STARTED";
        }

        return status; // PENDING, PROCESSING, COMPLETED, FAILED
    }

    /**
     * Calculate extraction progress percentage (0-100)
     */
    private Double calculateProgress(Paper paper) {
        if (Boolean.TRUE.equals(paper.getIsExtracted())) {
            return 100.0;
        }

        String status = paper.getExtractionStatus();
        if ("FAILED".equals(status)) {
            return 0.0;
        }

        if ("PENDING".equals(status)) {
            return 5.0; // Just started
        }

        if ("PROCESSING".equals(status)) {
            // Estimate progress based on time elapsed
            if (paper.getExtractionStartedAt() != null) {
                long elapsedMinutes = ChronoUnit.MINUTES.between(paper.getExtractionStartedAt(), Instant.now());

                // Assume average extraction takes 2-5 minutes
                // Progress grows non-linearly (faster at start, slower near end)
                double timeProgress = Math.min(elapsedMinutes / 4.0, 0.95);
                return 10.0 + (timeProgress * 85.0); // 10% to 95%
            }
            return 15.0; // Default processing
        }

        // Use coverage if available
        Double coverage = paper.getExtractionCoverage();
        return coverage != null ? coverage : 0.0;
    }

    /**
     * Estimate remaining time in seconds
     */
    private Integer estimateTimeRemaining(Paper paper) {
        if (Boolean.TRUE.equals(paper.getIsExtracted()) || "FAILED".equals(paper.getExtractionStatus())) {
            return 0;
        }

        if (paper.getExtractionStartedAt() == null) {
            return 300; // 5 minutes default estimate
        }

        long elapsedSeconds = ChronoUnit.SECONDS.between(paper.getExtractionStartedAt(), Instant.now());

        // Assume total time is 3-6 minutes based on PDF complexity
        long estimatedTotal = 240; // 4 minutes average
        long remaining = estimatedTotal - elapsedSeconds;

        return Math.max(0, (int) remaining);
    }

    /**
     * Check if a paper is ready for contextual chat
     */
    public boolean isChatReady(UUID paperId) {
        try {
            ExtractionStatusResponse status = checkExtractionStatus(paperId);
            return status.isExtracted() && "COMPLETED".equals(status.getStatus());
        } catch (Exception e) {
            log.error("Error checking chat readiness for paper: {}", paperId, e);
            return false;
        }
    }

    /**
     * Get extraction summary for chat context display
     */
    public ExtractionStatusResponse.ExtractionSummary getExtractionSummary(UUID paperId) {
        log.info("Getting extraction summary for paper: {}", paperId);

        Paper paper = paperRepository
                .findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException("Paper not found with ID: " + paperId));

        if (!Boolean.TRUE.equals(paper.getIsExtracted())) {
            return null;
        }

        // Get extraction details if available
        if (paper.getPaperExtraction() != null) {
            var extraction = paper.getPaperExtraction();
            return ExtractionStatusResponse.ExtractionSummary.builder()
                    .sectionsCount(extraction.getSections().size())
                    .figuresCount(extraction.getFigures().size())
                    .tablesCount(extraction.getTables().size())
                    .equationsCount(extraction.getEquations().size())
                    .referencesCount(extraction.getReferences().size())
                    .pageCount(extraction.getPageCount())
                    .language(extraction.getLanguage())
                    .extractionCoverage(extraction.getExtractionCoverage())
                    .contentTypes(determineContentTypes(extraction))
                    .build();
        }

        // Fallback summary
        return ExtractionStatusResponse.ExtractionSummary.builder()
                .sectionsCount(0)
                .figuresCount(0)
                .tablesCount(0)
                .equationsCount(0)
                .referencesCount(0)
                .pageCount(null)
                .language("unknown")
                .extractionCoverage(paper.getExtractionCoverage())
                .contentTypes(java.util.List.of("text"))
                .build();
    }

    private java.util.List<String> determineContentTypes(
            org.solace.scholar_ai.project_service.model.extraction.PaperExtraction extraction) {
        java.util.List<String> types = new java.util.ArrayList<>();

        if (!extraction.getSections().isEmpty()) types.add("text");
        if (!extraction.getFigures().isEmpty()) types.add("figures");
        if (!extraction.getTables().isEmpty()) types.add("tables");
        if (!extraction.getEquations().isEmpty()) types.add("equations");
        if (!extraction.getReferences().isEmpty()) types.add("references");

        return types;
    }
}
