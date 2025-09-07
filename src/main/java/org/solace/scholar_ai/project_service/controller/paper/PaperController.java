package org.solace.scholar_ai.project_service.controller.paper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.service.paper.PaperService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing paper information
 */
@Slf4j
@RestController
@RequestMapping("/api/papers")
@RequiredArgsConstructor
@Tag(name = "Paper", description = "APIs for managing paper information")
public class PaperController {

    private final PaperService paperService;

    @Operation(
            summary = "Get structured facts for a paper",
            description = "Retrieves structured facts including title, authors, and basic information for a paper")
    @ApiResponses(
            value = {
                @ApiResponse(responseCode = "200", description = "Paper facts retrieved successfully"),
                @ApiResponse(responseCode = "404", description = "Paper not found"),
                @ApiResponse(responseCode = "500", description = "Internal server error")
            })
    @GetMapping("/{paperId}/structured-facts")
    public ResponseEntity<Map<String, Object>> getStructuredFacts(
            @Parameter(description = "Paper ID", required = true) @PathVariable String paperId) {

        log.info("📄 Getting structured facts for paper: {}", paperId);

        try {
            UUID paperUuid = UUID.fromString(paperId);
            Map<String, Object> structuredFacts = paperService.getStructuredFacts(paperUuid);

            log.info("✅ Retrieved structured facts for paper: {}", paperId);
            return ResponseEntity.ok(structuredFacts);

        } catch (IllegalArgumentException e) {
            log.warn("❌ Invalid paper ID format: {}", paperId);
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            log.error("❌ Error getting structured facts for paper {}: {}", paperId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
