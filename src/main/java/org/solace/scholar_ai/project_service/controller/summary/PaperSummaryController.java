package org.solace.scholar_ai.project_service.controller.summary;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.summary.PaperSummaryResponseDto;
import org.solace.scholar_ai.project_service.model.summary.PaperSummary;
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

    @Operation(summary = "Generate summary for a paper")
    @PostMapping("/generate")
    public ResponseEntity<PaperSummaryResponseDto> generateSummary(@PathVariable UUID paperId) {
        log.info("Received request to generate summary for paper: {}", paperId);

        // Check if summary already exists
        if (summaryRepository.findByPaperId(paperId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null); // Summary already exists
        }

        PaperSummary summary = summaryGenerationService.generateSummary(paperId);
        return ResponseEntity.ok(PaperSummaryResponseDto.fromEntity(summary));
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
}
