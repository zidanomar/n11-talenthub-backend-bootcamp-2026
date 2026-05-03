package com.example.cart.exception;

import com.example.lib.exception.BaseGlobalExceptionHandler;
import com.example.lib.exception.InsufficientStockException;
import com.example.lib.web.ApiErrorResponses;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class RestExceptionHandler extends BaseGlobalExceptionHandler {

    private static final Pattern SERVICE_URL_PATTERN = Pattern.compile("http://([^/]+)/");

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeign(
            FeignException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.status());
        if (status == null) status = HttpStatus.BAD_GATEWAY;
        String message;
        if (status == HttpStatus.NOT_FOUND) {
            message = "Product not found";
        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            Matcher m = SERVICE_URL_PATTERN.matcher(ex.getMessage());
            message = (m.find() ? m.group(1) : "Upstream service") + " is not available";
        } else {
            message = ex.getMessage();
        }
        return ResponseEntity.status(status).body(
                ApiErrorResponses.errorBody(status, message, req.getRequestURI(), null));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(
                ApiErrorResponses.errorBody(HttpStatus.BAD_REQUEST,
                        "Missing required header: " + ex.getHeaderName(),
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
        return super.handleGeneric(ex, req);
    }
}
