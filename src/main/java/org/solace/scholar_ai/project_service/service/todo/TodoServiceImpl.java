package org.solace.scholar_ai.project_service.service.todo;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoCreateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoFiltersReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoStatusUpdateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoUpdateReqDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoResponseDTO;
import org.solace.scholar_ai.project_service.dto.todo.response.TodoSummaryResDTO;
import org.solace.scholar_ai.project_service.mapping.todo.TodoMapper;
import org.solace.scholar_ai.project_service.model.todo.Todo;
import org.solace.scholar_ai.project_service.model.todo.TodoReminder;
import org.solace.scholar_ai.project_service.model.todo.TodoSubtask;
import org.solace.scholar_ai.project_service.repository.todo.TodoRepository;
import org.solace.scholar_ai.project_service.repository.todo.TodoSubtaskRepository;
import org.solace.scholar_ai.project_service.util.todo.TodoSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoRepository todoRepository;
    private final TodoSubtaskRepository subtaskRepository;
    private final TodoMapper todoMapper;

    @Override
    public TodoResponseDTO createTodo(TodoCreateReqDTO request) throws Exception {
        try {
            // Validate user ID from request
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }

            // Validate required fields
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (request.getPriority() == null) {
                throw new IllegalArgumentException("Priority is required");
            }
            if (request.getCategory() == null) {
                throw new IllegalArgumentException("Category is required");
            }

            Todo todo = todoMapper.todoCreateRequestToTodo(request);

            if (request.getSubtasks() != null) {
                request.getSubtasks().forEach(subtaskReq -> {
                    TodoSubtask subtask = TodoSubtask.builder()
                            .title(subtaskReq.getTitle())
                            .completed(false)
                            .build();
                    todo.addSubtask(subtask);
                });
            }
            if (request.getReminders() != null) {
                request.getReminders().forEach(reminderReq -> {
                    TodoReminder reminder = TodoReminder.builder()
                            .remindAt(todoMapper.stringToLocalDateTime(reminderReq.getRemindAt()))
                            .message(reminderReq.getMessage())
                            .sent(false)
                            .build();
                    todo.addReminder(reminder);
                });
            }
            return todoMapper.todoToTodoResponse(todoRepository.save(todo));
        } catch (IllegalStateException e) {
            throw new Exception("Authentication error: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new Exception("Validation error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new Exception("Failed to create todo: " + e.getMessage(), e);
        }
    }

    @Override
    public TodoResponseDTO updateStatus(String id, TodoStatusUpdateReqDTO statusUpdate) throws Exception {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Todo not found"));
        // Note: In a real application, you would validate that the todo belongs to the current user
        // For now, we'll just update the status
        todo.setStatus(statusUpdate.getStatus());
        return todoMapper.todoToTodoResponse(todoRepository.save(todo));
    }

    @Override
    public TodoResponseDTO updateTodo(String id, TodoUpdateReqDTO updateReqDTO) throws Exception {
        Todo todo = todoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Todo not found"));
        if (updateReqDTO.getTitle() != null) todo.setTitle(updateReqDTO.getTitle());
        if (updateReqDTO.getDescription() != null) todo.setDescription(updateReqDTO.getDescription());
        if (updateReqDTO.getPriority() != null) todo.setPriority(updateReqDTO.getPriority());
        if (updateReqDTO.getCategory() != null) todo.setCategory(updateReqDTO.getCategory());
        if (updateReqDTO.getDueDate() != null)
            todo.setDueDate(todoMapper.stringToLocalDateTime(updateReqDTO.getDueDate()));
        if (updateReqDTO.getEstimatedTime() != null) todo.setEstimatedTime(updateReqDTO.getEstimatedTime());
        if (updateReqDTO.getRelatedProjectId() != null) todo.setRelatedProjectId(updateReqDTO.getRelatedProjectId());
        if (updateReqDTO.getTags() != null) todo.setTags(updateReqDTO.getTags());

        todo.getSubtasks().clear();
        if (updateReqDTO.getSubtasks() != null) {
            updateReqDTO.getSubtasks().forEach(subtaskReq -> {
                TodoSubtask subtask = TodoSubtask.builder()
                        .title(subtaskReq.getTitle())
                        .completed(false)
                        .build();
                todo.addSubtask(subtask);
            });
        }

        todo.getReminders().clear();
        if (updateReqDTO.getReminders() != null) {
            updateReqDTO.getReminders().forEach(reminderReq -> {
                TodoReminder reminder = TodoReminder.builder()
                        .remindAt(todoMapper.stringToLocalDateTime(reminderReq.getRemindAt()))
                        .message(reminderReq.getMessage())
                        .sent(false)
                        .build();
                todo.addReminder(reminder);
            });
        }
        return todoMapper.todoToTodoResponse(todoRepository.save(todo));
    }

    @Override
    public void deleteTodo(String id) throws Exception {
        if (!todoRepository.existsById(id)) throw new EntityNotFoundException("Todo not found");
        todoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TodoResponseDTO getTodoById(String id) throws Exception {
        return todoRepository
                .findById(id)
                .map(todoMapper::todoToTodoResponse)
                .orElseThrow(() -> new EntityNotFoundException("Todo not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoResponseDTO> filterTodos(TodoFiltersReqDTO filters) throws Exception {
        if (filters.getUserId() == null || filters.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        Specification<Todo> spec = TodoSpecification.fromFilters(filters, filters.getUserId());
        List<Todo> todos = todoRepository.findAll(spec);
        return todoMapper.todosToTodoResponses(todos);
    }

    private static boolean enumEquals(String actual, String expected) {
        if (actual == null || expected == null) return false;
        return actual.replace("_", "").equalsIgnoreCase(expected.replace("_", ""));
    }

    @Override
    public TodoSummaryResDTO getSummary(String userId) throws Exception {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        List<Todo> todos = todoRepository.findAll(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("userId"), userId));

        // Debug: print all status and priority values
        System.out.println("[DEBUG] Todo statuses:");
        todos.forEach(t -> System.out.println("  status=" + t.getStatus() + ", priority=" + t.getPriority()));

        // Calculate summary statistics
        int total = todos.size();

        // Count by status
        long pending = todos.stream()
                .filter(t -> enumEquals(t.getStatus().name(), "PENDING"))
                .count();
        long inProgress = todos.stream()
                .filter(t -> enumEquals(t.getStatus().name(), "IN_PROGRESS"))
                .count();
        long completed = todos.stream()
                .filter(t -> enumEquals(t.getStatus().name(), "COMPLETED"))
                .count();
        long cancelled = todos.stream()
                .filter(t -> enumEquals(t.getStatus().name(), "CANCELLED"))
                .count();

        // Count by priority
        long urgent = todos.stream()
                .filter(t -> enumEquals(t.getPriority().name(), "URGENT"))
                .count();
        long high = todos.stream()
                .filter(t -> enumEquals(t.getPriority().name(), "HIGH"))
                .count();
        long medium = todos.stream()
                .filter(t -> enumEquals(t.getPriority().name(), "MEDIUM"))
                .count();
        long low = todos.stream()
                .filter(t -> enumEquals(t.getPriority().name(), "LOW"))
                .count();

        // Debug: print all summary counts
        System.out.println("[DEBUG] pending=" + pending + ", inProgress=" + inProgress + ", completed=" + completed
                + ", cancelled=" + cancelled);
        System.out.println("[DEBUG] urgent=" + urgent + ", high=" + high + ", medium=" + medium + ", low=" + low);

        // Calculate overdue, due today, and due this week
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);
        LocalDateTime endOfWeek =
                now.plusDays(7 - now.getDayOfWeek().getValue()).toLocalDate().atTime(23, 59, 59);

        long overdue = todos.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isBefore(startOfDay)
                        && !enumEquals(t.getStatus().name(), "COMPLETED"))
                .count();

        long dueToday = todos.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isAfter(startOfDay)
                        && t.getDueDate().isBefore(endOfDay))
                .count();

        long dueThisWeek = todos.stream()
                .filter(t -> t.getDueDate() != null
                        && t.getDueDate().isAfter(endOfDay)
                        && t.getDueDate().isBefore(endOfWeek))
                .count();

        return TodoSummaryResDTO.builder()
                .total(total)
                .byStatus(TodoSummaryResDTO.StatusCount.builder()
                        .pending((int) pending)
                        .inProgress((int) inProgress)
                        .completed((int) completed)
                        .cancelled((int) cancelled)
                        .build())
                .byPriority(TodoSummaryResDTO.PriorityCount.builder()
                        .urgent((int) urgent)
                        .high((int) high)
                        .medium((int) medium)
                        .low((int) low)
                        .build())
                .overdue((int) overdue)
                .dueToday((int) dueToday)
                .dueThisWeek((int) dueThisWeek)
                .build();
    }

    @Override
    public TodoResponseDTO addSubtask(String todoId, String title) throws Exception {
        Todo todo = todoRepository.findById(todoId).orElseThrow(() -> new EntityNotFoundException("Todo not found"));
        TodoSubtask subtask =
                TodoSubtask.builder().title(title).completed(false).build();
        todo.addSubtask(subtask);
        return todoMapper.todoToTodoResponse(todoRepository.save(todo));
    }

    @Override
    public TodoResponseDTO toggleSubtaskCompletion(String subtaskId) throws Exception {
        TodoSubtask subtask = subtaskRepository
                .findById(subtaskId)
                .orElseThrow(() -> new EntityNotFoundException("Subtask not found"));
        subtask.setCompleted(!subtask.isCompleted());
        subtaskRepository.save(subtask);
        return todoMapper.todoToTodoResponse(subtask.getTodo());
    }
}
