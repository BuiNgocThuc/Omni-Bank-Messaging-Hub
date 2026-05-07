package com.example.common.config.api;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    private String path;

    public ApiResponse(boolean success, String code, String message, T data, String path) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        this.path = path;
    }

    public static <T> ApiResponse<T> success(String code, String message, T data, String path) {
        return new ApiResponse<>(true, code, message, data, path);
    }

    public static <T> ApiResponse<T> error(String code, String message, String path) {
        return new ApiResponse<>(false, code, message, null, path);
    }


}
