package org.solace.scholar_ai.project_service.controller.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.ai.AbstractAnalysisDto;
import org.solace.scholar_ai.project_service.dto.ai.AbstractAnalysisRequestDto;
import org.solace.scholar_ai.project_service.dto.ai.AbstractHighlightDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.service.ai.AbstractAnalysisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/ai/abstract")
@Tag(name = "🤖 AI Abstract Analysis", description = "AI-powered analysis of research paper abstracts")
public class AbstractAnalysisController {

    private final AbstractAnalysisService abstractAnalysisService;

    @PostMapping("/highlights")
    @Operation(
            summary = "🔍 Analyze Abstract Highlights",
            description =
                    "Analyze a research paper abstract and identify important keywords, numbers, and concepts that should be highlighted for emphasis.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Abstract highlights analyzed successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "500", description = "AI analysis failed")
            })
    public ResponseEntity<APIResponse<AbstractHighlightDto>> analyzeAbstractHighlights(
            @Valid @RequestBody @Parameter(description = "Abstract text to analyze")
                    AbstractAnalysisRequestDto request) {
        try {
            log.info(
                    "🔍 Analyzing abstract highlights for text length: {}",
                    request.getAbstractText().length());

            AbstractHighlightDto highlights =
                    abstractAnalysisService.analyzeAbstractHighlights(request.getAbstractText());

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), "Abstract highlights analyzed successfully", highlights));
        } catch (Exception e) {
            log.error("❌ Error analyzing abstract highlights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to analyze abstract highlights", null));
        }
    }

    @PostMapping("/insights")
    @Operation(
            summary = "🧠 Analyze Abstract Insights",
            description =
                    "Analyze a research paper abstract and extract key insights including focus, approach, emphasis, and other relevant information.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Abstract insights analyzed successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "500", description = "AI analysis failed")
            })
    public ResponseEntity<APIResponse<AbstractAnalysisDto>> analyzeAbstractInsights(
            @Valid @RequestBody @Parameter(description = "Abstract text to analyze")
                    AbstractAnalysisRequestDto request) {
        try {
            log.info(
                    "🧠 Analyzing abstract insights for text length: {}",
                    request.getAbstractText().length());

            AbstractAnalysisDto insights = abstractAnalysisService.analyzeAbstractInsights(request.getAbstractText());

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Abstract insights analyzed successfully", insights));
        } catch (Exception e) {
            log.error("❌ Error analyzing abstract insights: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to analyze abstract insights", null));
        }
    }

    @PostMapping("/analyze")
    @Operation(
            summary = "📊 Complete Abstract Analysis",
            description =
                    "Perform a complete analysis of a research paper abstract, including both highlights and insights in a single request.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Complete abstract analysis performed successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "500", description = "AI analysis failed")
            })
    public ResponseEntity<APIResponse<CompleteAnalysisResponse>> analyzeAbstract(
            @Valid @RequestBody @Parameter(description = "Abstract text to analyze")
                    AbstractAnalysisRequestDto request) {
        try {
            log.info(
                    "📊 Performing complete abstract analysis for text length: {}",
                    request.getAbstractText().length());

            AbstractHighlightDto highlights =
                    abstractAnalysisService.analyzeAbstractHighlights(request.getAbstractText());
            AbstractAnalysisDto insights = abstractAnalysisService.analyzeAbstractInsights(request.getAbstractText());

            CompleteAnalysisResponse completeAnalysis = new CompleteAnalysisResponse(highlights, insights);

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), "Complete abstract analysis performed successfully", completeAnalysis));
        } catch (Exception e) {
            log.error("❌ Error performing complete abstract analysis: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to perform complete abstract analysis",
                            null));
        }
    }

    @PostMapping("/paper/{paperId}/analyze")
    @Operation(
            summary = "📊 Analyze Paper Abstract",
            description =
                    "Get or create analysis for a specific paper's abstract. Returns cached results if available, otherwise performs new analysis.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Paper abstract analysis retrieved/created successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "500", description = "AI analysis failed")
            })
    public ResponseEntity<APIResponse<CompleteAnalysisResponse>> analyzePaperAbstract(
            @PathVariable @Parameter(description = "Paper ID") String paperId,
            @Valid @RequestBody @Parameter(description = "Abstract text to analyze")
                    AbstractAnalysisRequestDto request) {
        try {
            log.info("📊 Analyzing paper abstract for paper: {}", paperId);

            AbstractAnalysisDto insights =
                    abstractAnalysisService.getOrCreateAnalysis(paperId, request.getAbstractText());
            AbstractHighlightDto highlights = abstractAnalysisService.getHighlightsFromDb(paperId);

            CompleteAnalysisResponse completeAnalysis = new CompleteAnalysisResponse(highlights, insights);

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), "Paper abstract analysis completed successfully", completeAnalysis));
        } catch (Exception e) {
            log.error("❌ Error analyzing paper abstract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to analyze paper abstract", null));
        }
    }

    @PostMapping("/paper/{paperId}/reanalyze")
    @Operation(
            summary = "🔄 Re-analyze Paper Abstract",
            description = "Force re-analysis of a paper's abstract, updating the database with new results.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Paper abstract re-analyzed successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "500", description = "AI analysis failed")
            })
    public ResponseEntity<APIResponse<CompleteAnalysisResponse>> reanalyzePaperAbstract(
            @PathVariable @Parameter(description = "Paper ID") String paperId,
            @Valid @RequestBody @Parameter(description = "Abstract text to re-analyze")
                    AbstractAnalysisRequestDto request) {
        try {
            log.info("🔄 Re-analyzing paper abstract for paper: {}", paperId);

            AbstractAnalysisDto insights =
                    abstractAnalysisService.reanalyzeAbstract(paperId, request.getAbstractText());
            AbstractHighlightDto highlights = abstractAnalysisService.getHighlightsFromDb(paperId);

            CompleteAnalysisResponse completeAnalysis = new CompleteAnalysisResponse(highlights, insights);

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), "Paper abstract re-analyzed successfully", completeAnalysis));
        } catch (Exception e) {
            log.error("❌ Error re-analyzing paper abstract: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to re-analyze paper abstract", null));
        }
    }

    public static class CompleteAnalysisResponse {
        private final AbstractHighlightDto highlights;
        private final AbstractAnalysisDto insights;

        public CompleteAnalysisResponse(AbstractHighlightDto highlights, AbstractAnalysisDto insights) {
            this.highlights = highlights;
            this.insights = insights;
        }

        public AbstractHighlightDto getHighlights() {
            return highlights;
        }

        public AbstractAnalysisDto getInsights() {
            return insights;
        }
    }
}
