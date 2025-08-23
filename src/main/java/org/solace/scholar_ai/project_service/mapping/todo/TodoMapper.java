package org.solace.scholar_ai.project_service.mapping.todo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoCreateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoResponseDTO;
import org.solace.scholar_ai.project_service.model.todo.Todo;
import org.solace.scholar_ai.project_service.model.todo.TodoSubtask;

@Mapper(
        componentModel = "spring",
        imports = {LocalDateTime.class})
public interface TodoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "dueDate", source = "dueDate", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updatedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "actualTime", ignore = true)
    @Mapping(target = "relatedPaperId", ignore = true)
    @Mapping(target = "subtasks", ignore = true)
    @Mapping(target = "reminders", ignore = true)
    Todo todoCreateRequestToTodo(TodoCreateReqDTO request);

    @Mapping(source = "dueDate", target = "dueDate", qualifiedByName = "localDateTimeToString")
    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToString")
    @Mapping(source = "updatedAt", target = "updatedAt", qualifiedByName = "localDateTimeToString")
    @Mapping(source = "completedAt", target = "completedAt", qualifiedByName = "localDateTimeToString")
    TodoResponseDTO todoToTodoResponse(Todo todo);

    List<TodoResponseDTO> todosToTodoResponses(List<Todo> todos);

    @Mapping(source = "createdAt", target = "createdAt", qualifiedByName = "localDateTimeToString")
    TodoResponseDTO.SubtaskResponse subtaskToSubtaskResponse(TodoSubtask subtask);

    // --- Custom Mappers for Date/Time ---

    @Named("localDateTimeToString")
    default String localDateTimeToString(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) return null;
        try {
            // Handle ISO strings with milliseconds and timezone (e.g., "2025-07-07T08:30:00.000Z")
            if (dateString.contains("Z") || dateString.contains("+") || dateString.contains("-")) {
                return java.time.OffsetDateTime.parse(dateString).toLocalDateTime();
            }
            // Handle local date time strings
            return LocalDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            // Fallback: try different common formats
            try {
                return LocalDateTime.parse(dateString.replace("Z", ""), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                throw new IllegalArgumentException("Unable to parse date string: " + dateString, e2);
            }
        }
    }
}
