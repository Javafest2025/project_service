package org.solace.scholar_ai.project_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // Set timeouts for external API calls (120 seconds for both connect and read)
        factory.setConnectTimeout(120000); // 2 minutes
        factory.setReadTimeout(120000); // 2 minutes

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
