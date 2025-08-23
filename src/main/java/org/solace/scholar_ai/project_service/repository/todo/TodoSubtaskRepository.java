package org.solace.scholar_ai.project_service.repository.todo;

import org.solace.scholar_ai.project_service.model.todo.TodoSubtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TodoSubtaskRepository extends JpaRepository<TodoSubtask, String> {}
