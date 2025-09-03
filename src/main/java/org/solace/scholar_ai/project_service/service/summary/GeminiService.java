package org.solace.scholar_ai.project_service.service.summary;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.config.GeminiConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Data
    @Builder
    public static class GenerationConfig {
        @Builder.Default
        private Double temperature = 0.3;

        @Builder.Default
        private Integer maxOutputTokens = 2000;

        @Builder.Default
        private Double topP = 0.95;

        @Builder.Default
        private Integer topK = 40;
    }

    /**
     * Generate content using Gemini API with resilience4j protection
     */
    @Retry(name = "gemini-api", fallbackMethod = "generateFallback")
    @CircuitBreaker(name = "gemini-api", fallbackMethod = "generateFallback")
    @RateLimiter(name = "gemini-api", fallbackMethod = "generateFallback")
    public String generate(String prompt, GenerationConfig config) {
        try {
            String url = geminiConfig.getApiUrl() + "?key=" + geminiConfig.getApiKey();

            Map<String, Object> requestBody = new HashMap<>();

            // Add prompt
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", prompt)));
            requestBody.put("contents", List.of(content));

            // Add generation config
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", config.getTemperature());
            generationConfig.put("maxOutputTokens", config.getMaxOutputTokens());
            generationConfig.put("topP", config.getTopP());
            generationConfig.put("topK", config.getTopK());
            requestBody.put("generationConfig", generationConfig);

            // Add safety settings
            requestBody.put(
                    "safetySettings",
                    List.of(
                            Map.of("category", "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold", "BLOCK_NONE"),
                            Map.of("category", "HARM_CATEGORY_HATE_SPEECH", "threshold", "BLOCK_NONE"),
                            Map.of("category", "HARM_CATEGORY_HARASSMENT", "threshold", "BLOCK_NONE"),
                            Map.of("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold", "BLOCK_NONE")));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractTextFromResponse(response.getBody());
            }

            throw new RuntimeException("Failed to get response from Gemini");

        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("Gemini API call failed", e);
        }
    }

    /**
     * Extract text from Gemini response
     */
    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            throw new RuntimeException("Invalid response structure from Gemini");
        } catch (Exception e) {
            log.error("Error extracting text from Gemini response", e);
            throw new RuntimeException("Failed to parse Gemini response", e);
        }
    }

    /**
     * Fallback method for when Gemini API is unavailable
     * Returns a structured response that can be used to continue summary generation
     */
    public String generateFallback(String prompt, GenerationConfig config, Exception exception) {
        log.warn("Gemini API unavailable, using fallback response. Error: {}", exception.getMessage());

        // Add metadata to identify this as a fallback response
        String metadata = "\"_response_source\": \"fallback\", \"_fallback_reason\": \"" + exception.getMessage()
                + "\", \"_timestamp\": \"" + java.time.Instant.now() + "\"";

        // Return a structured fallback response based on the prompt type
        if (prompt.contains("quick_take")) {
            return "{\"one_liner\": \"Summary generation temporarily unavailable\", \"key_contributions\": [\"Please try again later\"], \"method_overview\": \"AI service is currently overloaded\", \"main_findings\": [], \"limitations\": [\"Service temporarily unavailable\"], \"applicability\": [\"Retry in a few minutes\"], "
                    + metadata + "}";
        } else if (prompt.contains("methods")) {
            return "{\"study_type\": \"UNKNOWN\", \"research_questions\": [\"Service unavailable\"], \"datasets\": [], \"participants\": null, \"procedure_or_pipeline\": \"AI service overloaded\", \"baselines_or_controls\": [\"Please retry\"], \"metrics\": [], \"statistical_analysis\": [\"Service temporarily unavailable\"], \"compute_resources\": null, \"implementation_details\": null, "
                    + metadata + "}";
        } else if (prompt.contains("reproducibility")) {
            return "{\"artifacts\": null, \"reproducibility_notes\": \"AI service temporarily unavailable\", \"repro_score\": 0.0, "
                    + metadata + "}";
        } else if (prompt.contains("ethics")) {
            return "{\"ethics\": null, \"bias_and_fairness\": [\"Service unavailable\"], \"risks_and_misuse\": [\"Please retry later\"], \"data_rights\": \"AI service overloaded\", "
                    + metadata + "}";
        } else if (prompt.contains("context_impact")) {
            return "{\"novelty_type\": \"UNKNOWN\", \"positioning\": [\"Service temporarily unavailable\"], \"related_works_key\": [], \"impact_notes\": \"AI service is currently overloaded\", \"domain_classification\": [\"Please retry in a few minutes\"], \"technical_depth\": \"unknown\", \"interdisciplinary_connections\": [\"Service unavailable\"], \"future_work\": [\"Please try again later\"], "
                    + metadata + "}";
        } else {
            return "{\"error\": \"AI service temporarily unavailable\", \"message\": \"Please try again in a few minutes\", "
                    + metadata + "}";
        }
    }
}
