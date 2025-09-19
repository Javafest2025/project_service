package org.solace.scholar_ai.project_service.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class CitationCorsConfig {

    // Temporarily disabled - CORS might be handled globally by API Gateway
    // @Bean
    // public WebMvcConfigurer citationsCors() {
    //     return new WebMvcConfigurer() {
    //         @Override
    //         public void addCorsMappings(CorsRegistry registry) {
    //             registry.addMapping("/api/citations/**")
    //                     .allowedOriginPatterns("http://localhost:*")
    //                     .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
    //                     .allowedHeaders("*")
    //                     .allowCredentials(true)
    //                     .maxAge(3600);
    //         }
    //     };
    // }
}
