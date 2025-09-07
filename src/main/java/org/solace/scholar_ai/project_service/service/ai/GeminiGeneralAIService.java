package org.solace.scholar_ai.project_service.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.config.GeminiConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiGeneralAIService {
    private final RestTemplate restTemplate;
    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;

    public String generateContent(String prompt) {
        log.info("🚀 Calling Gemini API with prompt: {}", prompt);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(Map.of("text", prompt)));
            requestBody.put("contents", List.of(content));

            String url = geminiConfig.getApiUrl() + "?key=" + geminiConfig.getApiKey();
            log.info("🌐 Gemini API URL: {}", url.replace(geminiConfig.getApiKey(), "***HIDDEN***"));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            log.info("📤 Request body: {}", requestBody);

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            log.info("📥 Response status: {}", response.getStatusCode());
            log.info("📥 Response body: {}", response.getBody());

            // Extract text from response
            String extractedText = extractTextFromResponse(response.getBody());
            log.info("📄 Extracted text: {}", extractedText);

            return extractedText;
        } catch (Exception e) {
            log.error("❌ Error generating content with Gemini: {}", e.getMessage(), e);
            return "I apologize, but I'm having trouble processing your request right now. Please try again later.";
        }
    }

    private String extractTextFromResponse(Map responseBody) {
        try {
            // Parse Gemini response structure
            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map content = (Map) candidates.get(0).get("content");
                List<Map> parts = (List<Map>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
            return "No response generated";
        } catch (Exception e) {
            log.error("Error extracting text from Gemini response: {}", e.getMessage(), e);
            return "Error processing response";
        }
    }
}
