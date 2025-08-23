package org.solace.scholar_ai.project_service.dto.todo.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.solace.scholar_ai.project_service.constant.todo.TodoStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoStatusUpdateReqDTO {
    @NotNull(message = "Status is required") private TodoStatus status;
}
