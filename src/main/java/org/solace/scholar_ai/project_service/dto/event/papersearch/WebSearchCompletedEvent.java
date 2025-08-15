package org.solace.scholar_ai.project_service.dto.event.papersearch;

import java.util.List;
import java.util.UUID;
import org.solace.scholar_ai.project_service.dto.paper.PaperMetadataDto;

public record WebSearchCompletedEvent(UUID projectId, String correlationId, List<PaperMetadataDto> papers) {}
