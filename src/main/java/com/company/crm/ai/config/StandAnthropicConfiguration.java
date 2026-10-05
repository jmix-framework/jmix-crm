package com.company.crm.ai.config;

import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import com.embabel.common.ai.model.LlmOptions;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Direct Anthropic connection for the isolated UI comparison stand only. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "crm.dynmodel.provider", havingValue = "anthropic")
public class StandAnthropicConfiguration {
    @Bean
    SpringAiLlmService standClaudeLlm(@Value("${crm.dynmodel.model}") String model,
                                    @Value("${stand.anthropic.api-key:}") String apiKey,
                                    @Value("${crm.dynmodel.max-output-tokens}") int maxOutputTokens) {
        ChatModel chatModel;
        if (apiKey.isBlank()) {
            chatModel = prompt -> {
                throw new IllegalStateException("The Claude preview stand needs ANTHROPIC_API_KEY; no request was sent.");
            };
        } else {
            chatModel = AnthropicChatModel.builder().options(AnthropicChatOptions.builder()
                    .model(model).apiKey(apiKey).baseUrl("https://api.anthropic.com")
                    .maxTokens(maxOutputTokens).maxRetries(0).timeout(Duration.ofMinutes(4))
                    .thinkingDisabled().build()).build();
        }
        return new SpringAiLlmService(model, "Anthropic", chatModel,
                (options, name) -> convertOptions(options, name, maxOutputTokens));
    }

    static AnthropicChatOptions convertOptions(LlmOptions options, String model, int maxOutputTokens) {
        var builder = AnthropicChatOptions.builder().model(model).maxTokens(maxOutputTokens)
                .maxRetries(0).thinkingDisabled();
        if (options.getTimeout() != null) {
            builder.timeout(options.getTimeout());
        }
        return builder.build();
    }
}
