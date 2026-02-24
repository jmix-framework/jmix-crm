package com.company.crm.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crm.ai.title")
public class AiConversationTitleProperties {

    private String modelId = "arn:aws:bedrock:eu-central-1:279497544307:inference-profile/eu.anthropic.claude-3-5-haiku-20241022-v1:0";
    private Double temperature = 0.0;
    private Integer maxTokens = 32;
    private String systemPrompt = """
            You generate concise CRM conversation titles.
            Return only the title text.
            Do not use quotes, markdown, or punctuation at the end.
            Keep it specific and under 8 words.
            """;

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}
