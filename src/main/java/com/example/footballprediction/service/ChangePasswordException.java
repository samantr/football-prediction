package com.example.footballprediction.service;

public class ChangePasswordException extends RuntimeException {

    private final String field;

    public ChangePasswordException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
