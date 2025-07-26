package com.example.adowebhook.handler;

import org.springframework.http.HttpStatus;

public class CustomExp extends RuntimeException {
    private final HttpStatus status;

    public CustomExp(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
