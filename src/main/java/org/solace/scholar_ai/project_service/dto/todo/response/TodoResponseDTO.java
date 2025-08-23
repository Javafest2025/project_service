package org.solace.scholar_ai.project_service.dto.todo.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.constant.todo.TodoCategory;
import org.solace.scholar_ai.project_service.constant.todo.TodoPriority;
import org.solace.scholar_ai.project_service.constant.todo.TodoStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponseDTO {
    private String id;
    private String title;
    private String description;
    private TodoStatus status;
    private TodoPriority priority;
    private TodoCategory category;

    @JsonProperty("due_date")
    private String dueDate; // ISO string format

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("completed_at")
    private String completedAt;

    @JsonProperty("estimated_time")
    private Integer estimatedTime;

    @JsonProperty("actual_time")
    private Integer actualTime;

    @JsonProperty("related_project_id")
    private String relatedProjectId;

    @JsonProperty("related_paper_id")
    private String relatedPaperId;

    private Set<String> tags;
    private List<SubtaskResponse> subtasks;
    private List<ReminderResponse> reminders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtaskResponse {
        private String id;
        private String title;
        private boolean completed;

        @JsonProperty("created_at")
        private String createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReminderResponse {
        private String id;

        @JsonProperty("remind_at")
        private String remindAt;

        private String message;
        private boolean sent;
    }
}
