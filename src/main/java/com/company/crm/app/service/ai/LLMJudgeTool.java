package com.company.crm.app.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class LLMJudgeTool {

    private static final Logger log = LoggerFactory.getLogger(LLMJudgeTool.class);

    private JudgeResult lastResult;

    public record JudgeResult(boolean correct, String reasoning) {}

    @Tool(name = "submitJudgement", description = """
        Submit evaluation result: correct (true/false) and reasoning (explanation).
        """)
    public void submitJudgement(
            @ToolParam(description = "Whether the AI response correctly answers the question") boolean correct,
            @ToolParam(description = "Explanation of why the answer is correct or incorrect") String reasoning) {
        log.info("Judge evaluation: correct={}, reasoning={}", correct, reasoning);
        this.lastResult = new JudgeResult(correct, reasoning);
    }

    public JudgeResult getLastResult() {
        return lastResult;
    }
}