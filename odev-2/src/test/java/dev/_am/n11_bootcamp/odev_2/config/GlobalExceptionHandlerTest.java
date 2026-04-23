package dev._am.n11_bootcamp.odev_2.config;

import dev._am.n11_bootcamp.odev_2.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_shouldReturn400WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleIllegalArgument(new IllegalArgumentException("Username already exists"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Username already exists", response.getBody().getMessage());
    }

    @Test
    void handleNotFound_shouldReturn404WithMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new NoResourceFoundException(HttpMethod.GET, "/auth/signupa", "/auth/signupa"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().getStatus());
        assertNotNull(response.getBody().getMessage());
    }

    @Test
    void handleGenericException_shouldReturn500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().getStatus());
    }
}
