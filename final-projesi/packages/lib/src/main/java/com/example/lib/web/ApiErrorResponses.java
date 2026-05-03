package com.example.lib.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

public final class ApiErrorResponses {
    private ApiErrorResponses() {
    }

    public static Map<String, Object> errorBody(
            HttpStatus status, String message, String path, Object errors) {
        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", Instant.now());
        body.put("status", status != null ? status.value() : 500);
        body.put("error", status != null ? status.getReasonPhrase() : "Error");
        body.put("message", message);
        body.put("path", path);
        if (errors != null) {
            body.put("errors", errors);
        }
        return body;
    }

    public static List<Map<String, String>> fieldErrorList(List<FieldError> fields) {
        return fields.stream()
                .map(field -> Map.of("field", field.getField(), "message", message(field)))
                .toList();
    }

    public static Map<String, String> fieldErrorMap(List<FieldError> fields) {
        return fields.stream()
                .collect(LinkedHashMap::new,
                        (errors, field) -> errors.put(field.getField(), message(field)),
                        Map::putAll);
    }

    public static String message(FieldError field) {
        return field.getDefaultMessage() != null ? field.getDefaultMessage() : "invalid value";
    }

    public static String typeMismatchMessage(MethodArgumentTypeMismatchException ex) {
        return "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
    }

    public static HttpStatus status(ResponseStatusException ex) {
        var status = HttpStatus.resolve(ex.getStatusCode().value());
        return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
