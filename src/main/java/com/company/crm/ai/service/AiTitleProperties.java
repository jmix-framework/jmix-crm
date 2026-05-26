package com.company.crm.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AI conversation title generation.
 */
@Component
@ConfigurationProperties(prefix = "crm.ai.title")
public class AiTitleProperties {

    public static final String SKIP_MARKER = "NEW_CONVERSATION";

    private boolean enabled = true;
    private int maxLength = 80;
    private int maxContextMessages = 6;
    private int minUserMessages = 1;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxContextMessages() {
        return maxContextMessages;
    }

    public void setMaxContextMessages(int maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }

    public int getMinUserMessages() {
        return minUserMessages;
    }

    public void setMinUserMessages(int minUserMessages) {
        this.minUserMessages = minUserMessages;
    }
}
