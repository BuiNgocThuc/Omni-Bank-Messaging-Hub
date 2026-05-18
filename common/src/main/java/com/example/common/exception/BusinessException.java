package com.example.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;
    private final String detail;

    public BusinessException(
            HttpStatus status,
            String code,
            String message
    ) {
        super(message);
        this.code = code;
        this.status = status;
        this.detail = message;
    }

    public BusinessException(
            HttpStatus status,
            String code,
            String message,
            String detail
    ) {
        super(message);
        this.code = code;
        this.status = status;
        this.detail = detail;
    }

    // default BAD_REQUEST
    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }
}