package org.solace.scholar_ai.project_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for the application.
 * Configures CORS, content negotiation, and other web-related settings.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * CORS is handled by the API Gateway.
     * No CORS configuration needed here.
     */
}
