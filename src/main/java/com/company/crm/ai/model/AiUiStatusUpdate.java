package com.company.crm.ai.model;

public record AiUiStatusUpdate(String message, String resultSnippet) {

    public AiUiStatusUpdate(String message) {
        this(message, null);
    }

    public boolean isCompleted() {
        return org.springframework.util.StringUtils.hasText(resultSnippet);
    }
}
