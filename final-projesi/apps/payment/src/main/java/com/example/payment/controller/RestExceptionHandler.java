package com.example.payment.controller;

import com.example.lib.exception.BaseGlobalExceptionHandler;
import com.example.lib.exception.InsufficientStockException;
import com.example.lib.web.ApiErrorResponses;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler extends BaseGlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(
            IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI(), null));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest req) {
        return ResponseEntity.unprocessableEntity().body(
                ApiErrorResponses.errorBody(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI(), null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST,
                        "Missing required header: " + ex.getHeaderName(),
                        req.getRequestURI(), null));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST,
                        "Missing required parameter: " + ex.getParameterName(),
                        req.getRequestURI(), null));
    }

    @Override
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, Object>> handleStock(
            InsufficientStockException ex, HttpServletRequest req) {
        return super.handleStock(ex, req);
    }

    @Override
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        return super.handleValidation(ex, req);
    }

    @Override
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return super.handleUnreadable(ex, req);
    }

    @Override
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return super.handleTypeMismatch(ex, req);
    }

    @Override
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest req) {
        return super.handleResponseStatus(ex, req);
    }

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError().body(
                ApiErrorResponses.errorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Internal payment error", req.getRequestURI(), null));
    }
}
