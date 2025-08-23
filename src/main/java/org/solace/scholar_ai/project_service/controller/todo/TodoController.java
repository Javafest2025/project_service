package org.solace.scholar_ai.project_service.controller.todo;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.solace.scholar_ai.project_service.dto.response.APIResponse;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoCreateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoFiltersReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoStatusUpdateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoUpdateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoResponseDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoSummaryResDTO;
import org.solace.scholar_ai.project_service.service.todo.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/todo")
@RequiredArgsConstructor
@Slf4j
public class TodoController {

    private final TodoService todoService;

    @PostMapping
    public ResponseEntity<APIResponse<TodoResponseDTO>> createTodo(@RequestBody @Valid TodoCreateReqDTO request) {
        try {
            TodoResponseDTO todo = todoService.createTodo(request);
            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.CREATED.value(), "Todo created successfully", todo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(
                            HttpStatus.BAD_REQUEST.value(), "Validation error: " + e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(APIResponse.error(
                            HttpStatus.UNAUTHORIZED.value(), "Authentication error: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error creating todo", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(
                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                            "Failed to create todo: " + e.getMessage(),
                            null));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<APIResponse<TodoResponseDTO>> updateStatus(
            @PathVariable String id, @RequestBody @Valid TodoStatusUpdateReqDTO statusUpdate) {
        try {
            TodoResponseDTO todo = todoService.updateStatus(id, statusUpdate);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Status updated successfully", todo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<APIResponse<TodoResponseDTO>> updateTodo(
            @PathVariable String id, @RequestBody @Valid TodoUpdateReqDTO updateReqDTO) {
        try {
            TodoResponseDTO todo = todoService.updateTodo(id, updateReqDTO);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Todo updated successfully", todo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteTodo(@PathVariable String id) {
        try {
            todoService.deleteTodo(id);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Todo deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<TodoResponseDTO>> getTodoById(@PathVariable String id) {
        try {
            TodoResponseDTO todo = todoService.getTodoById(id);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Todo fetched successfully", todo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(APIResponse.error(HttpStatus.NOT_FOUND.value(), e.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<TodoResponseDTO>>> filterTodos(
            TodoFiltersReqDTO filters, Principal principal) {
        try {
            List<TodoResponseDTO> todos = todoService.filterTodos(filters);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Todos fetched successfully", todos));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PostMapping("/{id}/subtask")
    public ResponseEntity<APIResponse<TodoResponseDTO>> addSubtask(
            @PathVariable String id, @RequestParam String title) {
        try {
            TodoResponseDTO todo = todoService.addSubtask(id, title);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Subtask added successfully", todo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @PatchMapping("/{todoId}/subtask/{subtaskId}/toggle")
    public ResponseEntity<APIResponse<TodoResponseDTO>> toggleSubtask(
            @PathVariable String todoId, @PathVariable String subtaskId) {
        try {
            TodoResponseDTO todo = todoService.toggleSubtaskCompletion(subtaskId);
            return ResponseEntity.ok(APIResponse.success(HttpStatus.OK.value(), "Subtask toggled successfully", todo));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(HttpStatus.BAD_REQUEST.value(), e.getMessage(), null));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<APIResponse<TodoSummaryResDTO>> getSummary(@RequestParam String userId) {
        try {
            TodoSummaryResDTO summary = todoService.getSummary(userId);
            return ResponseEntity.ok(
                    APIResponse.success(HttpStatus.OK.value(), "Summary fetched successfully", summary));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), null));
        }
    }
}
