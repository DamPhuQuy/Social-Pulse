package com.socialpulse.app.common.exception;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // handle AppException
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {

        ErrorCode code = ex.getErrorCode();

        return ResponseEntity
                .status(code.getCode())
                .body(ErrorResponse.builder()
                        .status(code.getCode())
                        .message(code.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {

        return ResponseEntity
                .status(500)
                .body(ErrorResponse.builder()
                        .status(500)
                        .message("Internal Server Error")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}

