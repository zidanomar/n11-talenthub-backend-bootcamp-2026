package com.example.user.exception;

import com.example.lib.web.ApiErrorResponses;
import com.example.lib.exception.BaseGlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<Map<String, Object>> handleKeycloak(
            KeycloakException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(
                ApiErrorResponses.errorBody(status, ex.getMessage(), req.getRequestURI(), null));
    }

    @Override
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(LinkedHashMap::new,
                        (errors, field) -> errors.put(field.getField(), ApiErrorResponses.message(field)),
                        Map::putAll);
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST, "Validation failed", req.getRequestURI(), fieldErrors));
    }
}
