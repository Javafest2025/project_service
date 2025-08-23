package org.solace.scholar_ai.project_service.controller.library;

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
import org.solace.scholar_ai.project_service.dto.library.LibraryRequestDto;
import org.solace.scholar_ai.project_service.dto.library.LibraryResponseDto;
import org.solace.scholar_ai.project_service.dto.library.upload.UploadedPaperRequest;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.service.library.LibraryService;
import org.solace.scholar_ai.project_service.service.library.UploadedPaperService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/library")
@Tag(name = "📚 Library Management", description = "Manage project libraries and papers")
public class LibraryController {

    private final LibraryService libraryService;
    private final UploadedPaperService uploadedPaperService;

    @PostMapping("/project/{projectId}")
    @Operation(
            summary = "📚 Get Project Library",
            description = "Retrieve all papers for a specific project. This endpoint fetches all papers "
                    + "from completed web search operations associated with the project. "
                    + "Returns comprehensive paper metadata including titles, authors, abstracts, "
                    + "citation counts, and PDF URLs when available.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Project library retrieved successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Project not found or no papers available")
            })
    public ResponseEntity<APIResponse<LibraryResponseDto>> getProjectLibrary(
            @PathVariable
                    @Parameter(
                            description = "Project ID to retrieve library for",
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    UUID projectId,
            @Valid @RequestBody LibraryRequestDto request) {
        try {
            log.info("Get project library {} endpoint hit by user: {}", projectId, request.userId());

            LibraryResponseDto library = libraryService.getProjectLibrary(projectId, request.userId());

            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Project library retrieved successfully", library));
        } catch (RuntimeException e) {
            log.error("Error retrieving library for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving library for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to retrieve project library", null));
        }
    }

    @PostMapping("/project/{projectId}/papers")
    @Operation(
            summary = "📄 Upload Paper to Project Library",
            description = "Upload a paper to a project's library. This endpoint accepts paper metadata "
                    + "including title, authors, abstract, and PDF URLs. The paper will be saved to the "
                    + "database with the same structure as papers from web searches.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Paper uploaded successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "404", description = "Project not found or access denied")
            })
    public ResponseEntity<APIResponse<PaperMetadataDto>> uploadPaper(
            @PathVariable
                    @Parameter(
                            description = "Project ID to upload paper to",
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    UUID projectId,
            @Valid @RequestBody @Parameter(description = "Paper metadata and PDF information")
                    UploadedPaperRequest request) {
        try {
            log.info("Upload paper to project {} endpoint hit", projectId);

            // Extract userId from the request body - we'll add it to UploadedPaperRequest
            PaperMetadataDto savedPaper = uploadedPaperService.saveUploadedPaper(request, request.userId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(APIResponse.success(HttpStatus.CREATED.value(), "Paper uploaded successfully", savedPaper));
        } catch (RuntimeException e) {
            log.error("Error uploading paper to project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error uploading paper to project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to upload paper", null));
        }
    }

    @PostMapping("/project/{projectId}/latest")
    @Operation(
            summary = "📄 Get Latest Papers from Project",
            description = "Retrieve all papers from the most recent search operation (latest correlation ID) "
                    + "for a specific project. This endpoint returns papers from the most recent web search "
                    + "or uploaded paper operation, ordered by submission date.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Latest papers retrieved successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Project not found or no papers available")
            })
    public ResponseEntity<APIResponse<List<PaperMetadataDto>>> getLatestProjectPapers(
            @PathVariable
                    @Parameter(
                            description = "Project ID to retrieve latest papers for",
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    UUID projectId,
            @Valid @RequestBody LibraryRequestDto request) {
        try {
            log.info("Get latest project papers {} endpoint hit by user: {}", projectId, request.userId());

            List<PaperMetadataDto> latestPapers = libraryService.getLatestProjectPapers(projectId, request.userId());

            String message = String.format(
                    "Retrieved %d papers from the latest search operation for project %s",
                    latestPapers.size(), projectId);

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), message, latestPapers));
        } catch (RuntimeException e) {
            log.error("Error retrieving latest papers for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving latest papers for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to retrieve latest project papers",
                            null));
        }
    }

    @PostMapping("/project/{projectId}/stats")
    @Operation(
            summary = "📊 Get Project Library Statistics",
            description = "Get statistical overview of a project's library including paper count, "
                    + "search operations status, and correlation IDs summary.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Library statistics retrieved successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Project not found")
            })
    public ResponseEntity<APIResponse<LibraryResponseDto>> getProjectLibraryStats(
            @PathVariable
                    @Parameter(
                            description = "Project ID to retrieve library statistics for",
                            example = "123e4567-e89b-12d3-a456-426614174000")
                    UUID projectId,
            @Valid @RequestBody LibraryRequestDto request) {
        try {
            log.info("Get project library stats {} endpoint hit by user: {}", projectId, request.userId());

            LibraryResponseDto library = libraryService.getProjectLibrary(projectId, request.userId());

            // Remove papers from response to only show statistics
            LibraryResponseDto statsOnly = new LibraryResponseDto(
                    library.projectId(),
                    library.correlationIds(),
                    library.totalPapers(),
                    library.completedSearchOperations(),
                    library.retrievedAt(),
                    library.message(),
                    List.of()); // Empty papers list for stats endpoint

            String message = String.format(
                    "Project has %d papers from %d completed search operations",
                    library.totalPapers(), library.completedSearchOperations());

            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), message, statsOnly));
        } catch (RuntimeException e) {
            log.error("Error retrieving library statistics for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error retrieving library statistics for project {}: {}", projectId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to retrieve project library statistics",
                            null));
        }
    }
}
