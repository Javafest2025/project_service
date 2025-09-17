package org.solace.scholar_ai.project_service.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.config.GeminiConfig;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIContentService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate AI content for notes based on user prompt and context
     */
    public String generateNoteContent(String userPrompt, String noteContext, UUID projectId) {
        try {
            log.info("Generating AI content for project {} with prompt: {}", projectId, userPrompt);

            // Build the prompt with context
            String systemPrompt = buildSystemPrompt(noteContext, userPrompt);

            // Prepare the request
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> part = new HashMap<>();

            part.put("text", systemPrompt);
            contents.put("parts", List.of(part));
            requestBody.put("contents", List.of(contents));

            // Add generation config for better output
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.put("generationConfig", generationConfig);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Make the API call
            String url = geminiConfig.getApiUrl() + "?key=" + geminiConfig.getApiKey();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String content = extractContentFromResponse(response.getBody());
                log.info("Successfully generated AI content for project {}", projectId);
                return content;
            } else {
                log.error("Failed to generate AI content. Status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to generate AI content");
            }

        } catch (Exception e) {
            log.error("Error generating AI content for project {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to generate AI content: " + e.getMessage());
        }
    }

    /**
     * Build system prompt with note context and user request
     */
    private String buildSystemPrompt(String noteContext, String userPrompt) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an AI assistant helping with research note writing. ");
        prompt.append("Generate concise, well-structured markdown content based on the user's request.\n\n");

        if (noteContext != null && !noteContext.trim().isEmpty()) {
            prompt.append("Current note context:\n");
            prompt.append("```\n");
            prompt.append(noteContext);
            prompt.append("\n```\n\n");
        }

        prompt.append("User request: ").append(userPrompt).append("\n\n");

        prompt.append("Instructions:\n");
        prompt.append("- Generate content that fits naturally with the existing note context\n");
        prompt.append("- Use proper markdown formatting (headers, lists, emphasis, etc.)\n");
        prompt.append("- Keep content concise and focused\n");
        prompt.append("- If the request is unclear, provide a helpful response\n");
        prompt.append("- Do not include any meta-commentary or explanations about the generation\n");
        prompt.append("- Output only the requested content in markdown format\n");

        return prompt.toString();
    }

    /**
     * Extract content from Gemini API response
     */
    private String extractContentFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && parts.size() > 0) {
                    JsonNode firstPart = parts.get(0);
                    String text = firstPart.path("text").asText();
                    return text.trim();
                }
            }

            log.warn("Unexpected response format from Gemini API");
            return "Unable to generate content. Please try again.";

        } catch (Exception e) {
            log.error("Error parsing Gemini API response: {}", e.getMessage());
            return "Error generating content. Please try again.";
        }
    }
}
