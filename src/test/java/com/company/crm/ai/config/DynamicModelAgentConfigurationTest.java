package com.company.crm.ai.config;

import com.embabel.common.ai.model.LlmOptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicModelAgentConfigurationTest {

    @Test
    void openAiPathSendsMaxCompletionTokensWithoutOpenRouterFields() {
        var llm = new DynamicModelAgentConfiguration()
                .openAiDynamicModelLlm("gpt-5.4", "https://api.openai.com/v1", "test-key", 4096);

        var options = (OpenAiChatOptions) llm.getOptionsConverter()
                .convertOptions(LlmOptions.withDefaultLlm(), "gpt-5.4");

        assertThat(options.getMaxCompletionTokens()).isEqualTo(4096);
        assertThat(options.getMaxTokens()).isNull();
        assertThat(options.getExtraBody()).isNullOrEmpty();
    }
}
