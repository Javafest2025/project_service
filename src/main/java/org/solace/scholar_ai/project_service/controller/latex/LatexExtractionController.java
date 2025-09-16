package org.solace.scholar_ai.project_service.controller.latex;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.extraction.ExtractionRequest;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractionResponse;
import org.solace.scholar_ai.project_service.service.extraction.ExtractionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/latex/context/extraction")
@RequiredArgsConstructor
@Tag(
        name = "📝 LaTeX Context Extraction",
        description = "Batch trigger extraction for papers used in LaTeX editor context")
public class LatexExtractionController {

    private final ExtractionService extractionService;

    @PostMapping("/trigger")
    @Operation(
            summary = "Trigger extraction for multiple papers",
            description = "Accepts a list of paper IDs, skips those already extracted or currently processing, "
                    + "and triggers extraction for the rest. Returns per-paper results without failing the batch.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Batch extraction processed",
                        content = @Content(schema = @Schema(implementation = APIResponse.class)))
            })
    public ResponseEntity<APIResponse<BatchExtractionResponse>> triggerBatchExtraction(
            @Valid @RequestBody BatchExtractionRequest request) {
        List<BatchExtractionItemResult> results = new ArrayList<>();

        boolean async = request.asyncProcessing() != null ? request.asyncProcessing() : true;

        for (UUID paperUuid : request.paperIds()) {
            String paperId = paperUuid.toString();
            try {
                // If already extracted, skip
                Boolean alreadyExtracted = extractionService.isPaperExtracted(paperId);
                if (Boolean.TRUE.equals(alreadyExtracted)) {
                    results.add(BatchExtractionItemResult.skippedAlreadyExtracted(paperId));
                    continue;
                }

                // If currently processing, skip triggering again
                String status = extractionService.getExtractionStatusOnly(paperId);
                if (status != null && ("PROCESSING".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status))) {
                    results.add(BatchExtractionItemResult.skippedInProgress(paperId, status));
                    continue;
                }

                // Trigger extraction with default options (all true), async as requested
                ExtractionResponse resp = extractionService.triggerExtraction(new ExtractionRequest(
                        paperId,
                        null, // extractText (default true)
                        null, // extractFigures (default true)
                        null, // extractTables (default true)
                        null, // extractEquations (default true)
                        null, // extractCode (default true)
                        null, // extractReferences (default true)
                        null, // useOcr (default true)
                        null, // detectEntities (default true)
                        async // asyncProcessing
                        ));

                results.add(BatchExtractionItemResult.triggered(paperId, resp.jobId(), resp.status(), resp.message()));
            } catch (Exception e) {
                log.warn("Batch extraction error for paper {}: {}", paperId, e.getMessage());
                results.add(BatchExtractionItemResult.error(paperId, e.getMessage()));
            }
        }

        BatchExtractionResponse body = BatchExtractionResponse.from(results);
        return ResponseEntity.ok(APIResponse.success(
                HttpStatus.OK.value(),
                String.format(
                        "Batch processed: %d total, %d triggered, %d skipped (extracted), %d in-progress, %d errors",
                        body.total(),
                        body.triggered(),
                        body.skippedAlreadyExtracted(),
                        body.skippedInProgress(),
                        body.errors()),
                body));
    }

    // -------- DTOs --------

    @Schema(description = "Batch extraction trigger request")
    public record BatchExtractionRequest(
            @NotEmpty(message = "paperIds cannot be empty") List<@NotNull UUID> paperIds,
            @Schema(description = "Process asynchronously", example = "true") Boolean asyncProcessing) {}

    @Schema(description = "Per-paper batch extraction result item")
    public record BatchExtractionItemResult(
            String paperId,
            String action, // TRIGGERED | SKIPPED_ALREADY_EXTRACTED | SKIPPED_IN_PROGRESS | ERROR
            String status, // initial known status if any
            String jobId,
            String message) {
        public static BatchExtractionItemResult skippedAlreadyExtracted(String paperId) {
            return new BatchExtractionItemResult(
                    paperId, "SKIPPED_ALREADY_EXTRACTED", "COMPLETED", null, "Already extracted");
        }

        public static BatchExtractionItemResult skippedInProgress(String paperId, String status) {
            return new BatchExtractionItemResult(
                    paperId, "SKIPPED_IN_PROGRESS", status, null, "Extraction already in progress");
        }

        public static BatchExtractionItemResult triggered(String paperId, String jobId, String status, String message) {
            return new BatchExtractionItemResult(paperId, "TRIGGERED", status, jobId, message);
        }

        public static BatchExtractionItemResult error(String paperId, String message) {
            return new BatchExtractionItemResult(paperId, "ERROR", "FAILED", null, message);
        }
    }

    @Schema(description = "Batch extraction response with summary")
    public record BatchExtractionResponse(
            int total,
            int triggered,
            int skippedAlreadyExtracted,
            int skippedInProgress,
            int errors,
            List<BatchExtractionItemResult> results) {

        public static BatchExtractionResponse from(List<BatchExtractionItemResult> items) {
            int triggered = 0;
            int skippedExtracted = 0;
            int skippedInProgress = 0;
            int errors = 0;
            for (BatchExtractionItemResult i : items) {
                if ("TRIGGERED".equals(i.action())) triggered++;
                else if ("SKIPPED_ALREADY_EXTRACTED".equals(i.action())) skippedExtracted++;
                else if ("SKIPPED_IN_PROGRESS".equals(i.action())) skippedInProgress++;
                else if ("ERROR".equals(i.action())) errors++;
            }
            return new BatchExtractionResponse(
                    items.size(), triggered, skippedExtracted, skippedInProgress, errors, items);
        }
    }
}
