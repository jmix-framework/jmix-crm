package com.company.crm.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationTitleServiceConfigTest {

    @Test
    void titleGenerationCanBeDisabledThroughProperties() {
        AiTitleProperties titleProperties = new AiTitleProperties();

        assertThat(titleProperties.isEnabled()).isTrue();

        titleProperties.setEnabled(false);

        assertThat(titleProperties.isEnabled()).isFalse();
    }

    @Test
    void titleOptionsUseSmallModelWhenConfigured() {
        AiSmallModelProperties smallModelProperties = new AiSmallModelProperties();
        smallModelProperties.setModelId("gpt-test-small");

        OpenAiChatOptions options = AiConversationTitleService.buildTitleOptions(smallModelProperties);

        assertThat(options.getModel()).isEqualTo("gpt-test-small");
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getMaxCompletionTokens()).isEqualTo(32);
        assertThat(options.getServiceTier()).isNull();
    }

    @Test
    void titleOptionsDoNotSetModelWhenUnset() {
        OpenAiChatOptions options = AiConversationTitleService.buildTitleOptions(new AiSmallModelProperties());

        assertThat(options.getModel()).isNull();
        assertThat(options.getTemperature()).isEqualTo(0.0);
        assertThat(options.getMaxCompletionTokens()).isEqualTo(32);
        assertThat(options.getServiceTier()).isNull();
    }

    @Test
    void systemPromptUsesConfiguredSkipMarker() {
        AiTitleProperties titleProperties = new AiTitleProperties();
        ByteArrayResource prompt = new ByteArrayResource(
                "Return {skipMarker} for vague messages.".getBytes(StandardCharsets.UTF_8));

        String renderedPrompt = AiConversationTitleService.renderSystemPrompt(prompt, titleProperties);

        assertThat(renderedPrompt).isEqualTo("Return NEW_CONVERSATION for vague messages.");
    }
}
