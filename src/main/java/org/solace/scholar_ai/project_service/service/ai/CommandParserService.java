package org.solace.scholar_ai.project_service.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.constant.CommandType;
import org.solace.scholar_ai.project_service.dto.chat.ParsedCommand;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandParserService {
    private final GeminiGeneralService geminiGeneralService;
    private final ObjectMapper objectMapper;

    public ParsedCommand parseCommand(String userInput) {
        log.info("🔍 Parsing command: {}", userInput);

        try {
            String prompt = buildParsingPrompt(userInput);
            log.info("📝 Generated prompt: {}", prompt);

            String geminiResponse = geminiGeneralService.generateContent(prompt);
            log.info("🤖 Gemini response: {}", geminiResponse);

            // Parse JSON response from Gemini
            String jsonResponse = extractJsonFromResponse(geminiResponse);
            log.info("📄 Extracted JSON: {}", jsonResponse);

            Map<String, Object> parsedResponse =
                    objectMapper.readValue(jsonResponse, new TypeReference<Map<String, Object>>() {});
            log.info("✅ Parsed response: {}", parsedResponse);

            // Safely extract values with null checks
            String commandTypeStr = (String) parsedResponse.get("commandType");
            Map<String, Object> parameters = (Map<String, Object>) parsedResponse.get("parameters");
            String response = (String) parsedResponse.get("response");

            // Validate required fields
            if (commandTypeStr == null || commandTypeStr.isEmpty()) {
                throw new IllegalArgumentException("Command type is missing from Gemini response");
            }

            if (parameters == null) {
                parameters = new HashMap<>();
            }

            if (response == null || response.isEmpty()) {
                response = "I'll help you with that.";
            }

            return ParsedCommand.builder()
                    .commandType(CommandType.valueOf(commandTypeStr))
                    .parameters(parameters)
                    .originalQuery(userInput)
                    .naturalResponse(response)
                    .build();
        } catch (Exception e) {
            log.error("❌ Error parsing command: {}", e.getMessage(), e);
            log.info("🔄 Falling back to general question handler");

            // Fallback to general question if parsing fails
            String fallbackResponse = geminiGeneralService.generateContent(userInput);
            log.info("🔄 Fallback response: {}", fallbackResponse);

            return ParsedCommand.builder()
                    .commandType(CommandType.GENERAL_QUESTION)
                    .originalQuery(userInput)
                    .naturalResponse(fallbackResponse)
                    .build();
        }
    }

    private String buildParsingPrompt(String userInput) {
        return """
                You are a command parser for a productivity assistant. Analyze the following user input and determine:
                1. The command type
                2. Extract relevant parameters
                3. Generate a natural language response

                Available command types:
                - CREATE_TODO: Create a new todo item
                - UPDATE_TODO: Update existing todo
                - DELETE_TODO: Delete a todo
                - SEARCH_TODO: Search for todos
                - SUMMARIZE_TODOS: Summarize todos (by date, status, etc.)
                - SEARCH_PAPERS: Search academic papers
                - GENERAL_QUESTION: General questions

                User input: "%s"

                Respond ONLY with a JSON object in this exact format:
                {
                    "commandType": "COMMAND_TYPE",
                    "parameters": {
                        "title": "extracted title if applicable",
                        "description": "extracted description",
                        "dueDate": "ISO date string if mentioned",
                        "priority": "HIGH/MEDIUM/LOW if mentioned",
                        "searchQuery": "search terms if searching",
                        "timeRange": "TODAY/THIS_WEEK/THIS_MONTH if summarizing"
                    },
                    "response": "Natural language response to show the user"
                }

                Examples:
                Input: "Create todo for meeting today at 6 pm"
                Output: {
                    "commandType": "CREATE_TODO",
                    "parameters": {
                        "title": "Meeting",
                        "dueDate": "2024-01-15T18:00:00",
                        "priority": "MEDIUM"
                    },
                    "response": "I'll create a todo for your meeting today at 6 PM."
                }

                Input: "Create a to-do for meeting tomorrow at 6 pm"
                Output: {
                    "commandType": "CREATE_TODO",
                    "parameters": {
                        "title": "Meeting",
                        "dueDate": "2024-01-16T18:00:00",
                        "priority": "MEDIUM"
                    },
                    "response": "I'll create a todo for your meeting tomorrow at 6 PM."
                }
                """
                .formatted(userInput);
    }

    private String extractJsonFromResponse(String response) {
        // Extract JSON from response (handle markdown code blocks)
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}") + 1;
        if (start != -1 && end > start) {
            return response.substring(start, end);
        }
        return "{}";
    }
}
