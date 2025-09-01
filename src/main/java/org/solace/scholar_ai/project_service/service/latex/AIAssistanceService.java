package org.solace.scholar_ai.project_service.service.latex;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIAssistanceService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:AIzaSyD1f5phqFOLvgC4zRnpPCd6EBQOegKHNuw}")
    private String geminiApiKey;

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent";

    public Map<String, Object> reviewDocument(String content) {
        try {
            String prompt = String.format(
                    """
                Review this LaTeX document for academic writing quality. Provide scores and suggestions for:
                1. Clarity (0-1 score)
                2. Completeness (0-1 score)
                3. Grammar issues (list of specific problems)
                4. Style suggestions (list of improvements)

                Document content:
                %s

                Respond in JSON format with keys: clarityScore, completenessScore, grammarIssues, styleSuggestions
                """,
                    content);

            String response = callGeminiAPI(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Error reviewing document", e);
            return createDefaultReviewResponse();
        }
    }

    public String generateContextualSuggestions(String content, String context) {
        try {
            String prompt = String.format(
                    """
                Generate writing suggestions for this LaTeX document. Context: %s

                Document content:
                %s

                Provide specific, actionable suggestions for improving the document. Focus on:
                - Structure and organization
                - Academic writing style
                - LaTeX formatting
                - Content clarity

                Respond with clear, numbered suggestions.
                """,
                    context, content);

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Error generating suggestions", e);
            return "Unable to generate suggestions at this time.";
        }
    }

    public Map<String, Object> checkCompliance(String content, String venue) {
        try {
            String prompt = String.format(
                    """
                Check this LaTeX document for compliance with %s standards. Analyze:
                1. Word count
                2. Page count estimation
                3. Presence of abstract
                4. Presence of references
                5. IEEE compliance (if applicable)

                Document content:
                %s

                Respond in JSON format with keys: wordCount, pageCount, hasAbstract, hasReferences, ieeeCompliant
                """,
                    venue, content);

            String response = callGeminiAPI(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Error checking compliance", e);
            return createDefaultComplianceResponse();
        }
    }

    public Map<String, Object> validateCitations(String content) {
        try {
            String prompt = String.format(
                    """
                Validate citations in this LaTeX document. Check for:
                1. Proper citation format
                2. Bibliography consistency
                3. Missing citations
                4. Duplicate citations

                Document content:
                %s

                Respond in JSON format with keys: validCitations, invalidCitations, missingSources, duplicates
                """,
                    content);

            String response = callGeminiAPI(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Error validating citations", e);
            return createDefaultCitationResponse();
        }
    }

    public Map<String, Object> generateCorrections(String content) {
        try {
            String prompt = String.format(
                    """
                Generate AI corrections for this LaTeX document. Provide:
                1. Grammar corrections
                2. Style improvements
                3. Structure suggestions
                4. LaTeX formatting fixes

                Document content:
                %s

                Respond in JSON format with keys: originalText, correctedText, corrections (array of changes)
                """,
                    content);

            String response = callGeminiAPI(prompt);
            return parseGeminiResponse(response);
        } catch (Exception e) {
            log.error("Error generating corrections", e);
            return createDefaultCorrectionsResponse();
        }
    }

    public String processChatRequest(String selectedText, String userRequest, String fullDocument) {
        try {
            String prompt = String.format(
                    """
                You are an AI assistant for LaTeX document editing. Help the user with their request.

                Selected text: %s
                User request: %s
                Full document context: %s

                Based on the selected text and user request, provide a helpful LaTeX response.

                IMPORTANT: Structure your response in exactly this format:

                1. First, provide a brief explanation of what you're doing (1-2 sentences)
                2. Then, on a new line, provide the LaTeX code in a code block like this:
                ```latex
                [your LaTeX code here]
                ```

                Examples:
                - If adding content: "I'll add a new table at the specified position."
                ```latex
                \\begin{table}[h]
                \\centering
                \\caption{New Table}
                \\begin{tabular}{|c|c|}
                \\hline
                Header 1 & Header 2 \\\\
                \\hline
                Data 1 & Data 2 \\\\
                \\hline
                \\end{tabular}
                \\end{table}
                ```

                - If replacing content: "I'll replace the selected text with an improved version."
                ```latex
                [new LaTeX content]
                ```

                - If deleting content: "I'll remove the selected text as requested."
                ```latex
                [empty or comment]
                ```

                Keep responses focused on LaTeX editing and be specific about the changes.
                """,
                    selectedText,
                    userRequest,
                    fullDocument.length() > 1000 ? fullDocument.substring(0, 1000) + "..." : fullDocument);

            return callGeminiAPI(prompt);
        } catch (Exception e) {
            log.error("Error processing chat request", e);
            return "I'm sorry, I encountered an error processing your request. Please try again.";
        }
    }

    private String callGeminiAPI(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, String> parts = new HashMap<>();
            parts.put("text", prompt);
            contents.put("parts", new Map[] {parts});
            requestBody.put("contents", new Map[] {contents});

            String url = GEMINI_API_URL + "?key=" + geminiApiKey;

            Map<String, Object> response = restTemplate.postForObject(url, requestBody, Map.class);

            if (response != null && response.containsKey("candidates")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> candidates =
                        (java.util.List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> content =
                            (Map<String, Object>) candidates.get(0).get("content");
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, String>> contentParts =
                            (java.util.List<Map<String, String>>) content.get("parts");
                    if (!contentParts.isEmpty()) {
                        return contentParts.get(0).get("text");
                    }
                }
            }

            return "No response from AI service";
        } catch (Exception e) {
            log.error("Error calling Gemini API", e);
            throw new RuntimeException("AI service unavailable", e);
        }
    }

    private Map<String, Object> parseGeminiResponse(String response) {
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse JSON response, returning as text", e);
            Map<String, Object> result = new HashMap<>();
            result.put("text", response);
            return result;
        }
    }

    private Map<String, Object> createDefaultReviewResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("clarityScore", 0.7);
        response.put("completenessScore", 0.8);
        response.put("grammarIssues", java.util.List.of("AI service unavailable"));
        response.put("styleSuggestions", java.util.List.of("AI service unavailable"));
        return response;
    }

    private Map<String, Object> createDefaultComplianceResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("wordCount", 0);
        response.put("pageCount", 0);
        response.put("hasAbstract", false);
        response.put("hasReferences", false);
        response.put("ieeeCompliant", false);
        return response;
    }

    private Map<String, Object> createDefaultCitationResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("validCitations", java.util.List.of());
        response.put("invalidCitations", java.util.List.of());
        response.put("missingSources", java.util.List.of());
        response.put("duplicates", java.util.List.of());
        return response;
    }

    private Map<String, Object> createDefaultCorrectionsResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("originalText", "");
        response.put("correctedText", "");
        response.put("corrections", java.util.List.of());
        return response;
    }
}
