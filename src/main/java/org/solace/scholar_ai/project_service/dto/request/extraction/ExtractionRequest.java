package org.solace.scholar_ai.project_service.dto.request.extraction;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for triggering paper extraction
 */
public record ExtractionRequest(
        @NotNull(message = "Paper ID is required") String paperId,

        // Extraction options
        Boolean extractText,
        Boolean extractFigures,
        Boolean extractTables,
        Boolean extractEquations,
        Boolean extractCode,
        Boolean extractReferences,
        Boolean useOcr,
        Boolean detectEntities,
        Boolean asyncProcessing) {
    // Default constructor with default values
    public ExtractionRequest {
        extractText = extractText != null ? extractText : true;
        extractFigures = extractFigures != null ? extractFigures : true;
        extractTables = extractTables != null ? extractTables : true;
        extractEquations = extractEquations != null ? extractEquations : true;
        extractCode = extractCode != null ? extractCode : true;
        extractReferences = extractReferences != null ? extractReferences : true;
        useOcr = useOcr != null ? useOcr : true;
        detectEntities = detectEntities != null ? detectEntities : true;
        asyncProcessing = asyncProcessing != null ? asyncProcessing : true;
    }
}
