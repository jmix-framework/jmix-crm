package com.company.crm.ai.service;

import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// TODO: remove, inline
@Component
public class AiSmallModelOptionsFactory {

    private final AiSmallModelProperties properties;

    public AiSmallModelOptionsFactory(AiSmallModelProperties properties) {
        this.properties = properties;
    }

    public OpenAiChatOptions.Builder builder() {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (StringUtils.hasText(properties.getModelId())) {
            builder.model(properties.getModelId());
        }
        // TODO: rausschmeissen
        if (StringUtils.hasText(properties.getServiceTier())) {
            builder.serviceTier(properties.getServiceTier());
        }
        return builder;
    }
}
