package org.solace.scholar_ai.project_service.service.citation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckRequestDto;
import org.solace.scholar_ai.project_service.dto.citation.CitationCheckResponseDto;
import org.solace.scholar_ai.project_service.dto.citation.CitationSummaryDto;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;
import org.solace.scholar_ai.project_service.model.citation.CitationCheck;
import org.solace.scholar_ai.project_service.model.citation.CitationEvidence;
import org.solace.scholar_ai.project_service.model.citation.CitationIssue;
import org.solace.scholar_ai.project_service.repository.citation.CitationCheckRepository;
import org.solace.scholar_ai.project_service.repository.citation.CitationEvidenceRepository;
import org.solace.scholar_ai.project_service.repository.citation.CitationIssueRepository;
import org.solace.scholar_ai.project_service.service.paper.PaperPersistenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CitationCheckService {

    private static final Logger logger = LoggerFactory.getLogger(CitationCheckService.class);

    // Listener interface for SSE streaming
    public interface CitationJobListener {
        void onStatus(UUID jobId, String status, String step, int progressPct);

        void onIssue(UUID jobId, CitationCheckResponseDto.CitationIssueDto issue);

        void onSummary(UUID jobId, CitationSummaryDto summary);

        void onError(UUID jobId, String error);

        void onComplete(UUID jobId);
    }

    // Map to store job listeners for SSE streaming
    private final Map<UUID, CitationJobListener> jobListeners = new ConcurrentHashMap<>();

    @Autowired
    private CitationCheckRepository citationCheckRepository;

    @Autowired
    private CitationIssueRepository citationIssueRepository;

    @Autowired
    private CitationEvidenceRepository citationEvidenceRepository;

    @Autowired
    private CitationAnalysisService citationAnalysisService;

    @Autowired
    private PaperPersistenceService paperPersistenceService;

    /**
     * Register a job listener for SSE streaming
     */
    public void registerJobListener(UUID jobId, CitationJobListener listener) {
        jobListeners.put(jobId, listener);
        logger.debug("Registered listener for job {}", jobId);
    }

    /**
     * Unregister a job listener
     */
    public void unregisterJobListener(UUID jobId) {
        jobListeners.remove(jobId);
        logger.debug("Unregistered listener for job {}", jobId);
    }

    /**
     * Notify listeners about status updates
     */
    private void notifyStatus(UUID jobId, String status, String step, int progressPct) {
        CitationJobListener listener = jobListeners.get(jobId);
        if (listener != null) {
            try {
                listener.onStatus(jobId, status, step, progressPct);
            } catch (Exception e) {
                logger.warn("Error notifying listener for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Notify listeners about new issues
     */
    private void notifyIssue(UUID jobId, CitationCheckResponseDto.CitationIssueDto issue) {
        CitationJobListener listener = jobListeners.get(jobId);
        if (listener != null) {
            try {
                listener.onIssue(jobId, issue);
            } catch (Exception e) {
                logger.warn("Error notifying listener for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Notify listeners about summary
     */
    private void notifySummary(UUID jobId, CitationSummaryDto summary) {
        CitationJobListener listener = jobListeners.get(jobId);
        if (listener != null) {
            try {
                listener.onSummary(jobId, summary);
            } catch (Exception e) {
                logger.warn("Error notifying listener for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Notify listeners about errors
     */
    private void notifyError(UUID jobId, String error) {
        CitationJobListener listener = jobListeners.get(jobId);
        if (listener != null) {
            try {
                listener.onError(jobId, error);
            } catch (Exception e) {
                logger.warn("Error notifying listener for job {}: {}", jobId, e.getMessage());
            }
        }
    }

    /**
     * Notify listeners about completion
     */
    private void notifyComplete(UUID jobId) {
        CitationJobListener listener = jobListeners.get(jobId);
        if (listener != null) {
            try {
                listener.onComplete(jobId);
            } catch (Exception e) {
                logger.warn("Error notifying listener for job {}: {}", jobId, e.getMessage());
            }
        }
        // Auto-unregister after completion
        unregisterJobListener(jobId);
    }

    /**
     * Start a new citation check job
     */
    public CitationCheckResponseDto startCitationCheck(CitationCheckRequestDto request) {
        logger.info(
                "Starting citation check for document {} in project {}",
                request.getDocumentId(),
                request.getProjectId());

        // Calculate content hash for reuse detection
        String contentHash = calculateContentHash(request.getContent());
        logger.info("Content hash for citation check: {}", contentHash);

        // Check if recent check exists with same content hash (unless force recheck)
        if (!Boolean.TRUE.equals(request.getForceRecheck())) {
            Optional<CitationCheck> existing = citationCheckRepository.findLatestCompletedByDocumentIdAndContentHash(
                    request.getDocumentId(), contentHash);

            if (existing.isPresent()) {
                CitationCheck existingCheck = existing.get();
                logger.info(
                        "Found existing citation check {} with matching content hash, returning cached result",
                        existingCheck.getId());
                return convertToResponseDto(existingCheck);
            }

            // Also check for recent checks without content hash (legacy)
            Optional<CitationCheck> recentCheck =
                    citationCheckRepository.findLatestCompletedByDocumentId(request.getDocumentId());
            if (recentCheck.isPresent()) {
                CitationCheck existingCheck = recentCheck.get();
                // If completed within last hour, return existing result
                if (existingCheck.isCompleted()
                        && existingCheck
                                .getUpdatedAt()
                                .isAfter(LocalDateTime.now().minusHours(1))) {
                    logger.info("Found recent citation check {}, returning existing result", existingCheck.getId());
                    return convertToResponseDto(existingCheck);
                }
            }
        }

        // If force recheck or no cached check, delete any existing DONE checks for this document
        // to avoid unique constraint violation
        citationCheckRepository.deleteCompletedByDocumentId(request.getDocumentId());

        // Create new citation check job
        CitationCheck citationCheck = CitationCheck.builder()
                .projectId(request.getProjectId())
                .documentId(request.getDocumentId())
                .texFileName(request.getFilename() != null ? request.getFilename() : "document.tex")
                .contentHash(contentHash) // Store content hash
                .status(CitationCheck.Status.QUEUED)
                .step(CitationCheck.Step.PARSING)
                .progressPct(0)
                .build();

        citationCheck = citationCheckRepository.save(citationCheck);

        // Start async processing
        processCitationCheckAsync(citationCheck.getId(), request);

        return convertToResponseDto(citationCheck);
    }

    /**
     * Get citation check by ID
     */
    @Transactional(readOnly = true)
    public Optional<CitationCheckResponseDto> getCitationCheck(UUID id) {
        return citationCheckRepository.findByIdWithIssues(id).map(this::convertToResponseDto);
    }

    /**
     * Get latest citation check for document
     */
    @Transactional(readOnly = true)
    public Optional<CitationCheckResponseDto> getLatestCitationCheck(UUID documentId) {
        return citationCheckRepository
                .findLatestByDocumentIdWithIssues(documentId)
                .map(this::convertToResponseDto);
    }

    /**
     * Get all citation checks for a project
     */
    @Transactional(readOnly = true)
    public List<CitationCheckResponseDto> getCitationChecksByProject(UUID projectId) {
        return citationCheckRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::convertToResponseDto)
                .toList();
    }

    /**
     * Mark citation issue as resolved
     */
    public void markIssueResolved(UUID issueId, boolean resolved) {
        Optional<CitationIssue> issueOpt = citationIssueRepository.findById(issueId);
        if (issueOpt.isPresent()) {
            CitationIssue issue = issueOpt.get();
            issue.setResolved(resolved);
            citationIssueRepository.save(issue);
            logger.info("Marked citation issue {} as resolved: {}", issueId, resolved);
        }
    }

    /**
     * Cancel a running citation check
     */
    public void cancelCitationCheck(UUID id) {
        Optional<CitationCheck> checkOpt = citationCheckRepository.findById(id);
        if (checkOpt.isPresent()) {
            CitationCheck check = checkOpt.get();
            if (check.getStatus() == CitationCheck.Status.QUEUED || check.getStatus() == CitationCheck.Status.RUNNING) {
                check.setStatus(CitationCheck.Status.ERROR);
                check.setErrorMessage("Citation check cancelled by user");
                citationCheckRepository.save(check);
                logger.info("Cancelled citation check {}", id);
            }
        }
    }

    /**
     * Process citation check asynchronously
     */
    @org.springframework.scheduling.annotation.Async
    public CompletableFuture<Void> processCitationCheckAsync(UUID citationCheckId, CitationCheckRequestDto request) {
        try {
            logger.info("Starting async processing for citation check {}", citationCheckId);

            Optional<CitationCheck> checkOpt = citationCheckRepository.findById(citationCheckId);
            if (checkOpt.isEmpty()) {
                logger.error("Citation check {} not found", citationCheckId);
                return CompletableFuture.completedFuture(null);
            }

            CitationCheck check = checkOpt.get();

            // Update status to running
            updateCheckProgress(check, CitationCheck.Status.RUNNING, CitationCheck.Step.PARSING, 10);

            // Parse LaTeX content and extract citations using real AI analysis
            updateCheckProgress(check, CitationCheck.Status.RUNNING, CitationCheck.Step.LOCAL_RETRIEVAL, 30);

            // Use selectedPaperIds from request if provided, otherwise use LaTeX context papers
            List<String> selectedPaperIds;
            if (request.getSelectedPaperIds() != null
                    && !request.getSelectedPaperIds().isEmpty()) {
                selectedPaperIds = request.getSelectedPaperIds().stream()
                        .map(UUID::toString)
                        .toList();
                logger.info(
                        "Using {} selectedPaperIds from request for citation check {}",
                        selectedPaperIds.size(),
                        citationCheckId);
            } else {
                // Fallback to papers marked for LaTeX context from the project
                List<PaperMetadataDto> latexContextPapers =
                        paperPersistenceService.findLatexContextPapersByProjectId(check.getProjectId());
                selectedPaperIds = latexContextPapers.stream()
                        .map(paper -> paper.id().toString())
                        .toList();
                logger.info(
                        "Using {} LaTeX context papers for project {} in citation check {}",
                        selectedPaperIds.size(),
                        check.getProjectId(),
                        citationCheckId);
            }

            boolean enableWebSearch = request.getOptions() != null
                    && Boolean.TRUE.equals(request.getOptions().getCheckWeb());

            // Pass the configurable options to the analysis service
            List<CitationIssue> issues = citationAnalysisService.analyzeDocument(
                    check,
                    request.getContent(),
                    selectedPaperIds,
                    enableWebSearch,
                    request.getOptions() // Pass options for configurable thresholds
                    );

            updateCheckProgress(check, CitationCheck.Status.RUNNING, CitationCheck.Step.WEB_RETRIEVAL, 60);

            // TODO: Enhance with web verification if enabled
            if (Boolean.TRUE.equals(
                    request.getOptions() != null && request.getOptions().getCheckWeb())) {
                // Enhanced with web verification
                logger.info("Web verification enabled for citation check {}", citationCheckId);
            }

            updateCheckProgress(check, CitationCheck.Status.RUNNING, CitationCheck.Step.SAVING, 80);

            // Finalize results
            finalizeCheck(check, issues);

            logger.info("Completed citation check {} with {} issues", citationCheckId, issues.size());

        } catch (Exception e) {
            logger.error("Error processing citation check " + citationCheckId, e);
            Optional<CitationCheck> checkOpt = citationCheckRepository.findById(citationCheckId);
            if (checkOpt.isPresent()) {
                CitationCheck check = checkOpt.get();
                check.setStatus(CitationCheck.Status.ERROR);
                check.setErrorMessage("Error: " + e.getMessage());
                citationCheckRepository.save(check);

                // Notify listeners about error
                notifyError(citationCheckId, e.getMessage());
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private void updateCheckProgress(
            CitationCheck check, CitationCheck.Status status, CitationCheck.Step step, int progressPercent) {
        check.setStatus(status);
        check.setStep(step);
        check.setProgressPct(progressPercent);
        citationCheckRepository.save(check);

        // Notify listeners about progress
        notifyStatus(check.getId(), status.toString(), step.toString(), progressPercent);
    }

    private void finalizeCheck(CitationCheck check, List<CitationIssue> issues) {
        // Save all issues
        for (CitationIssue issue : issues) {
            issue.setCitationCheck(check);
        }
        citationIssueRepository.saveAll(issues);

        // Notify listeners about each issue
        for (CitationIssue issue : issues) {
            CitationCheckResponseDto.CitationIssueDto issueDto = convertIssueToDto(issue);
            notifyIssue(check.getId(), issueDto);
        }

        // Update check status
        check.setStatus(CitationCheck.Status.DONE);
        check.setStep(CitationCheck.Step.DONE);
        check.setProgressPct(100);

        // Create summary JSON
        Map<String, Object> summaryMap = new HashMap<>();
        summaryMap.put("totalIssues", issues.size());
        summaryMap.put(
                "errorCount",
                issues.stream()
                        .mapToInt(i -> i.getSeverity() == CitationIssue.Severity.HIGH ? 1 : 0)
                        .sum());
        summaryMap.put(
                "warningCount",
                issues.stream()
                        .mapToInt(i -> i.getSeverity() == CitationIssue.Severity.MEDIUM ? 1 : 0)
                        .sum());
        summaryMap.put(
                "infoCount",
                issues.stream()
                        .mapToInt(i -> i.getSeverity() == CitationIssue.Severity.LOW ? 1 : 0)
                        .sum());
        summaryMap.put("completedAt", LocalDateTime.now().toString());

        check.setSummary(summaryMap);
        citationCheckRepository.save(check);

        // Notify listeners about summary and completion
        CitationSummaryDto summaryDto = convertSummaryToDto(summaryMap, check);
        notifySummary(check.getId(), summaryDto);
        notifyComplete(check.getId());
    }

    private List<String> extractCitations(String content) {
        // Simple regex-based citation extraction
        // In a real implementation, this would use a proper LaTeX parser
        List<String> citations = new ArrayList<>();
        // TODO: Implement proper citation extraction
        return citations;
    }

    private List<CitationIssue> createMockIssues(CitationCheck check, String content) {
        // Create some mock issues for testing
        List<CitationIssue> issues = new ArrayList<>();

        // First issue: "For a European agricultural case study, see" - Line 45 (based on actual CodeMirror position)
        CitationIssue mockIssue1 = CitationIssue.builder()
                .citationCheck(check)
                .projectId(check.getProjectId())
                .documentId(check.getDocumentId())
                .type(CitationIssue.IssueType.MISSING_CITATION)
                .severity(CitationIssue.Severity.LOW)
                .fromPos(1909) // Position in document for line 45
                .toPos(1952) // End position
                .lineStart(45) // Actual CodeMirror line number
                .lineEnd(45)
                .snippet("For a European agricultural case study, see")
                .citedKeys(new String[] {}) // Add empty array for cited keys
                .suggestions(new ArrayList<>()) // Add empty list for suggestions
                .build();

        // Second issue: "Challenges and Opportunities of Large Transnational Datasets" - Line 65 (based on actual
        // CodeMirror position)
        CitationIssue mockIssue2 = CitationIssue.builder()
                .citationCheck(check)
                .projectId(check.getProjectId())
                .documentId(check.getDocumentId())
                .type(CitationIssue.IssueType.MISSING_CITATION)
                .severity(CitationIssue.Severity.LOW)
                .fromPos(2494) // Position in document for line 65
                .toPos(2605) // End position
                .lineStart(65) // Actual CodeMirror line number
                .lineEnd(65)
                .snippet(
                        "Challenges and Opportunities of Large Transnational Datasets: A Case Study on European Administrative Crop Data")
                .citedKeys(new String[] {}) // Add empty array for cited keys
                .suggestions(new ArrayList<>()) // Add empty list for suggestions
                .build();

        issues.add(mockIssue1);
        issues.add(mockIssue2);

        // Log the created issues to verify line numbers
        logger.info("Created mock issues:");
        for (CitationIssue issue : issues) {
            logger.info(
                    "Issue ID: {}, Line: {}, Snippet: '{}'", issue.getId(), issue.getLineStart(), issue.getSnippet());
        }

        return issues;
    }

    private CitationCheckResponseDto convertToResponseDto(CitationCheck check) {
        CitationCheckResponseDto.CitationCheckResponseDtoBuilder builder = CitationCheckResponseDto.builder()
                .id(check.getId())
                .projectId(check.getProjectId())
                .documentId(check.getDocumentId())
                .status(check.getStatus().toString())
                .currentStep(check.getStep().toString())
                .progressPercent(check.getProgressPct())
                .message(check.getErrorMessage())
                .createdAt(check.getCreatedAt())
                .updatedAt(check.getUpdatedAt())
                .completedAt(check.isCompleted() ? check.getUpdatedAt() : null);

        // Parse summary JSON
        if (check.getSummary() != null) {
            CitationSummaryDto summaryDto = convertSummaryToDto(check.getSummary(), check);
            builder.summary(summaryDto);
        }

        // Convert issues if available
        if (check.getIssues() != null && !check.getIssues().isEmpty()) {
            List<CitationCheckResponseDto.CitationIssueDto> issueDtos =
                    check.getIssues().stream().map(this::convertIssueToDto).toList();
            builder.issues(issueDtos);
        }

        return builder.build();
    }

    private CitationCheckResponseDto.CitationIssueDto convertIssueToDto(CitationIssue issue) {
        logger.info(
                "Converting issue to DTO: ID={}, LineStart={}, LineEnd={}, Snippet='{}'",
                issue.getId(),
                issue.getLineStart(),
                issue.getLineEnd(),
                issue.getSnippet());

        CitationCheckResponseDto.CitationIssueDto.CitationIssueDtoBuilder builder =
                CitationCheckResponseDto.CitationIssueDto.builder()
                        .id(issue.getId())
                        .projectId(issue.getProjectId())
                        .documentId(issue.getDocumentId())
                        .issueType(issue.getType().getValue())
                        .severity(issue.getSeverity().toString())
                        .citationText(issue.getSnippet())
                        .position(issue.getFromPos())
                        .length(issue.getToPos() - issue.getFromPos())
                        .lineStart(issue.getLineStart())
                        .lineEnd(issue.getLineEnd())
                        .message("Citation issue detected") // TODO: Add proper message field
                        .citedKeys(issue.getCitedKeys() != null ? List.of(issue.getCitedKeys()) : List.of())
                        .suggestions(List.of()) // Empty suggestions for now
                        .resolved(issue.getResolved()); // Use the actual resolved field

        CitationCheckResponseDto.CitationIssueDto dto = builder.build();

        logger.info(
                "Created DTO: LineStart={}, LineEnd={}, CitationText='{}'",
                dto.getLineStart(),
                dto.getLineEnd(),
                dto.getCitationText());

        // Convert evidence if available
        if (issue.getEvidence() != null && !issue.getEvidence().isEmpty()) {
            List<CitationCheckResponseDto.CitationIssueDto.EvidenceDto> evidenceDtos =
                    issue.getEvidence().stream().map(this::convertEvidenceToDto).toList();
            builder.evidence(evidenceDtos);
        }

        return dto;
    }

    private CitationCheckResponseDto.CitationIssueDto.EvidenceDto convertEvidenceToDto(CitationEvidence evidence) {
        return CitationCheckResponseDto.CitationIssueDto.EvidenceDto.builder()
                .id(evidence.getId())
                .source(evidence.getSource()) // Now already a Map<String, Object>
                .matchedText(evidence.getMatchedText())
                .similarity(evidence.getSimilarity())
                .supportScore(evidence.getSupportScore())
                .extractedContext(
                        evidence.getExtra() != null
                                ? evidence.getExtra().toString()
                                : null) // Convert Map to String for DTO
                .build();
    }

    private CitationSummaryDto convertSummaryToDto(Map<String, Object> summaryMap, CitationCheck check) {
        if (summaryMap == null) {
            return null;
        }

        int totalIssues = summaryMap.get("totalIssues") != null ? (Integer) summaryMap.get("totalIssues") : 0;
        int errorCount = summaryMap.get("errorCount") != null ? (Integer) summaryMap.get("errorCount") : 0;
        int warningCount = summaryMap.get("warningCount") != null ? (Integer) summaryMap.get("warningCount") : 0;
        int infoCount = summaryMap.get("infoCount") != null ? (Integer) summaryMap.get("infoCount") : 0;

        Map<String, Integer> byType = new HashMap<>();
        byType.put("error", errorCount);
        byType.put("warning", warningCount);
        byType.put("info", infoCount);

        return CitationSummaryDto.builder()
                .total(totalIssues)
                .byType(byType)
                .contentHash(check.getContentHash()) // Use actual content hash from check
                .startedAt(check.getCreatedAt()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toInstant())
                .finishedAt(
                        check.isCompleted()
                                ? check.getUpdatedAt()
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toInstant()
                                : null)
                .build();
    }

    /**
     * Calculate SHA-256 hash of content for caching
     */
    private String calculateContentHash(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            return null;
        }
    }
}
