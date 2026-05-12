package com.example.common.config.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
            timezone = "UTC"
    )
    private Instant timestamp;

    private String status;

    private String message;

    private T data;

    private ErrorData error;

    public static <T> ApiResponse<T> success(
            String message,
            T data
    ) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status("SUCCESS")
                .message(message)
                .data(data)
                .build();
    }

    public static ApiResponse<Void> error(
            String message,
            String code
    ) {
        return ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status("ERROR")
                .message(message)
                .error(new ErrorData(code, message))
                .build();
    }

    public static ApiResponse<Void> error(
            String message,
            String code,
            String detail
    ) {
        return ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status("ERROR")
                .message(message)
                .error(new ErrorData(code, detail))
                .build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {

        private String code;

        private String message;
    }
}