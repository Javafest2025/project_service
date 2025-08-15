package org.solace.scholar_ai.project_service.messaging.listener.papersearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.event.papersearch.WebSearchCompletedEvent;
import org.solace.scholar_ai.project_service.service.paper.search.WebSearchService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSearchCompletedListener {

    private final WebSearchService webSearchService;

    @RabbitListener(queues = "${scholarai.rabbitmq.web-search.completed-queue}")
    public void handleWebSearchCompleted(WebSearchCompletedEvent event) {
        try {
            log.info("Received web search completed event for correlation ID: {}", event.correlationId());
            webSearchService.updateSearchResults(event);
            log.info("Successfully processed web search completed event for correlation ID: {}", event.correlationId());
        } catch (Exception e) {
            log.error(
                    "Error processing web search completed event for correlation ID {}: {}",
                    event.correlationId(),
                    e.getMessage(),
                    e);
            throw e; // Re-throw to trigger message rejection
        }
    }
}
