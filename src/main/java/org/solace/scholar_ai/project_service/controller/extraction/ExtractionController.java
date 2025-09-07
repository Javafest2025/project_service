package org.solace.scholar_ai.project_service.controller.extraction;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.extraction.ExtractionRequest;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractedFigureResponse;
import org.solace.scholar_ai.project_service.dto.response.extraction.ExtractedTableResponse;
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
            summary = "Get extraction status only",
            description = "Gets only the extraction status string for a paper")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @GetMapping("/status-only/{paperId}")
    public ResponseEntity<Map<String, String>> getExtractionStatusOnly(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId) {

        log.info("Received extraction status-only request for paper ID: {}", paperId);

        String status = extractionService.getExtractionStatusOnly(paperId);

        Map<String, String> response = new HashMap<>();
        response.put("status", status);

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

    @Operation(
            summary = "Check if paper is extracted",
            description = "Returns a boolean indicating whether the paper content has been successfully extracted")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Extraction status retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @GetMapping("/extracted/{paperId}")
    public ResponseEntity<Boolean> isPaperExtracted(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId) {

        log.info("Checking if paper is extracted for paper ID: {}", paperId);

        Boolean isExtracted = extractionService.isPaperExtracted(paperId);

        return ResponseEntity.ok(isExtracted);
    }

    @Operation(
            summary = "Get extracted figures for a paper",
            description = "Retrieves all extracted figures for a paper including image path, caption, and page number")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Figures retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper or extraction not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @GetMapping("/figures/{paperId}")
    public ResponseEntity<List<ExtractedFigureResponse>> getExtractedFigures(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId) {

        log.info("Received request for extracted figures for paper ID: {}", paperId);

        List<ExtractedFigureResponse> figures = extractionService.getExtractedFigures(paperId);

        log.info("Retrieved {} figures for paper ID: {}", figures.size(), paperId);

        return ResponseEntity.ok(figures);
    }

    @Operation(
            summary = "Get extracted tables for a paper",
            description = "Retrieves all extracted tables for a paper including CSV path, caption, and page number")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Tables retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper or extraction not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @GetMapping("/tables/{paperId}")
    public ResponseEntity<List<ExtractedTableResponse>> getExtractedTables(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId) {

        log.info("Received request for extracted tables for paper ID: {}", paperId);

        List<ExtractedTableResponse> tables = extractionService.getExtractedTables(paperId);

        log.info("Retrieved {} tables for paper ID: {}", tables.size(), paperId);

        return ResponseEntity.ok(tables);
    }
}
