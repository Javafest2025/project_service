package org.solace.scholar_ai.project_service.controller.citation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckRequestDto;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckResponseDto;
import org.solace.scholar_ai.project_service.service.citation.CitationCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/citations")
public class CitationController {

    private static final Logger logger = LoggerFactory.getLogger(CitationController.class);

    @Autowired
    private CitationCheckService citationCheckService;

    /**
     * Start a new citation check (Frontend expects /jobs endpoint)
     */
    @PostMapping("/jobs")
    public ResponseEntity<CitationCheckResponseDto> startCitationCheck(@RequestBody CitationCheckRequestDto request) {
        try {
            logger.info("Starting citation check for document {}", request.getDocumentId());
            CitationCheckResponseDto response = citationCheckService.startCitationCheck(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error starting citation check", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get citation check by job ID (Frontend expects /jobs/{jobId})
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<CitationCheckResponseDto> getCitationJob(@PathVariable UUID jobId) {
        try {
            Optional<CitationCheckResponseDto> response = citationCheckService.getCitationCheck(jobId);
            return response.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting citation job " + jobId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get latest citation check for document (Frontend expects /documents/{documentId})
     */
    @GetMapping("/documents/{documentId}")
    public ResponseEntity<CitationCheckResponseDto> getLatestCitationCheck(@PathVariable UUID documentId) {
        try {
            Optional<CitationCheckResponseDto> response = citationCheckService.getLatestCitationCheck(documentId);
            return response.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error getting latest citation check for document " + documentId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all citation checks for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<CitationCheckResponseDto>> getCitationChecksByProject(@PathVariable UUID projectId) {
        try {
            List<CitationCheckResponseDto> response = citationCheckService.getCitationChecksByProject(projectId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error getting citation checks for project " + projectId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update citation issue (Frontend expects PUT /issues/{issueId})
     */
    @PutMapping("/issues/{issueId}")
    public ResponseEntity<Void> updateCitationIssue(
            @PathVariable UUID issueId, @RequestBody Map<String, Object> patch) {
        try {
            // Handle the UpdateCitationIssueRequest format from frontend
            if (patch.containsKey("resolved")) {
                boolean resolved = (Boolean) patch.get("resolved");
                citationCheckService.markIssueResolved(issueId, resolved);
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error updating citation issue " + issueId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancel a running citation check
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelCitationCheck(@PathVariable UUID id) {
        try {
            citationCheckService.cancelCitationCheck(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error cancelling citation check " + id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Citation service is running");
    }
}
