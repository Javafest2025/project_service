package org.solace.scholar_ai.project_service.dto.todo.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class TodoCreateReqDTO {
    @NotBlank(message = "User ID is required")
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required") private TodoPriority priority;

    @NotNull(message = "Category is required") private TodoCategory category;

    @JsonProperty("due_date")
    private String dueDate;

    @JsonProperty("estimated_time")
    private Integer estimatedTime;

    @JsonProperty("related_project_id")
    private String relatedProjectId;

    private Set<String> tags;

    private List<SubtaskRequest> subtasks;

    private List<ReminderRequest> reminders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtaskRequest {
        @NotBlank(message = "Subtask title is required")
        private String title;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReminderRequest {
        @JsonProperty("remind_at")
        @NotBlank(message = "Reminder time is required")
        private String remindAt;

        @NotBlank(message = "Reminder message is required")
        private String message;
    }
}
