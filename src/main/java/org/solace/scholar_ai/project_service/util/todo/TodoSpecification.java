package org.solace.scholar_ai.project_service.util.todo;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.solace.scholar_ai.project_service.constant.todo.TodoCategory;
import org.solace.scholar_ai.project_service.constant.todo.TodoPriority;
import org.solace.scholar_ai.project_service.constant.todo.TodoStatus;
import org.solace.scholar_ai.project_service.dto.todo.request.TodoFiltersReqDTO;
import org.solace.scholar_ai.project_service.model.todo.Todo;
import org.springframework.data.jpa.domain.Specification;

public class TodoSpecification {

    public static Specification<Todo> fromFilters(TodoFiltersReqDTO filters, String userId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("userId"), userId));

            if (filters.getStatus() != null && !filters.getStatus().isEmpty()) {
                List<TodoStatus> statuses = filters.getStatus().stream()
                        .map(String::toUpperCase)
                        .map(TodoStatus::valueOf)
                        .collect(Collectors.toList());
                predicates.add(root.get("status").in(statuses));
            }

            if (filters.getPriority() != null && !filters.getPriority().isEmpty()) {
                List<TodoPriority> priorities = filters.getPriority().stream()
                        .map(String::toUpperCase)
                        .map(TodoPriority::valueOf)
                        .collect(Collectors.toList());
                predicates.add(root.get("priority").in(priorities));
            }

            if (filters.getCategory() != null && !filters.getCategory().isEmpty()) {
                List<TodoCategory> categories = filters.getCategory().stream()
                        .map(String::toUpperCase)
                        .map(TodoCategory::valueOf)
                        .collect(Collectors.toList());
                predicates.add(root.get("category").in(categories));
            }

            if (filters.getDueDateStart() != null) {
                LocalDateTime start = LocalDateTime.parse(filters.getDueDateStart());
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), start));
            }

            if (filters.getDueDateEnd() != null) {
                LocalDateTime end = LocalDateTime.parse(filters.getDueDateEnd());
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), end));
            }

            if (filters.getTags() != null && !filters.getTags().isEmpty()) {
                Join<Object, Object> tagsJoin = root.join("tags");
                predicates.add(tagsJoin.in(filters.getTags()));
            }

            if (filters.getProjectId() != null) {
                predicates.add(cb.equal(root.get("relatedProjectId"), filters.getProjectId()));
            }

            if (filters.getSearch() != null && !filters.getSearch().isEmpty()) {
                String keyword = "%" + filters.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
