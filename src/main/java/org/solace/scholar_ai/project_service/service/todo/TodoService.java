package org.solace.scholar_ai.project_service.service.todo;

import java.util.List;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoCreateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoFiltersReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoStatusUpdateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoUpdateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoResponseDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoSummaryResDTO;

public interface TodoService {
    TodoResponseDTO createTodo(TodoCreateReqDTO request) throws Exception;

    TodoResponseDTO updateStatus(String id, TodoStatusUpdateReqDTO statusUpdate) throws Exception;

    TodoResponseDTO updateTodo(String id, TodoUpdateReqDTO updateReqDTO) throws Exception;

    void deleteTodo(String id) throws Exception;

    TodoResponseDTO getTodoById(String id) throws Exception;

    List<TodoResponseDTO> filterTodos(TodoFiltersReqDTO filters) throws Exception;

    TodoSummaryResDTO getSummary(String userId) throws Exception;

    TodoResponseDTO addSubtask(String todoId, String subtaskTitle) throws Exception;

    TodoResponseDTO toggleSubtaskCompletion(String subtaskId) throws Exception;
}
