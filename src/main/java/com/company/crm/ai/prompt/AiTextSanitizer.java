package com.company.crm.ai.prompt;

import org.springframework.util.StringUtils;

import java.util.Optional;

public final class AiTextSanitizer {

    private AiTextSanitizer() {
    }

    public static Optional<String> normalizeSingleLine(String text, int maxLength) {
        return Optional.ofNullable(text)
                .filter(StringUtils::hasText)
                .map(value -> value.replaceAll("[\\n\\r]+", " ").trim())
                .map(value -> truncate(value, maxLength));
    }

    public static Optional<String> normalizeTextBlock(String text, int maxLength) {
        return Optional.ofNullable(text)
                .filter(StringUtils::hasText)
                .map(String::strip)
                .map(value -> truncate(value, maxLength));
    }

    public static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }
}
