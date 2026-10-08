package com.company.crm.ai.config;

import com.embabel.agent.openai.Gpt5ChatOptionsConverter;
import com.embabel.agent.openai.StandardOpenAiOptionsConverter;
import com.embabel.agent.spi.support.springai.SpringAiLlmService;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class DynamicModelAgentConfiguration {

    @Bean
    @ConditionalOnProperty(name = "crm.dynmodel.provider", havingValue = "openrouter", matchIfMissing = true)
    SpringAiLlmService dynamicModelLlm(@Value("${crm.dynmodel.model}") String model,
                                     @Value("${crm.dynmodel.base-url}") String baseUrl,
                                     @Value("${crm.dynmodel.api-key}") String apiKey,
                                     @Value("${crm.dynmodel.max-output-tokens}") int maxOutputTokens) {
        var chatModel = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
                .model(model).baseUrl(baseUrl).apiKey(apiKey).maxRetries(0)
                .maxTokens(maxOutputTokens).build()).build();
        return new SpringAiLlmService(model, "OpenRouter", chatModel, (options, name) -> {
            var converted = (OpenAiChatOptions) StandardOpenAiOptionsConverter.INSTANCE.convertOptions(options, name);
            return converted.mutate().maxTokens(maxOutputTokens).extraBody(Map.of(
                    "provider", Map.of("require_parameters", true),
                    "reasoning", Map.of("enabled", false))).build();
        });
    }

    /**
     * api.openai.com rejects the OpenRouter-only body fields ("Unknown parameter: 'provider'") and,
     * for the GPT-5 family, max_tokens; this path sends max_completion_tokens and no sampling fields.
     */
    @Bean
    @ConditionalOnProperty(name = "crm.dynmodel.provider", havingValue = "openai")
    SpringAiLlmService openAiDynamicModelLlm(@Value("${crm.dynmodel.model}") String model,
                                           @Value("${crm.dynmodel.base-url}") String baseUrl,
                                           @Value("${crm.dynmodel.api-key}") String apiKey,
                                           @Value("${crm.dynmodel.max-output-tokens}") int maxOutputTokens) {
        var chatModel = OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
                .model(model).baseUrl(baseUrl).apiKey(apiKey).maxRetries(0)
                .maxCompletionTokens(maxOutputTokens).build()).build();
        return new SpringAiLlmService(model, "OpenAI", chatModel, (options, name) ->
                ((OpenAiChatOptions) Gpt5ChatOptionsConverter.INSTANCE.convertOptions(options, name))
                        .mutate().maxCompletionTokens(maxOutputTokens).build());
    }
}
