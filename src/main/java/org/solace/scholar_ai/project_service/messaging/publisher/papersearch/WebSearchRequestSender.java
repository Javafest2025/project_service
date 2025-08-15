package org.solace.scholar_ai.project_service.messaging.publisher.papersearch;

import org.solace.scholar_ai.project_service.config.RabbitMQConfig;
import org.solace.scholar_ai.project_service.dto.request.papersearch.WebSearchRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSearchRequestSender {
    private final RabbitTemplate rt;
    private final RabbitMQConfig rabbitMQConfig;

    public WebSearchRequestSender(RabbitTemplate rabbitTemplate, RabbitMQConfig rabbitMQConfig) {
        this.rt = rabbitTemplate;
        this.rabbitMQConfig = rabbitMQConfig;
    }

    public void send(WebSearchRequest req) {
        rt.convertAndSend(rabbitMQConfig.getExchangeName(), rabbitMQConfig.getWebSearchRoutingKey(), req);
    }
}
