
package com.example.sellforeignservice.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
    // Shortcut cho 400
    public BusinessException(String code, String message) {
        this(HttpStatus.BAD_REQUEST, code, message);
    }
}