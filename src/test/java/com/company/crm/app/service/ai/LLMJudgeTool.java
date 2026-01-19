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
        Submit evaluation result: correct (true/false) and brief reasoning (max 100 chars, single line).
        IMPORTANT: Keep reasoning text as single line without newlines or line breaks.
        """)
    public void submitJudgement(
            @ToolParam(description = "Whether the AI response correctly answers the question") boolean correct,
            @ToolParam(description = "Brief explanation (max 100 characters, single line only)") String reasoning) {
        // Clean reasoning text from potential JSON-breaking characters
        String cleanReasoning = reasoning.replaceAll("[\r\n\t]", " ").trim();
        log.info("Judge evaluation: correct={}, reasoning={}", correct, cleanReasoning);
        this.lastResult = new JudgeResult(correct, cleanReasoning);
    }

    public JudgeResult getLastResult() {
        return lastResult;
    }
}