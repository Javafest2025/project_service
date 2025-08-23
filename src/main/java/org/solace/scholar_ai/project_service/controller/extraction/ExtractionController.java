package org.solace.scholar_ai.project_service.controller.extraction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.extraction.ExtractionRequest;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractionResponse;
import org.solace.scholar_ai.project_service.service.extraction.ExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing paper extraction operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/extraction")
@RequiredArgsConstructor
@Tag(name = "Paper Extraction", description = "APIs for managing paper content extraction")
public class ExtractionController {

    private final ExtractionService extractionService;

    @Operation(
            summary = "Trigger paper extraction",
            description = "Triggers content extraction for a paper using its PDF content URL")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Extraction triggered successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request or paper has no PDF URL"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @PostMapping("/trigger")
    public ResponseEntity<ExtractionResponse> triggerExtraction(@Valid @RequestBody ExtractionRequest request) {

        log.info("Received extraction request for paper ID: {}", request.paperId());

        ExtractionResponse response = extractionService.triggerExtraction(request);

        log.info("Extraction triggered for paper {}, job ID: {}", request.paperId(), response.jobId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get extraction status",
            description = "Gets the current extraction status and progress for a paper")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @GetMapping("/status/{paperId}")
    public ResponseEntity<ExtractionResponse> getExtractionStatus(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId) {

        log.info("Received extraction status request for paper ID: {}", paperId);

        ExtractionResponse response = extractionService.getExtractionStatus(paperId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Trigger extraction with default options",
            description = "Triggers content extraction for a paper with default extraction options")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Extraction triggered successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request or paper has no PDF URL"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @PostMapping("/trigger/{paperId}")
    public ResponseEntity<ExtractionResponse> triggerExtractionForPaper(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId,
            @Parameter(description = "Process asynchronously") @RequestParam(defaultValue = "true")
                    Boolean asyncProcessing) {

        log.info("Received extraction request for paper ID: {} (async: {})", paperId, asyncProcessing);

        // Create default extraction request
        ExtractionRequest request = new ExtractionRequest(
                paperId,
                true, // extractText
                true, // extractFigures
                true, // extractTables
                true, // extractEquations
                true, // extractCode
                true, // extractReferences
                true, // useOcr
                true, // detectEntities
                asyncProcessing);

        ExtractionResponse response = extractionService.triggerExtraction(request);

        log.info("Extraction triggered for paper {}, job ID: {}", paperId, response.jobId());

        return ResponseEntity.ok(response);
    }
}
