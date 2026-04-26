package com.payment.orchestrator.exception;

import com.payment.shared.exception.DuplicateRequestException;
import com.payment.shared.exception.PaymentException;
import com.payment.shared.exception.PaymentNotFoundException;
import com.payment.shared.exception.RoutingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(errorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateRequestException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RoutingException.class)
    public ResponseEntity<ErrorResponse> handleRouting(RoutingException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(errorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex) {
        HttpStatus status = "CANCELLATION_NOT_ALLOWED".equals(ex.getErrorCode())
                ? HttpStatus.CONFLICT
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status)
                .body(errorResponse(ex.getErrorCode(), ex.getMessage()));
    }

    // Bean validation failures (@Valid on request body)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            fieldErrors.put(field, error.getDefaultMessage());
        });
        var response = new ErrorResponse("VALIDATION_FAILED",
                "Request validation failed", Instant.now(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private ErrorResponse errorResponse(String code, String message) {
        return new ErrorResponse(code, message, Instant.now(), null);
    }

    public record ErrorResponse(
            String code,
            String message,
            Instant timestamp,
            Map<String, String> fieldErrors
    ) {}
}
