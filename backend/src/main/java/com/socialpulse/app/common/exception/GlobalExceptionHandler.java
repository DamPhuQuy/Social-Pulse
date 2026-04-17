package com.socialpulse.app.common.exception;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.socialpulse.app.common.status.AppCode;
import com.socialpulse.app.common.status.UserCode;

@RestControllerAdvice // catch global exception, appfasely all controller
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // handle AppException
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {

        AppCode code = ex.getErrorCode();

        return buildErrorResponse(code.getCode(), code.getMessage());
    }

    // handle @Valid fails
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = "Invalid request payload";
        FieldError firstFieldError = ex.getBindingResult().getFieldError();
        if (firstFieldError != null && firstFieldError.getDefaultMessage() != null) {
            message = firstFieldError.getDefaultMessage();
        }

        return buildErrorResponse(400, message);
    }

    // wrong JSON format handler
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        logger.error("Malformed JSON payload", ex);
        return buildErrorResponse(400, "Malformed JSON request body");
    }

    // database fails
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation", ex);
        String details = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String normalizedDetails = details == null ? "" : details.toLowerCase();

        // handle duplicate or unique constraint
        if (normalizedDetails.contains("unique") || normalizedDetails.contains("duplicate")) {
            return buildErrorResponse(400, UserCode.USER_ALREADY_EXISTS.getMessage());
        }

        return buildErrorResponse(400, "Invalid data for one or more required fields");
    }

    // handle AccessDeniedException explicitly
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        logger.error("Access denied error", ex);
        return buildErrorResponse(403, "Access Denied: You do not have permission to access this resource");
    }

    // catch all unhandled exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        logger.error("Unhandled exception", ex);

        return buildErrorResponse(500, "Internal Server Error");
    }

    // helper method to build error response with
    private ResponseEntity<ErrorResponse> buildErrorResponse(int status, String message) {
        return ResponseEntity
                .status(status)
                .body(ErrorResponse.builder()
                        .status(status)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}

