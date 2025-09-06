package org.solace.scholar_ai.project_service.dto.response.extraction;

/**
 * Response DTO for extracted figures
 */
public record ExtractedFigureResponse(
        String figureId,
        String label,
        String caption,
        Integer page,
        String imagePath,
        String figureType,
        Integer orderIndex) {}
