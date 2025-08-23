package org.solace.scholar_ai.project_service.config;

import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Add request interceptor for common headers
        restTemplate.setInterceptors(Collections.singletonList(requestInterceptor()));

        return restTemplate;
    }

    @Bean
    public ClientHttpRequestInterceptor requestInterceptor() {
        return (request, body, execution) -> {
            // Add User-Agent header for external API calls
            request.getHeaders().add("User-Agent", "ScholarAI-Project-Service/1.0");
            request.getHeaders().add("Accept", "application/json");
            request.getHeaders().add("Content-Type", "application/json");

            return execution.execute(request, body);
        };
    }
}
