package org.solace.scholar_ai.project_service.repository.todo;

import java.util.List;
import org.solace.scholar_ai.project_service.model.todo.TodoSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoSubtaskRepository extends JpaRepository<TodoSubtask, String> {

    /**
     * Delete subtasks by todo IDs
     */
    void deleteByTodoIdIn(List<String> todoIds);
}
