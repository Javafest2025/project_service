package org.solace.scholar_ai.project_service.dto.response.extraction;

/**
 * Response DTO for extracted tables
 */
public record ExtractedTableResponse(
        String tableId, String label, String caption, Integer page, String csvPath, Integer orderIndex) {}
