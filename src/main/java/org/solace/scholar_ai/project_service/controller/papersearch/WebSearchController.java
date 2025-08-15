package org.solace.scholar_ai.project_service.controller.papersearch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.papersearch.request.WebSearchRequestDto;
import org.solace.scholar_ai.project_service.dto.papersearch.response.WebSearchResponseDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.service.paper.search.WebSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/websearch")
@Tag(name = "Web Search", description = "Academic paper search endpoints using multiple sources")
@RequiredArgsConstructor
public class WebSearchController {

    private final WebSearchService webSearchService;

    @PostMapping
    @Operation(
            summary = "🔍 Search Academic Papers",
            description =
                    "Search for academic papers across multiple sources (Semantic Scholar, arXiv, Crossref, PubMed). "
                            + "Specify your search terms, academic domain, and how many papers you want to find. "
                            + "Returns full paper metadata including authors, abstracts, citations, and more.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Web search initiated successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request parameters")
            })
    public ResponseEntity<APIResponse<WebSearchResponseDto>> searchPapers(
            @Valid
                    @RequestBody
                    @Parameter(description = "Search parameters including query terms, domain, and batch size")
                    WebSearchRequestDto request) {
        try {
            log.info(
                    "Initiating web search with query terms: {}, domain: {}, batch size: {}",
                    request.queryTerms(),
                    request.domain(),
                    request.batchSize());

            WebSearchResponseDto response = webSearchService.initiateWebSearch(request);

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(),
                    "Web search initiated successfully. Use the correlation ID to retrieve results.",
                    response));
        } catch (RuntimeException e) {
            log.error("Error initiating web search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(
                            HttpStatus.BAD_REQUEST.value(), "Failed to initiate web search: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error initiating web search: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to initiate web search", null));
        }
    }

    @GetMapping("/{correlationId}")
    @Operation(
            summary = "📄 Get Search Results",
            description = "Retrieve the results of a web search using the correlation ID. "
                    + "Returns full paper metadata including titles, authors, abstracts, publication details, "
                    + "citation counts, and PDF URLs when available. If the search is still in progress, "
                    + "the papers list will be empty.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Search results retrieved successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Search not found")
            })
    public ResponseEntity<APIResponse<WebSearchResponseDto>> getSearchResults(
            @PathVariable
                    @Parameter(description = "Correlation ID from the initial search request", example = "corr-123-456")
                    String correlationId) {
        try {
            log.info("Retrieving search results for correlation ID: {}", correlationId);

            WebSearchResponseDto result = webSearchService.getSearchResults(correlationId);
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(APIResponse.error(
                                HttpStatus.NOT_FOUND.value(),
                                "Search not found for correlation ID: " + correlationId,
                                null));
            }

            String message = result.papers().isEmpty()
                    ? "Search in progress. Results will be available shortly."
                    : String.format(
                            "Search completed successfully! Found %d papers with full metadata.",
                            result.papers().size());

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), message, result));
        } catch (Exception e) {
            log.error("Error retrieving search results for correlation ID {}: {}", correlationId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve search results", null));
        }
    }

    @GetMapping
    @Operation(
            summary = "📚 Get All Search Results",
            description = "Retrieve all web search results with full paper metadata. "
                    + "Useful for seeing the history of searches and their current status. "
                    + "Each result includes complete paper information when available.")
    @ApiResponse(
            responseCode = "200",
            description = "All search results retrieved successfully",
            content = @Content(schema = @Schema(implementation = APIResponse.class)))
    public ResponseEntity<APIResponse<List<WebSearchResponseDto>>> getAllSearchResults() {
        try {
            log.info("Retrieving all search results");

            List<WebSearchResponseDto> results = webSearchService.getAllSearchResults();

            int totalPapers =
                    results.stream().mapToInt(result -> result.papers().size()).sum();

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(),
                    String.format("Retrieved %d search operations with %d total papers", results.size(), totalPapers),
                    results));
        } catch (Exception e) {
            log.error("Error retrieving all search results: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve search results", null));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "🔧 Health Check", description = "Check if the web search service is running and operational")
    @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(schema = @Schema(implementation = APIResponse.class)))
    public ResponseEntity<APIResponse<Map<String, String>>> health() {
        try {
            Map<String, String> healthInfo = Map.of(
                    "status", "UP",
                    "service", "Web Search Service",
                    "features", "Academic Paper Search (Semantic Scholar, arXiv, Crossref, PubMed)",
                    "version", "v1.0");

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), "Web search service is healthy and operational", healthInfo));
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(APIResponse.error(
                            HttpStatus.SERVICE_UNAVAILABLE.value(), "Service health check failed", null));
        }
    }
}
