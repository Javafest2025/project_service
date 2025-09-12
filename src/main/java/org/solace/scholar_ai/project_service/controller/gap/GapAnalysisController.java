package org.solace.scholar_ai.project_service.controller.gap;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisRequest;
import org.solace.scholar_ai.project_service.dto.response.gap.GapAnalysisResponse;
import org.solace.scholar_ai.project_service.model.gap.GapAnalysis;
import org.solace.scholar_ai.project_service.service.gap.GapAnalysisService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for gap analysis operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gap-analyses")
@RequiredArgsConstructor
@Tag(name = "Gap Analysis", description = "API for managing research gap analysis")
public class GapAnalysisController {

    private final GapAnalysisService gapAnalysisService;

    /**
     * Initiate gap analysis for a paper.
     */
    @PostMapping
    @Operation(summary = "Initiate gap analysis", description = "Start gap analysis for a paper")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "201", description = "Gap analysis initiated successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid request"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "409", description = "Paper not extracted")
            })
    public ResponseEntity<GapAnalysisResponse> initiateGapAnalysis(@RequestBody GapAnalysisRequest request) {

        log.info("Received gap analysis request for paper: {}", request.getPaperId());

        GapAnalysisResponse response = gapAnalysisService.initiateGapAnalysis(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get gap analysis by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get gap analysis", description = "Retrieve gap analysis by ID")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Gap analysis found"),
                @ApiResponse(responseCode = "404", description = "Gap analysis not found")
            })
    public ResponseEntity<GapAnalysisResponse> getGapAnalysis(
            @Parameter(description = "Gap analysis ID") @PathVariable UUID id) {

        GapAnalysisResponse response = gapAnalysisService.getGapAnalysis(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get gap analyses by paper ID.
     */
    @GetMapping("/paper/{paperId}")
    @Operation(summary = "Get gap analyses by paper", description = "Retrieve all gap analyses for a paper")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Gap analyses found")})
    public ResponseEntity<List<GapAnalysisResponse>> getGapAnalysesByPaperId(
            @Parameter(description = "Paper ID") @PathVariable UUID paperId) {

        List<GapAnalysisResponse> responses = gapAnalysisService.getGapAnalysesByPaperId(paperId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get gap analyses by paper ID with pagination.
     */
    @GetMapping("/paper/{paperId}/paginated")
    @Operation(
            summary = "Get gap analyses by paper (paginated)",
            description = "Retrieve gap analyses for a paper with pagination")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Gap analyses found")})
    public ResponseEntity<Page<GapAnalysisResponse>> getGapAnalysesByPaperIdPaginated(
            @Parameter(description = "Paper ID") @PathVariable UUID paperId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<GapAnalysisResponse> responses = gapAnalysisService.getGapAnalysesByPaperId(paperId, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get gap analyses by status.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get gap analyses by status", description = "Retrieve gap analyses by status")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Gap analyses found")})
    public ResponseEntity<List<GapAnalysisResponse>> getGapAnalysesByStatus(
            @Parameter(description = "Gap analysis status") @PathVariable GapAnalysis.GapStatus status) {

        List<GapAnalysisResponse> responses = gapAnalysisService.getGapAnalysesByStatus(status);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get gap analyses by status with pagination.
     */
    @GetMapping("/status/{status}/paginated")
    @Operation(
            summary = "Get gap analyses by status (paginated)",
            description = "Retrieve gap analyses by status with pagination")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Gap analyses found")})
    public ResponseEntity<Page<GapAnalysisResponse>> getGapAnalysesByStatusPaginated(
            @Parameter(description = "Gap analysis status") @PathVariable GapAnalysis.GapStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<GapAnalysisResponse> responses = gapAnalysisService.getGapAnalysesByStatus(status, pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get the latest gap analysis for a paper.
     */
    @GetMapping("/paper/{paperId}/latest")
    @Operation(summary = "Get latest gap analysis", description = "Retrieve the most recent gap analysis for a paper")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Latest gap analysis found"),
                @ApiResponse(responseCode = "404", description = "No gap analysis found for paper")
            })
    public ResponseEntity<GapAnalysisResponse> getLatestGapAnalysisByPaperId(
            @Parameter(description = "Paper ID") @PathVariable UUID paperId) {

        GapAnalysisResponse response = gapAnalysisService.getLatestGapAnalysisByPaperId(paperId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retry failed gap analysis.
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry gap analysis", description = "Retry a failed gap analysis")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Gap analysis retry initiated"),
                @ApiResponse(responseCode = "400", description = "Invalid operation"),
                @ApiResponse(responseCode = "404", description = "Gap analysis not found")
            })
    public ResponseEntity<GapAnalysisResponse> retryGapAnalysis(
            @Parameter(description = "Gap analysis ID") @PathVariable UUID id) {

        log.info("Retrying gap analysis: {}", id);

        GapAnalysisResponse response = gapAnalysisService.retryGapAnalysis(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get gap analysis request data by paper ID.
     */
    @GetMapping("/paper/{paperId}/request-data")
    @Operation(
            summary = "Get gap analysis request data by paper",
            description = "Retrieve gap analysis request data and configuration for a paper")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Gap analysis request data found"),
                @ApiResponse(responseCode = "404", description = "No gap analysis found for paper")
            })
    public ResponseEntity<List<GapAnalysisService.GapAnalysisRequestData>> getGapAnalysisRequestDataByPaperId(
            @Parameter(description = "Paper ID") @PathVariable UUID paperId) {

        List<GapAnalysisService.GapAnalysisRequestData> requestData =
                gapAnalysisService.getGapAnalysisRequestDataByPaperId(paperId);
        return ResponseEntity.ok(requestData);
    }

    /**
     * Get gap analysis statistics.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get gap analysis statistics", description = "Retrieve gap analysis statistics")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Statistics retrieved")})
    public ResponseEntity<GapAnalysisService.GapAnalysisStats> getGapAnalysisStats() {
        GapAnalysisService.GapAnalysisStats stats = gapAnalysisService.getGapAnalysisStats();
        return ResponseEntity.ok(stats);
    }
}
