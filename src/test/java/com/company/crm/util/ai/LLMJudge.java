package com.company.crm.util.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;

public class LLMJudge {
    private static final Logger log = LoggerFactory.getLogger(LLMJudge.class);

    private final ChatClient judgeClient;
    private final LLMJudgementCollectorTool llmJudgementCollectorTool;
    private final String judgePromptTemplate;

    LLMJudge(ChatClient judgeClient, LLMJudgementCollectorTool llmJudgementCollectorTool, String judgePromptTemplate) {
        this.judgeClient = judgeClient;
        this.llmJudgementCollectorTool = llmJudgementCollectorTool;
        this.judgePromptTemplate = judgePromptTemplate;
    }

    public void evaluateAnswerWithJudge(String question, String aiResponse, String expectedAnswer) {
        LLMJudgementCollectorTool.JudgeResult result = runJudgeEvaluation(question, aiResponse, expectedAnswer);

        assertThat(result.correct())
            .withFailMessage("""
                <ground-truth>
                %s
                </ground-truth>

                <llm-output>
                %s
                </llm-output>

                <judge-reasoning>
                %s
                </judge-reasoning>
                """, expectedAnswer, aiResponse, result.reasoning())
            .isTrue();
    }

    public void evaluateAnwserWithJudge(String question, String aiResponse, String expectedAnswer) {
        evaluateAnswerWithJudge(question, aiResponse, expectedAnswer);
    }

    public LLMJudgementCollectorTool.JudgeResult runJudgeEvaluation(String question, String aiResponse, String expectedAnswer) {
        try {
            String judgePrompt = judgePromptTemplate.formatted(question, aiResponse, expectedAnswer);
            judgeClient.prompt(judgePrompt).call().content();

            LLMJudgementCollectorTool.JudgeResult result = llmJudgementCollectorTool.getResult();
            if (result == null) {
                return new LLMJudgementCollectorTool.JudgeResult(false, "Judge did not submit evaluation");
            }
            return result;
        } catch (Exception e) {
            log.error("Judge evaluation failed: {}", e.getMessage(), e);
            return new LLMJudgementCollectorTool.JudgeResult(false, "Judge evaluation failed: " + e.getMessage());
        }
    }
}
