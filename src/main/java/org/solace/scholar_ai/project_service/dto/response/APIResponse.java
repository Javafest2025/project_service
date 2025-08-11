package org.solace.scholar_ai.project_service.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class APIResponse<T> {
    @Builder.Default
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;
    private String message;
    private T data;

    public static <T> APIResponse<T> success(int status, String message, T data) {
        return APIResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> APIResponse<T> error(int status, String message, T data) {
        return APIResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}
