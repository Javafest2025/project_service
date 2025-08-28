package org.solace.scholar_ai.project_service.messaging.listener.extraction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.event.extraction.ExtractionCompletedEvent;
import org.solace.scholar_ai.project_service.service.extraction.ExtractionService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listener for extraction completion events from the extractor service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionCompletedListener {

    private final ExtractionService extractionService;

    /**
     * Handle extraction completion events
     *
     * @param event The extraction completion event
     */
    @RabbitListener(queues = "${scholarai.rabbitmq.extraction.completed-queue}")
    public void handleExtractionCompleted(ExtractionCompletedEvent event) {
        try {
            log.info("Received extraction completed event for job ID: {} (paper: {})", event.jobId(), event.paperId());

            extractionService.handleExtractionCompleted(event);

            log.info("Successfully processed extraction completed event for job ID: {}", event.jobId());
        } catch (Exception e) {
            log.error(
                    "Error processing extraction completed event for job ID {}: {}", event.jobId(), e.getMessage(), e);
            throw e; // Re-throw to trigger message rejection
        }
    }
}
