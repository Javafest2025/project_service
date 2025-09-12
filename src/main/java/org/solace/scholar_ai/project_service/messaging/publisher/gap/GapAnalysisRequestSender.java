package org.solace.scholar_ai.project_service.messaging.publisher.gap;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.messaging.gap.GapAnalysisMessageRequest;
import org.solace.scholar_ai.project_service.dto.request.gap.GapAnalysisRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publisher for sending gap analysis requests to RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GapAnalysisRequestSender {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${scholarai.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${scholarai.rabbitmq.gap-analysis.request-routing-key:gap.analysis.request}")
    private String requestRoutingKey;

    /**
     * Send gap analysis request to the gap analyzer service.
     */
    public void sendGapAnalysisRequest(
            GapAnalysisRequest request, UUID paperExtractionId, String correlationId, String requestId) {
        try {
            log.info("Sending gap analysis request for paper: {}, requestId: {}", request.getPaperId(), requestId);

            // Convert to message format
            GapAnalysisMessageRequest messageRequest = GapAnalysisMessageRequest.builder()
                    .paperId(request.getPaperId())
                    .paperExtractionId(paperExtractionId)
                    .correlationId(correlationId)
                    .requestId(requestId)
                    .config(request.getConfig())
                    .build();

            // Send message to exchange with routing key
            rabbitTemplate.convertAndSend(exchangeName, requestRoutingKey, messageRequest);

            log.info(
                    "Gap analysis request sent successfully for paper: {}, requestId: {}",
                    request.getPaperId(),
                    requestId);

        } catch (Exception e) {
            log.error(
                    "Failed to send gap analysis request for paper: {}, requestId: {}",
                    request.getPaperId(),
                    requestId,
                    e);
            throw new RuntimeException("Failed to send gap analysis request", e);
        }
    }
}
