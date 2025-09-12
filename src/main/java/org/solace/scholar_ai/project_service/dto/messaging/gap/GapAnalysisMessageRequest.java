package org.solace.scholar_ai.project_service.dto.messaging.gap;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisConfigDto;

/**
 * Message DTO for sending gap analysis requests to RabbitMQ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisMessageRequest {

    private UUID paperId;
    private UUID paperExtractionId;
    private String correlationId;
    private String requestId;
    private GapAnalysisConfigDto config;
}
