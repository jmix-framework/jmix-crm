package com.company.crm.util.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class LLMJudgeBuilder {

    private final ChatClient.Builder chatClientBuilder;
    private final PromptData promptData = new PromptData();

    public LLMJudgeBuilder(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public LLMJudgeBuilder systemPrompt(String systemPrompt) {
        promptData.systemPrompt = systemPrompt;
        return this;
    }

    public LLMJudgeBuilder judgePrompt(String judgePrompt) {
        promptData.judgePrompt = judgePrompt;
        return this;
    }

    public LLMJudge build() {
        if (promptData.systemPrompt == null || promptData.systemPrompt.isBlank()) {
            throw new IllegalStateException("LLM judge system prompt must be configured before build()");
        }
        if (promptData.judgePrompt == null || promptData.judgePrompt.isBlank()) {
            throw new IllegalStateException("LLM judge prompt must be configured before build()");
        }

        LLMJudgementCollectorTool llmJudgementCollectorTool = new LLMJudgementCollectorTool();
        ChatClient judgeClient = chatClientBuilder.clone()
            .defaultSystem(promptData.systemPrompt)
            .defaultTools(llmJudgementCollectorTool)
            .build();

        return new LLMJudge(judgeClient, llmJudgementCollectorTool, promptData.judgePrompt);
    }

    private static class PromptData {
        private String systemPrompt;
        private String judgePrompt;
    }
}
