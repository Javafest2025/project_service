package org.solace.scholar_ai.project_service.service.ai;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.constant.todo.TodoCategory;
import org.solace.scholar_ai.project_service.constant.todo.TodoPriority;
import org.solace.scholar_ai.project_service.dto.chat.ParsedCommand;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoCreateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoFiltersReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoResponseDTO;
import org.solace.scholar_ai.project_service.service.todo.TodoService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandExecutorService {
    private final TodoService todoService;
    private final GeminiGeneralService geminiGeneralService;

    public Map<String, Object> executeCommand(ParsedCommand command, String userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            switch (command.getCommandType()) {
                case CREATE_TODO:
                    result.putAll(createTodo(command.getParameters(), userId));
                    break;

                case SEARCH_TODO:
                    result.putAll(searchTodos(command.getParameters()));
                    break;

                case SUMMARIZE_TODOS:
                    result.putAll(summarizeTodos(command.getParameters()));
                    break;

                case SEARCH_PAPERS:
                    result.putAll(searchPapers(command.getParameters()));
                    break;

                case GENERAL_QUESTION:
                    result.putAll(handleGeneralQuestion(command.getOriginalQuery()));
                    break;

                default:
                    result.put("error", "Unknown command type");
            }

            result.put("naturalResponse", command.getNaturalResponse());
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            result.put("naturalResponse", "I encountered an error: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> createTodo(Map<String, Object> parameters, String userId) throws Exception {
        String title = (String) parameters.get("title");
        String description = (String) parameters.get("description");
        String dueDateStr = (String) parameters.get("dueDate");
        String priorityStr = (String) parameters.getOrDefault("priority", "MEDIUM");

        // Handle null priority
        if (priorityStr == null || priorityStr.isEmpty()) {
            priorityStr = "MEDIUM";
        }

        LocalDateTime dueDate = null;
        if (dueDateStr != null) {
            try {
                dueDate = LocalDateTime.parse(dueDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                log.warn("Could not parse due date: {}", dueDateStr);
            }
        }

        TodoPriority priority;
        try {
            priority = TodoPriority.valueOf(priorityStr);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid priority '{}', defaulting to MEDIUM", priorityStr);
            priority = TodoPriority.MEDIUM;
        }

        TodoCreateReqDTO todoDto = TodoCreateReqDTO.builder()
                .userId(userId)
                .title(title)
                .description(description)
                .dueDate(dueDate != null ? dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .priority(priority)
                .category(TodoCategory.MISC)
                .build();

        TodoResponseDTO created = todoService.createTodo(todoDto);

        return Map.of("success", true, "todo", created, "message", "Todo created successfully");
    }

    private Map<String, Object> searchTodos(Map<String, Object> parameters) throws Exception {
        String searchQuery = (String) parameters.get("searchQuery");

        TodoFiltersReqDTO filters =
                TodoFiltersReqDTO.builder().search(searchQuery).build();

        List<TodoResponseDTO> todos = todoService.filterTodos(filters);

        return Map.of(
                "todos", todos,
                "count", todos.size(),
                "message", String.format("Found %d todos matching your search", todos.size()));
    }

    private Map<String, Object> summarizeTodos(Map<String, Object> parameters) throws Exception {
        String timeRange = (String) parameters.getOrDefault("timeRange", "THIS_WEEK");

        // Create filters based on time range - for now just get all todos
        TodoFiltersReqDTO filters = TodoFiltersReqDTO.builder().build();

        List<TodoResponseDTO> todos = todoService.filterTodos(filters);

        // Generate summary using Gemini
        String summaryPrompt = String.format(
                "Summarize these todos in a helpful way. Total count: %d. Todos: %s",
                todos.size(),
                todos.stream()
                        .map(todo -> String.format(
                                "- %s (Priority: %s, Status: %s)",
                                todo.getTitle(), todo.getPriority(), todo.getStatus()))
                        .reduce("", (a, b) -> a + "\n" + b));
        String summary = geminiGeneralService.generateContent(summaryPrompt);

        return Map.of(
                "todos", todos,
                "summary", summary,
                "count", todos.size(),
                "timeRange", timeRange);
    }

    private Map<String, Object> searchPapers(Map<String, Object> parameters) {
        String query = (String) parameters.get("searchQuery");
        // TODO: Implement paper search logic
        // This could integrate with academic APIs like Semantic Scholar, arXiv, etc.
        return Map.of("papers", List.of(), "message", "Paper search functionality is coming soon!", "query", query);
    }

    private Map<String, Object> handleGeneralQuestion(String question) {
        String response = geminiGeneralService.generateContent(question);
        return Map.of("response", response);
    }
}
