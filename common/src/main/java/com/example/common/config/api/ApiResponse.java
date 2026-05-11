package com.example.common.config.api;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private Instant timestamp;
    private String status;
    private T data;

    public ApiResponse(String status, T data) {
        this.timestamp = Instant.now();
        this.status = status;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(String status, T data) {
        return new ApiResponse<>(status, data);
    }

    public static ApiResponse<ApiResponse.ErrorData> error(String status, String message) {
        return new ApiResponse<>(status, new ErrorData(message));
    }


    @Getter
    @Setter
    @AllArgsConstructor
    public static class ErrorData {
        private String message;
    }
}
