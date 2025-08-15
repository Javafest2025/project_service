package org.solace.scholar_ai.project_service.dto.request.papersearch;

import java.util.List;
import java.util.UUID;

public record WebSearchRequest(
        UUID projectId, List<String> queryTerms, String domain, Integer batchSize, String correlationId) {}
