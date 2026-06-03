package com.company.crm.ai.service;

import org.springframework.util.StringUtils;

public final class AiMediaNameSanitizer {

    private static final int MAX_MEDIA_NAME_LENGTH = 96;
    private static final String FALLBACK_MEDIA_NAME = "uploaded-file";

    private AiMediaNameSanitizer() {
    }

    public static String sanitize(String fileName) {
        String sanitized = (StringUtils.hasText(fileName) ? fileName : FALLBACK_MEDIA_NAME)
                .replaceAll("[^A-Za-z0-9\\s\\-()\\[\\]]", "_")
                .replaceAll("\\s+", " ")
                .trim();

        if (!StringUtils.hasText(sanitized)) {
            sanitized = FALLBACK_MEDIA_NAME;
        }
        return sanitized.length() > MAX_MEDIA_NAME_LENGTH
                ? sanitized.substring(0, MAX_MEDIA_NAME_LENGTH)
                : sanitized;
    }
}
