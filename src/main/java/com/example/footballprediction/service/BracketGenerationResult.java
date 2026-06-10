package com.example.footballprediction.service;

import java.util.List;

public class BracketGenerationResult {

    private final List<String> messages;
    private final boolean adminReviewRequired;

    public BracketGenerationResult(List<String> messages, boolean adminReviewRequired) {
        this.messages = messages;
        this.adminReviewRequired = adminReviewRequired;
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean isAdminReviewRequired() {
        return adminReviewRequired;
    }
}
