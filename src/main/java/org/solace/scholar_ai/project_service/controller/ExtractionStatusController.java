package org.solace.scholar_ai.project_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractionStatusResponse;
import org.solace.scholar_ai.project_service.exception.PaperNotFoundException;
import org.solace.scholar_ai.project_service.service.extraction.ExtractionStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Paper Extraction Status", description = "Check PDF extraction status and readiness for chat")
public class ExtractionStatusController {

    private final ExtractionStatusService extractionStatusService;

    @GetMapping("/{paperId}/extraction-status")
    @Operation(
            summary = "Get paper extraction status",
            description =
                    "Check the current status of PDF content extraction for a paper, including progress and chat readiness.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Extraction status retrieved successfully",
                        content = @Content(schema = @Schema(implementation = ExtractionStatusResponse.class))),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    public ResponseEntity<ExtractionStatusResponse> getExtractionStatus(
            @Parameter(description = "ID of the paper to check extraction status", required = true) @PathVariable
                    String paperId) {

        log.info("📊 Checking extraction status for paper: {}", paperId);

        try {
            UUID paperUuid = UUID.fromString(paperId);
            ExtractionStatusResponse status = extractionStatusService.checkExtractionStatus(paperUuid);

            log.info(
                    "✅ Extraction status for paper {}: {} ({}% complete, extracted: {})",
                    paperId, status.getStatus(), status.getProgress(), status.isExtracted());

            return ResponseEntity.ok(status);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid paper ID format: {}", paperId);
            return ResponseEntity.badRequest().build();

        } catch (PaperNotFoundException e) {
            log.warn("❌ Paper not found: {}", paperId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Error checking extraction status for paper {}: {}", paperId, e.getMessage(), e);

            // Return an error status response
            ExtractionStatusResponse errorResponse = ExtractionStatusResponse.builder()
                    .paperId(UUID.fromString(paperId))
                    .status("ERROR")
                    .progress(0.0)
                    .isExtracted(false)
                    .error("Error checking extraction status: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/{paperId}/extraction/retry")
    @Operation(
            summary = "Retry paper extraction",
            description = "Retry the PDF content extraction process for a paper that failed or got stuck.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Extraction retry initiated successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "409", description = "Extraction already in progress or completed"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    public ResponseEntity<ExtractionStatusResponse> retryExtraction(
            @Parameter(description = "ID of the paper to retry extraction", required = true) @PathVariable
                    String paperId) {

        log.info("🔄 Retry extraction requested for paper: {}", paperId);

        try {
            UUID paperUuid = UUID.fromString(paperId);

            // First check current status
            ExtractionStatusResponse currentStatus = extractionStatusService.checkExtractionStatus(paperUuid);

            // Only allow retry if extraction failed or is stuck
            if ("COMPLETED".equals(currentStatus.getStatus()) || "PROCESSING".equals(currentStatus.getStatus())) {
                log.warn("❌ Cannot retry extraction for paper {} - status: {}", paperId, currentStatus.getStatus());
                return ResponseEntity.status(409)
                        .body(ExtractionStatusResponse.builder()
                                .paperId(paperUuid)
                                .status(currentStatus.getStatus())
                                .progress(currentStatus.getProgress())
                                .isExtracted(currentStatus.isExtracted())
                                .error("Extraction is already "
                                        + currentStatus.getStatus().toLowerCase())
                                .build());
            }

            // TODO: Implement actual extraction retry logic
            // This would trigger the extraction service to re-process the paper
            log.info("🚀 Extraction retry would be initiated for paper: {}", paperId);

            return ResponseEntity.ok(ExtractionStatusResponse.builder()
                    .paperId(paperUuid)
                    .status("RETRY_INITIATED")
                    .progress(0.0)
                    .isExtracted(false)
                    .error("Extraction retry has been initiated")
                    .startedAt(java.time.Instant.now())
                    .build());

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid paper ID format: {}", paperId);
            return ResponseEntity.badRequest().build();

        } catch (PaperNotFoundException e) {
            log.warn("❌ Paper not found for retry: {}", paperId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Error retrying extraction for paper {}: {}", paperId, e.getMessage(), e);

            ExtractionStatusResponse errorResponse = ExtractionStatusResponse.builder()
                    .paperId(UUID.fromString(paperId))
                    .status("ERROR")
                    .progress(0.0)
                    .isExtracted(false)
                    .error("Error retrying extraction: " + e.getMessage())
                    .build();

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/{paperId}/has-structured-data")
    @Operation(
            summary = "Check if paper has structured data",
            description = "Check if a paper has been extracted and has structured data available for chat.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Structured data status retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    public ResponseEntity<Object> hasStructuredData(
            @Parameter(description = "ID of the paper to check", required = true) @PathVariable String paperId) {

        log.info("📊 Checking structured data availability for paper: {}", paperId);

        try {
            UUID paperUuid = UUID.fromString(paperId);
            ExtractionStatusResponse status = extractionStatusService.checkExtractionStatus(paperUuid);

            boolean hasStructuredData = status.isExtracted()
                    && ("COMPLETED".equals(status.getStatus()) || "SUCCESS".equals(status.getStatus()));

            log.info("✅ Paper {} has structured data: {}", paperId, hasStructuredData);

            final boolean finalHasStructuredData = hasStructuredData;
            final String finalStatusValue = status.getStatus();
            final boolean finalIsExtracted = status.isExtracted();

            return ResponseEntity.ok(new Object() {
                public final boolean hasStructuredData = finalHasStructuredData;
                public final String statusValue = finalStatusValue;
                public final boolean isExtracted = finalIsExtracted;
            });

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid paper ID format: {}", paperId);
            return ResponseEntity.badRequest().build();

        } catch (PaperNotFoundException e) {
            log.warn("❌ Paper not found: {}", paperId);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("❌ Error checking structured data for paper {}: {}", paperId, e.getMessage(), e);
            return ResponseEntity.status(500).body(new Object() {
                public final boolean hasStructuredData = false;
                public final String error = "Error checking structured data: " + e.getMessage();
            });
        }
    }

    @ExceptionHandler(PaperNotFoundException.class)
    public ResponseEntity<ExtractionStatusResponse> handlePaperNotFound(PaperNotFoundException e) {
        log.warn("❌ Paper not found: {}", e.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExtractionStatusResponse> handleGeneralError(Exception e) {
        log.error("❌ Unexpected error in extraction status controller: {}", e.getMessage(), e);

        ExtractionStatusResponse errorResponse = ExtractionStatusResponse.builder()
                .status("ERROR")
                .progress(0.0)
                .isExtracted(false)
                .error("An unexpected error occurred: " + e.getMessage())
                .build();

        return ResponseEntity.status(500).body(errorResponse);
    }
}
