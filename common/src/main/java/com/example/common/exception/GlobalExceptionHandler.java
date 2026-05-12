package com.example.common.exception;

import com.example.common.config.api.ApiCode;
import com.example.common.config.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(
            BusinessException ex,
            HttpServletRequest request
    ) {

        log.warn(
                "[{}] {} - {}",
                ex.getCode(),
                request.getRequestURI(),
                ex.getMessage()
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(
                        ApiResponse.error(
                                ex.getMessage(),
                                ex.getCode(),
                                ex.getDetail()
                        )
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "Validation failed",
                        ApiCode.MISSING_FIELD
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(
            HttpMessageNotReadableException ex
    ) {

        return ResponseEntity.badRequest().body(
                ApiResponse.error(
                        "Invalid request body",
                        ApiCode.INVALID_REQUEST
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(
            Exception ex
    ) {

        log.error("Unhandled exception", ex);

        return ResponseEntity.internalServerError().body(
                ApiResponse.error(
                        "Internal error",
                        ApiCode.INTERNAL_ERROR
                )
        );
    }


//    //check currency  hop le hay khong
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<ApiResponse<ApiResponse.ErrorData>> handleNotReadable(
//            HttpMessageNotReadableException ex, HttpServletRequest request) {
//
//        String msg = (ex.getMessage() != null && ex.getMessage().contains("Currency"))
//                ? "Invalid currency code"
//                : "Malformed request body";
//
//        log.warn("Bad request: {}", ex.getMessage());
//
//        return ResponseEntity.badRequest().body(
//                ApiResponse.error("BAD_REQUEST", msg)
//        );
//    }
//
//    // lỗi k lường trước
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<ApiResponse.ErrorData>> handleAll(
//            Exception ex, HttpServletRequest request) {
//
//        log.error("Unexpected error at [{}]", request.getRequestURI(), ex);
//
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
//                ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred")
//        );
//    }
}
