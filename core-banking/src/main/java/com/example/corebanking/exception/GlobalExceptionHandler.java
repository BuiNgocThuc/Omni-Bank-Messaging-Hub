package com.example.corebanking.exception;

import com.example.common.config.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ApiResponse.ErrorData>> handleBusiness(
            BusinessException ex, HttpServletRequest request) {

        log.warn("[{}] {} - {}", ex.getCode(), request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(ex.getStatus()).body(
                ApiResponse.error(ex.getStatus().toString(), ex.getMessage(),ex.getCode())
        );
    }


}
