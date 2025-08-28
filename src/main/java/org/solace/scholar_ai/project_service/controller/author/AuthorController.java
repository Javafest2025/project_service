package org.solace.scholar_ai.project_service.controller.author;

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
import org.solace.scholar_ai.project_service.dto.author.AuthorDto;
import org.solace.scholar_ai.project_service.dto.author.AuthorSyncRequestDto;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.service.author.AuthorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/authors")
@Tag(name = "👨‍🔬 Author Management", description = "Multi-source author information with smart sync capabilities")
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping("/fetch/{name}")
    @Operation(
            summary = "📋 Fetch Author Information",
            description = "Fetch author information from database. If data is incomplete or missing, "
                    + "automatically triggers sync with paper-search service to get comprehensive data. "
                    + "This endpoint intelligently decides whether to return cached data or fetch fresh data.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Author information retrieved successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Author not found in any source"),
                @ApiResponse(responseCode = "503", description = "External sync service unavailable")
            })
    public ResponseEntity<APIResponse<AuthorDto>> fetchAuthor(
            @PathVariable @Parameter(description = "Author name", example = "Andrew Y. Ng") String name,
            @RequestParam(defaultValue = "fast")
                    @Parameter(description = "Search strategy if sync is needed", example = "comprehensive")
                    String strategy,
            @RequestParam(required = false) @Parameter(description = "User ID for tracking") String userId) {
        try {
            log.info("Fetching author: {} with strategy: {}", name, strategy);
            AuthorDto author = authorService.fetchAuthor(name, strategy, userId);

            String message = author.isSynced()
                    ? "Author retrieved from database"
                    : "Author synchronized and retrieved from external sources";

            return ResponseEntity.ok(APIResponse.success(200, message, author));
        } catch (Exception e) {
            log.error("Error fetching author: {}", name, e);
            return ResponseEntity.status(404)
                    .body(APIResponse.error(404, "Failed to fetch author: " + e.getMessage(), null));
        }
    }

    @PostMapping("/sync")
    @Operation(
            summary = "🔄 Sync Author from External Sources",
            description = "Force synchronization of author information from paper-search service. "
                    + "This endpoint always fetches fresh data from multiple academic APIs (Semantic Scholar, "
                    + "OpenAlex, ORCID, DBLP, Crossref) and updates the database. Use this when you need "
                    + "the most up-to-date information.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Author information synchronized successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "400", description = "Invalid request data"),
                @ApiResponse(responseCode = "503", description = "External service unavailable")
            })
    public ResponseEntity<APIResponse<AuthorDto>> syncAuthor(
            @Valid @RequestBody @Parameter(description = "Author sync request with name and strategy")
                    AuthorSyncRequestDto request) {
        try {
            log.info(
                    "Force syncing author: {} with strategy: {} for user: {}",
                    request.name(),
                    request.strategy(),
                    request.userId());
            AuthorDto author = authorService.syncAuthor(request);
            return ResponseEntity.ok(APIResponse.success(
                    200,
                    "Author information synchronized successfully from "
                            + author.dataSources().size() + " sources",
                    author));
        } catch (Exception e) {
            log.error("Error syncing author: {} for user: {}", request.name(), request.userId(), e);
            return ResponseEntity.status(503)
                    .body(APIResponse.error(503, "Failed to sync author: " + e.getMessage(), null));
        }
    }

    @PostMapping("/resync/{name}")
    @Operation(
            summary = "🔄 Resync Author Data",
            description = "Force resynchronization of author information from paper-search service. "
                    + "This endpoint always fetches fresh data from external sources and updates the database. "
                    + "Use this when you want to refresh cached author data.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Author information resynchronized successfully",
                        content = @Content(schema = @Schema(implementation = APIResponse.class))),
                @ApiResponse(responseCode = "404", description = "Author not found"),
                @ApiResponse(responseCode = "503", description = "External service unavailable")
            })
    public ResponseEntity<APIResponse<AuthorDto>> resyncAuthor(
            @PathVariable @Parameter(description = "Author name", example = "Andrew Y. Ng") String name,
            @RequestParam(defaultValue = "comprehensive")
                    @Parameter(description = "Search strategy", example = "comprehensive")
                    String strategy,
            @RequestParam(required = false) @Parameter(description = "User ID for tracking") String userId) {
        try {
            log.info("Resyncing author: {} with strategy: {}", name, strategy);

            // Create a sync request with force refresh
            AuthorSyncRequestDto syncRequest = new AuthorSyncRequestDto(userId, name, strategy, true);
            AuthorDto author = authorService.syncAuthor(syncRequest);

            return ResponseEntity.ok(APIResponse.success(
                    200,
                    "Author information resynchronized successfully from "
                            + author.dataSources().size() + " sources",
                    author));
        } catch (Exception e) {
            log.error("Error resyncing author: {}", name, e);
            return ResponseEntity.status(503)
                    .body(APIResponse.error(503, "Failed to resync author: " + e.getMessage(), null));
        }
    }
}
