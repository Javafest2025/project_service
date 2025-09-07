package org.solace.scholar_ai.project_service.controller.summary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.summary.PaperSummaryResponseDto;
import org.solace.scholar_ai.project_service.model.paper.Paper;
import org.solace.scholar_ai.project_service.model.summary.PaperSummary;
import org.solace.scholar_ai.project_service.repository.paper.PaperRepository;
import org.solace.scholar_ai.project_service.repository.summary.PaperSummaryRepository;
import org.solace.scholar_ai.project_service.service.summary.PaperSummaryGenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/papers/{paperId}/summary")
@RequiredArgsConstructor
@Tag(name = "Paper Summary", description = "API for generating and managing paper summaries")
public class PaperSummaryController {

    private final PaperSummaryGenerationService summaryGenerationService;
    private final PaperSummaryRepository summaryRepository;
    private final PaperRepository paperRepository;

    @Operation(summary = "Generate summary for a paper")
    @PostMapping("/generate")
    public ResponseEntity<PaperSummaryResponseDto> generateSummary(@PathVariable UUID paperId) {
        log.info("Received request to generate summary for paper: {}", paperId);

        // Check if summary already exists
        if (summaryRepository.findByPaperId(paperId).isPresent()) {
            log.info("Summary already exists for paper: {}, returning existing summary", paperId);
            PaperSummary existingSummary =
                    summaryRepository.findByPaperId(paperId).get();
            return ResponseEntity.ok(PaperSummaryResponseDto.fromEntity(existingSummary));
        }

        try {
            PaperSummary summary = summaryGenerationService.generateSummary(paperId);
            return ResponseEntity.ok(PaperSummaryResponseDto.fromEntity(summary));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Handle race condition: another process created the summary while we were
            // generating
            log.info("Race condition detected for paper: {}, another process created the summary", paperId);
            if (summaryRepository.findByPaperId(paperId).isPresent()) {
                PaperSummary existingSummary =
                        summaryRepository.findByPaperId(paperId).get();
                return ResponseEntity.ok(PaperSummaryResponseDto.fromEntity(existingSummary));
            }
            // If summary still doesn't exist, re-throw the exception
            throw e;
        }
    }

    @Operation(summary = "Regenerate summary for a paper")
    @PostMapping("/regenerate")
    public ResponseEntity<PaperSummaryResponseDto> regenerateSummary(@PathVariable UUID paperId) {
        log.info("Received request to regenerate summary for paper: {}", paperId);

        // Delete existing summary if present
        summaryRepository.findByPaperId(paperId).ifPresent(summaryRepository::delete);

        PaperSummary summary = summaryGenerationService.generateSummary(paperId);
        return ResponseEntity.ok(PaperSummaryResponseDto.fromEntity(summary));
    }

    @Operation(summary = "Get summary for a paper")
    @GetMapping
    public ResponseEntity<PaperSummaryResponseDto> getSummary(@PathVariable UUID paperId) {
        return summaryRepository
                .findByPaperId(paperId)
                .map(PaperSummaryResponseDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Update validation status")
    @PatchMapping("/validation")
    public ResponseEntity<PaperSummary> updateValidationStatus(
            @PathVariable UUID paperId,
            @RequestParam PaperSummary.ValidationStatus status,
            @RequestParam(required = false) String notes) {

        return summaryRepository
                .findByPaperId(paperId)
                .map(summary -> {
                    summary.setValidationStatus(status);
                    if (notes != null) {
                        summary.setValidationNotes(notes);
                    }
                    return ResponseEntity.ok(summaryRepository.save(summary));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Check if paper is summarized")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSummarizationStatus(@PathVariable UUID paperId) {
        log.info("Checking summarization status for paper: {}", paperId);

        try {
            Paper paper = paperRepository
                    .findById(paperId)
                    .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

            Map<String, Object> status = new HashMap<>();
            status.put("paperId", paperId);
            status.put("isSummarized", paper.getIsSummarized());
            status.put("summarizationStatus", paper.getSummarizationStatus());
            status.put("summarizationStartedAt", paper.getSummarizationStartedAt());
            status.put("summarizationCompletedAt", paper.getSummarizationCompletedAt());
            status.put("summarizationError", paper.getSummarizationError());

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error checking summarization status for paper: {}", paperId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check summarization status: " + e.getMessage()));
        }
    }

    @Operation(summary = "Check if paper is summarized")
    @GetMapping("/summarized")
    public ResponseEntity<Boolean> isPaperSummarized(@PathVariable UUID paperId) {
        log.info("Checking if paper is summarized for paper ID: {}", paperId);

        try {
            Paper paper = paperRepository
                    .findById(paperId)
                    .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

            Boolean isSummarized = paper.getIsSummarized();
            return ResponseEntity.ok(isSummarized != null ? isSummarized : false);
        } catch (Exception e) {
            log.error("Error checking if paper is summarized for paper: {}", paperId, e);
            return ResponseEntity.ok(false);
        }
    }

    @Operation(summary = "Get summarization status only")
    @GetMapping("/status-only")
    public ResponseEntity<Map<String, String>> getSummarizationStatusOnly(@PathVariable UUID paperId) {
        log.info("Getting summarization status only for paper ID: {}", paperId);

        try {
            Paper paper = paperRepository
                    .findById(paperId)
                    .orElseThrow(() -> new RuntimeException("Paper not found: " + paperId));

            String status = paper.getSummarizationStatus();
            Map<String, String> response = new HashMap<>();
            response.put("status", status != null ? status : "UNKNOWN");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting summarization status for paper: {}", paperId, e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "UNKNOWN");
            return ResponseEntity.ok(response);
        }
    }
}
