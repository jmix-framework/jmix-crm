package com.company.crm.ai.model;

import org.springframework.util.StringUtils;

public record AiUiStatusUpdate(String message, String resultSnippet) {

    public AiUiStatusUpdate(String message) {
        this(message, null);
    }

    public boolean isCompleted() {
        return StringUtils.hasText(resultSnippet);
    }
}
