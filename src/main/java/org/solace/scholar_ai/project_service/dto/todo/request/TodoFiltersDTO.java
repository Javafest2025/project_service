package org.solace.scholar_ai.project_service.dto.todo.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TodoFiltersDTO {
    private String status;
    private String priority;
    private String category;
    private String search;
}
