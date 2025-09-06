package org.solace.scholar_ai.project_service.controller.latex;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.service.paper.PaperPersistenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/latex")
@RequiredArgsConstructor
@Tag(name = "📝 LaTeX Context Management", description = "Manage papers for LaTeX context in projects")
public class LatexContextController {

    private final PaperPersistenceService paperPersistenceService;

    @GetMapping("/projects/{projectId}/context-papers")
    @Operation(
            summary = "📑 Get LaTeX Context Papers",
            description = "Retrieve all papers that are marked as LaTeX context for a specific project. "
                    + "These papers will be automatically loaded in the LaTeX editor for the project.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "LaTeX context papers retrieved successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Project not found")
            })
    public ResponseEntity<APIResponse<List<PaperMetadataDto>>> getLatexContextPapers(
            @PathVariable
                    @Parameter(
                            description = "Project ID to retrieve LaTeX context papers for",
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    UUID projectId) {
        try {
            log.info("Retrieving LaTeX context papers for project: {}", projectId);

            List<PaperMetadataDto> contextPapers = paperPersistenceService.findLatexContextPapersByProjectId(projectId);

            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(),
                    String.format("Retrieved %d LaTeX context papers for project", contextPapers.size()),
                    contextPapers));
        } catch (Exception e) {
            log.error("Error retrieving LaTeX context papers for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to retrieve LaTeX context papers: " + e.getMessage(),
                            null));
        }
    }

    @PutMapping("/papers/{paperId}/context")
    @Operation(
            summary = "🔄 Toggle LaTeX Context Status",
            description = "Toggle whether a paper is included in LaTeX context for its project. "
                    + "Papers marked as LaTeX context will be automatically loaded in the LaTeX editor.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "LaTeX context status updated successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Paper not found")
            })
    public ResponseEntity<APIResponse<PaperMetadataDto>> toggleLatexContext(
            @PathVariable
                    @Parameter(
                            description = "Paper ID to toggle LaTeX context status",
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    UUID paperId,
            @Valid @RequestBody ToggleLatexContextRequest request) {
        try {
            log.info("Toggling LaTeX context status for paper {} to {}", paperId, request.isLatexContext());

            PaperMetadataDto updatedPaper =
                    paperPersistenceService.toggleLatexContext(paperId, request.isLatexContext());

            String action = request.isLatexContext() ? "added to" : "removed from";
            return ResponseEntity.ok(APIResponse.success(
                    HttpStatus.OK.value(), String.format("Paper successfully %s LaTeX context", action), updatedPaper));
        } catch (RuntimeException e) {
            log.error("Error toggling LaTeX context for paper {}: {}", paperId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error toggling LaTeX context for paper {}: {}", paperId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to update LaTeX context status: " + e.getMessage(),
                            null));
        }
    }

    /**
     * Request DTO for toggling LaTeX context status
     */
    @Schema(description = "Request to toggle LaTeX context status for a paper")
    public record ToggleLatexContextRequest(
            @Schema(description = "Whether to include this paper in LaTeX context", example = "true")
                    boolean isLatexContext) {}
}
