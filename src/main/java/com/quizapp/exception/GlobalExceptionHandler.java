package com.quizapp.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(QuizAppException.class)
    public ResponseEntity<ErrorResponse> handleQuizAppException(QuizAppException ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ex.getErrorCode().getCode())
            .message(ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getHttpStatus() == 404) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex.getHttpStatus() == 409) {
            status = HttpStatus.CONFLICT;
        } else if (ex.getHttpStatus() == 400) {
            status = HttpStatus.BAD_REQUEST;
        }
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
            .message(message)
            .timestamp(System.currentTimeMillis())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        String message = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
            .collect(Collectors.joining("; "));

        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ErrorCode.VALIDATION_ERROR.getCode())
            .message(message)
            .timestamp(System.currentTimeMillis())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
            .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .message("An unexpected error occurred: " + ex.getMessage())
            .timestamp(System.currentTimeMillis())
            .path(request.getDescription(false).replace("uri=", ""))
            .build();
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
