package com.company.crm.ai.service;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.util.ai.LLMJudge;
import com.company.crm.util.ai.LLMJudgeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "AI_ENABLED", matches = "true")
class AiConversationTitleLLMTest extends AbstractTest {

    private static final String DEFAULT_TITLE = "New AI Conversation";

    @Autowired
    private LLMJudgeBuilder llmJudgeBuilder;

    private LLMJudge llmJudge;

    @BeforeEach
    void setUp() {
        llmJudge = llmJudgeBuilder
                .systemPrompt("""
                        You are an LLM Judge evaluating generated conversation titles for a CRM application.

                        Evaluate if the generated title is a good, concise summary of the user's question.
                        A good title should:
                        - Be short (under 8 words)
                        - Capture the main topic of the question
                        - Be relevant to CRM/business context

                        Always call submitJudgement(correct, reasoning) with your evaluation.
                        CRITICAL: Keep all reasoning text as single line without line breaks or newlines.
                        """)
                .judgePrompt("""
                        Evaluate the generated title for this CRM conversation.

                        User Question: %s
                        Generated Title: %s
                        Expected: %s

                        Call submitJudgement with your evaluation.
                        """)
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "hi",
            "hello",
            "hey there",
            "good morning",
            "how are you?",
            "what can you do?",
            "test",
            "..."
    })
    void greetings_shouldNotChangeTitle(String greeting) {
        UUID conversationId = createConversationWithUserMessage(greeting);

        assertThat(loadTitle(conversationId)).isEqualTo(DEFAULT_TITLE);
    }

    @Test
    void revenueQuestion_shouldGenerateTitle() {
        String question = "Show me total revenue for Q1";
        UUID conversationId = createConversationWithUserMessage(question);

        String title = loadTitle(conversationId);
        assertThat(title).isNotEqualTo(DEFAULT_TITLE);
        llmJudge.evaluateAnswerWithJudge(question, title,
                "A short title about Q1 revenue or quarterly revenue overview");
    }

    @Test
    void overdueInvoicesQuestion_shouldGenerateTitle() {
        String question = "Which clients have overdue invoices?";
        UUID conversationId = createConversationWithUserMessage(question);

        String title = loadTitle(conversationId);
        assertThat(title).isNotEqualTo(DEFAULT_TITLE);
        llmJudge.evaluateAnswerWithJudge(question, title,
                "A short title about overdue invoices or client payment status");
    }

    @Test
    void clientReportQuestion_shouldGenerateTitle() {
        String question = "Create a report for client Brakus Inc";
        UUID conversationId = createConversationWithUserMessage(question);

        String title = loadTitle(conversationId);
        assertThat(title).isNotEqualTo(DEFAULT_TITLE);
        llmJudge.evaluateAnswerWithJudge(question, title,
                "A short title mentioning Brakus Inc and report generation");
    }

    @Test
    void orderCountQuestion_shouldGenerateTitle() {
        String question = "How many orders did we get last month?";
        UUID conversationId = createConversationWithUserMessage(question);

        String title = loadTitle(conversationId);
        assertThat(title).isNotEqualTo(DEFAULT_TITLE);
        llmJudge.evaluateAnswerWithJudge(question, title,
                "A short title about last month's order count or order volume");
    }

    private UUID createConversationWithUserMessage(String userMessage) {
        AiConversation conversation = entities.createAndSaveEntity(AiConversation.class,
                c -> c.setTitle(DEFAULT_TITLE));

        // Saving the ChatMessage triggers AiConversationTitleListener after commit
        entities.createAndSaveEntity(ChatMessage.class, m -> {
            m.setConversation(conversation);
            m.setContent(userMessage);
            m.setType(ChatMessageType.USER);
        });

        return conversation.getId();
    }

    private String loadTitle(UUID conversationId) {
        return systemAuthenticator.withSystem(() ->
                dataManager.load(AiConversation.class).id(conversationId).one().getTitle());
    }
}
