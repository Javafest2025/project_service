package org.solace.scholar_ai.project_service.util.response;

import lombok.Getter;
import org.solace.scholar_ai.project_service.dto.response.APIErrorResponse;

/**
 * Wrapper class for API responses that can handle both success and error cases.
 */
@Getter
public class ResponseWrapper<T> {
    private final boolean success;
    private final T data;
    private final APIErrorResponse error;

    private ResponseWrapper(boolean success, T data, APIErrorResponse error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(true, data, null);
    }

    public static <T> ResponseWrapper<T> error(APIErrorResponse error) {
        return new ResponseWrapper<>(false, null, error);
    }
}
