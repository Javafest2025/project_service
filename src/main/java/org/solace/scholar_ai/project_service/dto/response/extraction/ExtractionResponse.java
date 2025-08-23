package org.solace.scholar_ai.project_service.dto.response.extraction;

import java.time.Instant;

/**
 * Response DTO for extraction operations
 */
public record ExtractionResponse(
        String jobId,
        String paperId,
        String status,
        String message,
        String b2Url,
        Instant startedAt,
        Instant completedAt,
        Double progress,
        String error) {}
