package com.example.common.exception;

import com.example.common.config.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {



    //loi nghiệp vụ
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        log.warn("[{}] {} - {}", ex.getCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(ex.getStatus()).body(
                ApiResponse.error(ex.getCode(), ex.getMessage(), request.getRequestURI())
        );
    }


    //check currency  hop le hay khong
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String msg = (ex.getMessage() != null && ex.getMessage().contains("Currency"))
                ? "Invalid currency code"
                : "Malformed request body";

        log.warn("Bad request: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(
                ApiResponse.error("BAD_REQUEST", msg, request.getRequestURI())
        );
    }

    // lỗi k lường trước
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAll(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error at [{}]", request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred",
                        request.getRequestURI())
        );
    }
}
