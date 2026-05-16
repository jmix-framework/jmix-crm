package com.company.crm.ai.model;

public record AiUiStatusUpdate(String message, String resultSnippet) {

    public AiUiStatusUpdate(String message) {
        this(message, null);
    }

    public boolean isCompleted() {
        return resultSnippet != null && !resultSnippet.isBlank();
    }
}
