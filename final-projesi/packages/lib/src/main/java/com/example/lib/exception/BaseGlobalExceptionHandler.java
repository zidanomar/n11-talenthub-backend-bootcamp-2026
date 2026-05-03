package com.example.lib.exception;

import com.example.lib.web.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

public abstract class BaseGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BaseGlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleStock(
            InsufficientStockException ex, HttpServletRequest req) {
        return ResponseEntity.unprocessableEntity().body(
                ApiErrorResponses.errorBody(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> fieldErrors = ApiErrorResponses.fieldErrorList(ex.getBindingResult().getFieldErrors());
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST, "Malformed or missing request body", req.getRequestURI(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST, ApiErrorResponses.typeMismatchMessage(ex), req.getRequestURI(), null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = ApiErrorResponses.status(ex);
        return ResponseEntity.status(status).body(
                ApiErrorResponses.errorBody(status, ex.getReason(), req.getRequestURI(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return ResponseEntity.internalServerError().body(
                ApiErrorResponses.errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req.getRequestURI(), null));
    }
}
