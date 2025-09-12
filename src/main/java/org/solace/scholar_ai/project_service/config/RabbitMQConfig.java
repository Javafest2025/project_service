package org.solace.scholar_ai.project_service.config;

import lombok.Getter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures RabbitMQ beans for message queuing within the ScholarAI
 * application.
 * This class sets up exchanges, queues, bindings, and message converters
 * to enable asynchronous communication between different services.
 */
@Configuration
@Getter
public class RabbitMQConfig {

    @Value("${scholarai.rabbitmq.exchange}")
    private String exchangeName;

    @Value("${scholarai.rabbitmq.web-search.queue}")
    private String webSearchQueue;

    @Value("${scholarai.rabbitmq.web-search.routing-key}")
    private String webSearchRoutingKey;

    @Value("${scholarai.rabbitmq.web-search.completed-queue}")
    private String webSearchCompletedQueue;

    @Value("${scholarai.rabbitmq.web-search.completed-routing-key}")
    private String webSearchCompletedRoutingKey;

    // Extraction queue properties
    @Value("${scholarai.rabbitmq.extraction.queue}")
    private String extractionQueue;

    @Value("${scholarai.rabbitmq.extraction.routing-key}")
    private String extractionRoutingKey;

    @Value("${scholarai.rabbitmq.extraction.completed-queue}")
    private String extractionCompletedQueue;

    @Value("${scholarai.rabbitmq.extraction.completed-routing-key}")
    private String extractionCompletedRoutingKey;

    // Gap analysis queue properties
    @Value("${scholarai.rabbitmq.gap-analysis.request-queue}")
    private String gapAnalysisRequestQueue;

    @Value("${scholarai.rabbitmq.gap-analysis.request-routing-key}")
    private String gapAnalysisRequestRoutingKey;

    @Value("${scholarai.rabbitmq.gap-analysis.response-queue}")
    private String gapAnalysisResponseQueue;

    @Value("${scholarai.rabbitmq.gap-analysis.response-exchange}")
    private String gapAnalysisResponseExchange;

    @Value("${scholarai.rabbitmq.gap-analysis.response-routing-key}")
    private String gapAnalysisResponseRoutingKey;

    /**
     * Creates a durable topic exchange for the application.
     * Topic exchanges route messages based on wildcard matches between the routing
     * key and routing patterns.
     *
     * @return The configured TopicExchange.
     */
    @Bean
    public TopicExchange appExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    /**
     * Creates a durable queue for web search tasks.
     * Configured with TTL and max length to match the Python service configuration.
     *
     * @return The configured Queue for web search.
     */
    @Bean
    public Queue webSearchQueue() {
        return QueueBuilder.durable(webSearchQueue)
                .withArgument("x-message-ttl", 300000) // 5 minutes TTL
                .withArgument("x-max-length", 1000) // Max 1000 messages
                .build();
    }

    /**
     * Creates a durable queue for completed web search tasks.
     *
     * @return The configured Queue for completed web search.
     */
    @Bean
    public Queue webSearchCompletedQueue() {
        return QueueBuilder.durable(webSearchCompletedQueue).build();
    }

    /**
     * Binds the web search queue to the application exchange using its specific
     * routing key.
     *
     * @param webSearchQueue The queue for web search tasks.
     * @param appExchange    The main application topic exchange.
     * @return The Binding definition.
     */
    @Bean
    public Binding bindWebSearch(Queue webSearchQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(webSearchQueue).to(appExchange).with(webSearchRoutingKey);
    }

    /**
     * Binds the web search completed queue to the application exchange using its
     * specific routing key.
     *
     * @param webSearchCompletedQueue The queue for completed web search tasks.
     * @param appExchange             The main application topic exchange.
     * @return The Binding definition.
     */
    @Bean
    public Binding bindWebSearchCompleted(Queue webSearchCompletedQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(webSearchCompletedQueue).to(appExchange).with(webSearchCompletedRoutingKey);
    }

    /**
     * Creates a durable queue for extraction tasks.
     * Configured with TTL and max length to match the Python service configuration.
     *
     * @return The configured Queue for extraction.
     */
    @Bean
    public Queue extractionQueue() {
        return QueueBuilder.durable(extractionQueue)
                .withArgument("x-message-ttl", 600000) // 10 minutes TTL for longer extraction tasks
                .withArgument("x-max-length", 500) // Max 500 messages
                .build();
    }

    /**
     * Creates a durable queue for completed extraction tasks.
     *
     * @return The configured Queue for completed extraction.
     */
    @Bean
    public Queue extractionCompletedQueue() {
        return QueueBuilder.durable(extractionCompletedQueue).build();
    }

    /**
     * Binds the extraction queue to the application exchange using its specific
     * routing key.
     *
     * @param extractionQueue The queue for extraction tasks.
     * @param appExchange     The main application topic exchange.
     * @return The Binding definition.
     */
    @Bean
    public Binding bindExtraction(Queue extractionQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(extractionQueue).to(appExchange).with(extractionRoutingKey);
    }

    /**
     * Binds the extraction completed queue to the application exchange using its
     * specific routing key.
     *
     * @param extractionCompletedQueue The queue for completed extraction tasks.
     * @param appExchange              The main application topic exchange.
     * @return The Binding definition.
     */
    @Bean
    public Binding bindExtractionCompleted(Queue extractionCompletedQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(extractionCompletedQueue).to(appExchange).with(extractionCompletedRoutingKey);
    }

    /**
     * Creates a durable queue for gap analysis requests.
     * Note: Queue configuration matches existing queue to avoid PRECONDITION_FAILED
     * errors.
     *
     * @return The configured Queue for gap analysis requests.
     */
    @Bean
    public Queue gapAnalysisRequestQueue() {
        return QueueBuilder.durable(gapAnalysisRequestQueue).build();
    }

    /**
     * Creates a durable queue for gap analysis responses.
     *
     * @return The configured Queue for gap analysis responses.
     */
    @Bean
    public Queue gapAnalysisResponseQueue() {
        return QueueBuilder.durable(gapAnalysisResponseQueue).build();
    }

    /**
     * Creates a durable topic exchange for gap analysis responses.
     * This exchange is used specifically for gap analysis response messages.
     *
     * @return The configured TopicExchange for gap analysis responses.
     */
    @Bean
    public TopicExchange gapAnalysisResponseExchange() {
        return ExchangeBuilder.topicExchange(gapAnalysisResponseExchange)
                .durable(true)
                .build();
    }

    /**
     * Binds the gap analysis request queue to the application exchange using its
     * specific routing key.
     *
     * @param gapAnalysisRequestQueue The queue for gap analysis requests.
     * @param appExchange             The main application topic exchange.
     * @return The Binding definition.
     */
    @Bean
    public Binding bindGapAnalysisRequest(Queue gapAnalysisRequestQueue, TopicExchange appExchange) {
        return BindingBuilder.bind(gapAnalysisRequestQueue).to(appExchange).with(gapAnalysisRequestRoutingKey);
    }

    /**
     * Binds the gap analysis response queue to the gap analysis response exchange
     * using its specific routing key.
     *
     * @param gapAnalysisResponseQueue    The queue for gap analysis responses.
     * @param gapAnalysisResponseExchange The gap analysis response topic exchange.
     * @return The Binding definition.
     */
    @Bean
    public Binding bindGapAnalysisResponse(Queue gapAnalysisResponseQueue, TopicExchange gapAnalysisResponseExchange) {
        return BindingBuilder.bind(gapAnalysisResponseQueue)
                .to(gapAnalysisResponseExchange)
                .with(gapAnalysisResponseRoutingKey);
    }

    /**
     * Creates a message converter to serialize and deserialize messages as JSON.
     * This allows Java objects to be sent and received as JSON payloads.
     *
     * @return The Jackson2JsonMessageConverter instance.
     */
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates a RabbitTemplate configured with a JSON message converter.
     * The RabbitTemplate provides helper methods for sending and receiving
     * messages.
     *
     * @param cf The connection factory for RabbitMQ.
     * @return The configured RabbitTemplate.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        var rt = new RabbitTemplate(cf);
        rt.setMessageConverter(jsonMessageConverter());
        return rt;
    }

    /**
     * Configures the listener container factory for message listeners.
     * This factory sets up concurrency, message conversion, and error handling
     * for RabbitMQ message listeners.
     *
     * @param cf The connection factory for RabbitMQ.
     * @return The configured SimpleRabbitListenerContainerFactory.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory listenerFactory(ConnectionFactory cf) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(cf);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        factory.setDefaultRequeueRejected(false); // send bad messages to DLQ
        return factory;
    }
}
