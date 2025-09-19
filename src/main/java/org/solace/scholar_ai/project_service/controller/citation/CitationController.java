package org.solace.scholar_ai.project_service.controller.citation;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckRequestDto;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckResponseDto;
import org.solace.scholar_ai.project_service.dto.citation.CitationSummaryDto;
import org.solace.scholar_ai.project_service.service.citation.CitationCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/citations")
public class CitationController {

    private static final Logger logger = LoggerFactory.getLogger(CitationController.class);
    private static final ScheduledExecutorService keepAliveExecutor = Executors.newScheduledThreadPool(4);

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
     * SSE streaming endpoint for real-time citation job updates
     */
    @GetMapping(path = "/jobs/{jobId}/events", produces = "text/event-stream")
    public SseEmitter streamCitationJob(@PathVariable UUID jobId, HttpServletResponse response) {
        try {
            logger.info("Starting SSE stream for citation job {}", jobId);

            // Set headers to prevent proxy buffering
            response.setHeader("Cache-Control", "no-cache, no-transform");
            response.setHeader("X-Accel-Buffering", "no");
            response.setContentType("text/event-stream");
            response.setCharacterEncoding("UTF-8");

            SseEmitter emitter = new SseEmitter(0L); // No timeout

            // Check if job is already completed
            Optional<CitationCheckResponseDto> existingJob = citationCheckService.getCitationCheck(jobId);
            if (existingJob.isPresent() && "DONE".equals(existingJob.get().getStatus())) {
                logger.info("Job {} is already completed, sending final status immediately", jobId);

                // Send initial status
                send(
                        emitter,
                        "status",
                        Map.of(
                                "status", existingJob.get().getStatus(),
                                "step", existingJob.get().getCurrentStep(),
                                "progressPct", existingJob.get().getProgressPercent()));

                // Send summary if available
                if (existingJob.get().getSummary() != null) {
                    send(emitter, "summary", Map.of("summary", existingJob.get().getSummary()));
                }

                // Send completion event and close immediately
                send(
                        emitter,
                        "complete",
                        Map.of(
                                "status", "DONE",
                                "step", "Citation check completed",
                                "progressPct", 100));

                emitter.complete();
                return emitter;
            }

            // Send immediate initial status event for active jobs
            send(
                    emitter,
                    "status",
                    Map.of(
                            "status", "STARTED",
                            "step", "Initializing citation check...",
                            "progressPct", 0));

            // Setup keep-alive to prevent proxy timeouts
            final Runnable keepAlive = () -> {
                try {
                    emitter.send(SseEmitter.event().comment("keep-alive"));
                } catch (Exception e) {
                    logger.debug("Keep-alive failed for job {}, connection likely closed", jobId);
                }
            };
            final var keepAliveTask = keepAliveExecutor.scheduleWithFixedDelay(keepAlive, 20, 20, TimeUnit.SECONDS);

            citationCheckService.registerJobListener(jobId, new CitationCheckService.CitationJobListener() {
                @Override
                public void onStatus(UUID jobId, String status, String step, int progressPct) {
                    send(
                            emitter,
                            "status",
                            Map.of(
                                    "status", status,
                                    "step", step,
                                    "progressPct", progressPct));
                }

                @Override
                public void onIssue(UUID jobId, CitationCheckResponseDto.CitationIssueDto issue) {
                    send(emitter, "issue", Map.of("issue", issue));
                }

                @Override
                public void onSummary(UUID jobId, CitationSummaryDto summary) {
                    send(emitter, "summary", Map.of("summary", summary));
                }

                @Override
                public void onError(UUID jobId, String message) {
                    send(emitter, "error", Map.of("message", message));
                    keepAliveTask.cancel(false);
                    emitter.completeWithError(new RuntimeException(message));
                }

                @Override
                public void onComplete(UUID jobId) {
                    // Send final completion event before closing connection
                    send(
                            emitter,
                            "complete",
                            Map.of(
                                    "status", "DONE",
                                    "step", "Citation check completed",
                                    "progressPct", 100));
                    keepAliveTask.cancel(false);
                    emitter.complete();
                }
            });

            emitter.onCompletion(() -> {
                keepAliveTask.cancel(false);
                citationCheckService.unregisterJobListener(jobId);
            });
            emitter.onTimeout(() -> {
                keepAliveTask.cancel(false);
                citationCheckService.unregisterJobListener(jobId);
            });
            emitter.onError((throwable) -> {
                keepAliveTask.cancel(false);
                citationCheckService.unregisterJobListener(jobId);
            });

            return emitter;
        } catch (Exception e) {
            logger.error("Error starting SSE stream for job " + jobId, e);
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(e);
            return emitter;
        }
    }

    /**
     * Cancel a running citation check
     */
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Void> cancelCitationCheck(@PathVariable UUID jobId) {
        try {
            citationCheckService.cancelCitationCheck(jobId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error cancelling citation check " + jobId, e);
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
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Citation service is running");
    }

    /**
     * Helper method to send SSE events safely with proper message format
     */
    private void send(SseEmitter emitter, String type, Object data) {
        try {
            Map<String, Object> message = Map.of("type", type, "data", data);
            emitter.send(SseEmitter.event().name("message").data(message));
        } catch (Exception e) {
            logger.warn("Failed to send SSE event of type {}", type, e);
            emitter.completeWithError(e);
        }
    }

    /**
     * Legacy helper method for backwards compatibility
     */
    private void sendEvent(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event().name("message").data(data));
        } catch (Exception e) {
            logger.warn("Failed to send SSE event", e);
            emitter.completeWithError(e);
        }
    }
}
