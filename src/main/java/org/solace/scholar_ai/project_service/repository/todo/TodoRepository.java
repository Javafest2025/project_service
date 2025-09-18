package org.solace.scholar_ai.project_service.repository.todo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.solace.scholar_ai.project_service.constant.todo.TodoCategory;
import org.solace.scholar_ai.project_service.constant.todo.TodoPriority;
import org.solace.scholar_ai.project_service.constant.todo.TodoStatus;
import org.solace.scholar_ai.project_service.model.todo.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoRepository extends JpaRepository<Todo, String>, JpaSpecificationExecutor<Todo> {

    // Find todos by status
    List<Todo> findByStatusIn(List<TodoStatus> statuses);

    // Find todos by priority
    List<Todo> findByPriorityIn(List<TodoPriority> priorities);

    // Find todos by category
    List<Todo> findByCategoryIn(List<TodoCategory> categories);

    // Find todos by due date range
    List<Todo> findByDueDateBetween(LocalDateTime start, LocalDateTime end);

    // Find overdue todos
    @Query("SELECT t FROM Todo t WHERE t.dueDate < :now AND t.status != 'completed'")
    List<Todo> findOverdueTodos(@Param("now") LocalDateTime now);

    // Find todos due today
    @Query("SELECT t FROM Todo t WHERE DATE(t.dueDate) = DATE(:today)")
    List<Todo> findTodosDueToday(@Param("today") LocalDateTime today);

    // Find todos due this week
    @Query("SELECT t FROM Todo t WHERE t.dueDate BETWEEN :startOfWeek AND :endOfWeek")
    List<Todo> findTodosDueThisWeek(
            @Param("startOfWeek") LocalDateTime startOfWeek, @Param("endOfWeek") LocalDateTime endOfWeek);

    // Search todos by title or description
    @Query("SELECT t FROM Todo t WHERE " + "LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR "
            + "LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Todo> searchTodos(@Param("search") String search);

    // Find todos by tags
    @Query("SELECT DISTINCT t FROM Todo t JOIN t.tags tag WHERE tag IN :tags")
    List<Todo> findByTagsIn(@Param("tags") Set<String> tags);

    // Find todos by related project
    List<Todo> findByRelatedProjectId(String projectId);

    // Count todos by status
    long countByStatus(TodoStatus status);

    // Count todos by priority
    long countByPriority(TodoPriority priority);

    // Find todo IDs by project ID
    @Query("SELECT t.id FROM Todo t WHERE t.relatedProjectId = :projectId")
    List<String> findIdsByProjectId(@Param("projectId") String projectId);

    // Delete todos by project ID
    void deleteByRelatedProjectId(String projectId);
}
