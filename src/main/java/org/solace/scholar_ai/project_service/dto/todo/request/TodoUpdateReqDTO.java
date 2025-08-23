package org.solace.scholar_ai.project_service.dto.todo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.constant.todo.TodoCategory;
import org.solace.scholar_ai.project_service.constant.todo.TodoPriority;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoUpdateReqDTO {
    private String title;
    private String description;
    private TodoPriority priority;
    private TodoCategory category;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("estimated_time")
    private Integer estimatedTime;

    @JsonProperty("related_project_id")
    private String relatedProjectId;

    private Set<String> tags;
    private List<TodoCreateReqDTO.SubtaskRequest> subtasks;
    private List<TodoCreateReqDTO.ReminderRequest> reminders;
}
