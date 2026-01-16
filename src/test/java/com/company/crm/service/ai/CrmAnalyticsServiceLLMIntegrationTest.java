package com.company.crm.service.ai;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.app.service.ai.LLMJudgeTool;
import com.company.crm.model.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

class CrmAnalyticsServiceLLMIntegrationTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsServiceLLMIntegrationTest.class);

    @Autowired
    private CrmAnalyticsService analyticsService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private LLMJudgeTool llmJudgeTool;
    private ChatClient judgeClient;

    @BeforeEach
    void setUp() {
        setupKnownTestData();
        setupJudge();
    }

    private void setupJudge() {
        llmJudgeTool = new LLMJudgeTool();
        judgeClient = chatClientBuilder
            .defaultSystem("""
                You are an LLM Judge. Evaluate if AI responses correctly answer questions.
                Always call submitJudgement(correct, reasoning) with your evaluation.
                """)
            .defaultTools(llmJudgeTool)
            .build();
    }

    @Test
    void testClientCountQuestion() {
        // Given: 3 clients in database
        long actualClientCount = dataManager.loadValue("SELECT COUNT(c) FROM Client c", Long.class).one();
        assertThat(actualClientCount).isEqualTo(3L);

        // When: Ask about client count
        String question = "How many clients do we have in total?";
        String response = analyticsService.processBusinessQuestion(question);

        // Then: Judge should verify correct answer
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualClientCount + " clients");

        assertThat(evaluation.correct())
            .as("LLM should correctly answer that there are " + actualClientCount + " clients")
            .isTrue();
    }

    @Test
    void testTotalRevenueQuestion() {
        // Given: Total revenue of 15000 from all orders
        BigDecimal actualTotalRevenue = dataManager.loadValue("SELECT COALESCE(SUM(o.total), 0) FROM Order_ o", BigDecimal.class).one();
        assertThat(actualTotalRevenue).isEqualTo(new BigDecimal("15000.00"));

        // When: Ask about total revenue
        String question = "What is our total revenue from all clients?";
        String response = analyticsService.processBusinessQuestion(question);

        // Then: Judge should verify correct amount
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualTotalRevenue.toString());

        assertThat(evaluation.correct())
            .as("LLM should correctly calculate total revenue as " + actualTotalRevenue)
            .isTrue();
    }

    @Test
    void testTopClientQuestion() {
        // Given: TestClient_Charlie should be the top client with 6000 revenue
        String topClientName = dataManager.loadValue(
            "SELECT c.name FROM Client c LEFT JOIN c.orders o GROUP BY c ORDER BY COALESCE(SUM(o.total), 0) DESC",
            String.class
        ).one();

        BigDecimal topClientRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", topClientName).one();

        assertThat(topClientName).isEqualTo("TestClient_Charlie");
        assertThat(topClientRevenue).isEqualTo(new BigDecimal("6000.00"));

        // When: Ask about top client
        String question = "Which client has the highest total revenue?";
        String response = analyticsService.processBusinessQuestion(question);

        // Then: Judge should verify correct client
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, topClientName + " with " + topClientRevenue + " revenue");

        assertThat(evaluation.correct())
            .as("LLM should correctly identify " + topClientName + " as the top client")
            .isTrue();
    }

    @Test
    void testOrderCountQuestion() {
        // Given: 6 orders in database
        long actualOrderCount = dataManager.loadValue("SELECT COUNT(o) FROM Order_ o", Long.class).one();
        assertThat(actualOrderCount).isEqualTo(6L);

        // When: Ask about order count
        String question = "How many orders do we have in total?";
        String response = analyticsService.processBusinessQuestion(question);

        // Then: Judge should verify correct count
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualOrderCount + " orders");

        assertThat(evaluation.correct())
            .as("LLM should correctly count " + actualOrderCount + " orders total")
            .isTrue();
    }

    @Test
    void testAverageOrderValueQuestion() {
        // Given: Average order value of 2500
        BigDecimal actualAverageOrderValue = dataManager.loadValue("SELECT COALESCE(AVG(o.total), 0) FROM Order_ o", BigDecimal.class).one();
        assertThat(actualAverageOrderValue).isEqualTo(new BigDecimal("2500.00"));

        // When: Ask about average order value
        String question = "What is our average order value?";
        String response = analyticsService.processBusinessQuestion(question);

        // Then: Judge should verify correct average
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualAverageOrderValue.toString());

        assertThat(evaluation.correct())
            .as("LLM should correctly calculate average order value as " + actualAverageOrderValue)
            .isTrue();
    }

    private LLMJudgeTool.JudgeResult evaluateWithJudge(String question, String aiResponse, String expectedAnswer) {
        try {
            String judgePrompt = String.format("""
                Evaluate if the AI response correctly answers the question.

                Question: %s
                AI Response: %s
                Expected Answer: %s

                Use submitJudgement(correct, reasoning) to submit your evaluation.
                """, question, aiResponse, expectedAnswer);

            judgeClient.prompt(judgePrompt).call().content();

            LLMJudgeTool.JudgeResult result = llmJudgeTool.getLastResult();
            if (result == null) {
                return new LLMJudgeTool.JudgeResult(false, "Judge did not submit evaluation");
            }

            return result;

        } catch (Exception e) {
            log.error("Judge evaluation failed: {}", e.getMessage(), e);
            return new LLMJudgeTool.JudgeResult(false, "Judge evaluation failed: " + e.getMessage());
        }
    }

    private void setupKnownTestData() {
        var testClient1 = entities.client("TestClient_Alpha");
        createTestOrder(testClient1, "ALPHA-001", new BigDecimal("2000.00"), LocalDate.now().minusDays(5));
        createTestOrder(testClient1, "ALPHA-002", new BigDecimal("3000.00"), LocalDate.now().minusDays(15));

        var testClient2 = entities.client("TestClient_Beta");
        createTestOrder(testClient2, "BETA-001", new BigDecimal("1500.00"), LocalDate.now().minusDays(10));
        createTestOrder(testClient2, "BETA-002", new BigDecimal("2500.00"), LocalDate.now().minusDays(20));

        var testClient3 = entities.client("TestClient_Charlie");
        createTestOrder(testClient3, "CHARLIE-001", new BigDecimal("3500.00"), LocalDate.now().minusDays(8));
        createTestOrder(testClient3, "CHARLIE-002", new BigDecimal("2500.00"), LocalDate.now().minusDays(18));

        log.info("Created test data: 3 clients with 2 orders each, total revenue 15000");
    }

    private void createTestOrder(Client client, String orderNumber, BigDecimal total, LocalDate date) {
        var order = dataManager.create(com.company.crm.model.order.Order.class);
        order.setNumber(orderNumber);
        order.setClient(client);
        order.setTotal(total);
        order.setDate(date);
        dataManager.save(order);
    }
}