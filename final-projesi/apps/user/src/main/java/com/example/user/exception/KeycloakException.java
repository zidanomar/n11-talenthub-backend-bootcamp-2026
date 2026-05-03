package com.example.user.exception;

import lombok.Getter;

@Getter
public class KeycloakException extends RuntimeException {
    private final int status;

    public KeycloakException(String message, int status) {
        super(message);
        this.status = status;
    }
}
