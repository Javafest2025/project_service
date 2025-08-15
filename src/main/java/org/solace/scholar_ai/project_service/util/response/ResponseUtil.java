package org.solace.scholar_ai.project_service.util.response;

import java.time.LocalDateTime;
import org.solace.scholar_ai.project_service.dto.response.APIErrorResponse;
import org.solace.scholar_ai.project_service.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility class for creating standardized API responses.
 */
public class ResponseUtil {

    private ResponseUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates an APIErrorResponse with the given parameters.
     */
    public static APIErrorResponse createErrorResponse(HttpStatus status, ErrorCode errorCode, String message) {
        return APIErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(errorCode.name())
                .message(message)
                .suggestion(errorCode.getSuggestion())
                .build();
    }

    /**
     * Creates a success response with data.
     */
    public static <T> ResponseEntity<ResponseWrapper<T>> success(T data) {
        return ResponseEntity.ok(ResponseWrapper.success(data));
    }

    /**
     * Creates a success response with data and custom status.
     */
    public static <T> ResponseEntity<ResponseWrapper<T>> success(T data, HttpStatus status) {
        return new ResponseEntity<>(ResponseWrapper.success(data), status);
    }

    /**
     * Creates an error response.
     */
    public static <T> ResponseEntity<ResponseWrapper<T>> error(HttpStatus status, ErrorCode errorCode, String message) {
        return ResponseEntity.status(status)
                .body(ResponseWrapper.error(createErrorResponse(status, errorCode, message)));
    }
}
