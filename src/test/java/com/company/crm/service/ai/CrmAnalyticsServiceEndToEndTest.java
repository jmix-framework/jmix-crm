package com.company.crm.service.ai;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-End test for CRM Analytics Service with LLM Judge verification.
 * This test focuses on business questions and uses LLM as a Judge
 * to verify the correctness of AI-generated answers against known data.
 */
class CrmAnalyticsServiceEndToEndTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsServiceEndToEndTest.class);

    @Autowired
    private CrmAnalyticsService analyticsService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private LLMJudgeTool llmJudgeTool;
    private ChatClient judgeClient;
    private String conversationId;

    @BeforeEach
    void setUp() {
        setupKnownTestData();
        setupTestConversation();
        setupJudge();
    }

    private void setupTestConversation() {
        // Create a test conversation for each test
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("Test LLM Judge Conversation");
        dataManager.save(conversation);
        conversationId = conversation.getId().toString();
        log.info("Created test conversation with ID: {}", conversationId);
    }

    private void setupJudge() {
        llmJudgeTool = new LLMJudgeTool();
        judgeClient = chatClientBuilder
            .defaultSystem("""
                You are an LLM Judge. Evaluate if AI responses correctly answer questions.
                Always call submitJudgement(correct, reasoning) with your evaluation.
                CRITICAL: Keep all reasoning text as single line without line breaks or newlines.
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
        String response = analyticsService.processBusinessQuestion(question, conversationId);

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
        String response = analyticsService.processBusinessQuestion(question, conversationId);

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
        String response = analyticsService.processBusinessQuestion(question, conversationId);

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
        String response = analyticsService.processBusinessQuestion(question, conversationId);

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
        String response = analyticsService.processBusinessQuestion(question, conversationId);

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

    @Test
    void testBusinessAnalysis_ClientSegmentationQuestion() {
        // Given: Client revenue data - Charlie (6000), Alpha (5000), Beta (4000)
        BigDecimal charlieRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", "TestClient_Charlie").one();

        BigDecimal alphaRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", "TestClient_Alpha").one();

        BigDecimal betaRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", "TestClient_Beta").one();

        assertThat(charlieRevenue).isEqualTo(new BigDecimal("6000.00"));
        assertThat(alphaRevenue).isEqualTo(new BigDecimal("5000.00"));
        assertThat(betaRevenue).isEqualTo(new BigDecimal("4000.00"));

        // When: Ask about client segmentation
        String question = "Can you segment our clients into high-value, medium-value, and low-value based on their order history?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // Then: Judge should verify segmentation using the verified revenue data
        String expectedAnswer = String.format("Client segmentation based on revenue totals: TestClient_Charlie %s (high-value), TestClient_Alpha %s (medium-value), TestClient_Beta %s (low-value)",
            charlieRevenue, alphaRevenue, betaRevenue);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly segment clients by value")
            .isTrue();
    }

    @Test
    void testBusinessAnalysis_ClientPerformanceAnalysis() {
        String question = "Which clients have the highest order volumes this quarter? Please analyze their performance trends.";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // Judge should verify performance analysis
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response,
            "Each client has 2 orders, but TestClient_Charlie has highest total value at 6000");

        assertThat(evaluation.correct())
            .as("LLM should correctly analyze client performance")
            .isTrue();
    }

    @Test
    void testBusinessAnalysis_TrendAnalysisQuestion() {
        // Given: 6 orders total with dates distributed over recent months
        long actualOrderCount = dataManager.loadValue("SELECT COUNT(o) FROM Order_ o", Long.class).one();
        assertThat(actualOrderCount).isEqualTo(6L);

        // Verify orders are distributed over time with known pattern
        long recentOrdersLast30Days = dataManager.loadValue(
            "SELECT COUNT(o) FROM Order_ o WHERE o.date >= :startDate",
            Long.class
        ).parameter("startDate", LocalDate.now().minusDays(30)).one();

        assertThat(recentOrdersLast30Days).isEqualTo(6L); // All orders are within last 30 days

        // When: Ask about order trends
        String question = "What are the trends in our order volumes over the past few months? Are we growing or declining?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // Then: Judge should verify trend analysis using the verified data
        String expectedAnswer = String.format("%d orders total: %d orders within last 30 days, showing recent order activity",
            actualOrderCount, recentOrdersLast30Days);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly analyze order trends")
            .isTrue();
    }

    @Test
    void testInteractiveLinksGeneration() {
        // Given: Get actual client IDs and names from database
        UUID alphaClientUuid = dataManager.loadValue("SELECT c.id FROM Client c WHERE c.name = :name", UUID.class)
            .parameter("name", "TestClient_Alpha").one();
        UUID betaClientUuid = dataManager.loadValue("SELECT c.id FROM Client c WHERE c.name = :name", UUID.class)
            .parameter("name", "TestClient_Beta").one();
        UUID charlieClientUuid = dataManager.loadValue("SELECT c.id FROM Client c WHERE c.name = :name", UUID.class)
            .parameter("name", "TestClient_Charlie").one();

        String alphaClientId = alphaClientUuid.toString();
        String betaClientId = betaClientUuid.toString();
        String charlieClientId = charlieClientUuid.toString();

        assertThat(alphaClientId).isNotNull();
        assertThat(betaClientId).isNotNull();
        assertThat(charlieClientId).isNotNull();

        // When: Ask about clients (should generate markdown links with real IDs)
        String question = "Show me all our clients with their details";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // Then: Response should contain markdown links with the actual client IDs
        assertThat(response).isNotNull();

        // Check that response contains links to the correct client IDs (flexible link text)
        assertThat(response).contains("/clients/" + alphaClientId + ")");
        assertThat(response).contains("/clients/" + betaClientId + ")");
        assertThat(response).contains("/clients/" + charlieClientId + ")");

        // Also check that the client names appear somewhere (but not necessarily in link format)
        assertThat(response).containsIgnoringCase("TestClient_Alpha");
        assertThat(response).containsIgnoringCase("TestClient_Beta");
        assertThat(response).containsIgnoringCase("TestClient_Charlie");

        log.info("Interactive links test response with client IDs: {}", response);
        log.info("Alpha ID: {}, Beta ID: {}, Charlie ID: {}", alphaClientId, betaClientId, charlieClientId);
    }

    @Test
    void testBusinessAnalysis_ComparisonQuestion() {
        // Given: All clients have 2 orders each, but different total values
        // Verify each client has exactly 2 orders
        long alphaOrderCount = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            Long.class
        ).parameter("clientName", "TestClient_Alpha").one();

        long betaOrderCount = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            Long.class
        ).parameter("clientName", "TestClient_Beta").one();

        long charlieOrderCount = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            Long.class
        ).parameter("clientName", "TestClient_Charlie").one();

        assertThat(alphaOrderCount).isEqualTo(2L);
        assertThat(betaOrderCount).isEqualTo(2L);
        assertThat(charlieOrderCount).isEqualTo(2L);

        // Verify revenue ranges: Beta (4000), Alpha (5000), Charlie (6000)
        BigDecimal alphaRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", "TestClient_Alpha").one();

        BigDecimal betaRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", "TestClient_Beta").one();

        BigDecimal charlieRevenue = dataManager.loadValue(
            "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
            BigDecimal.class
        ).parameter("clientName", "TestClient_Charlie").one();

        assertThat(betaRevenue).isEqualTo(new BigDecimal("4000.00"));   // Minimum
        assertThat(alphaRevenue).isEqualTo(new BigDecimal("5000.00"));  // Medium
        assertThat(charlieRevenue).isEqualTo(new BigDecimal("6000.00")); // Maximum

        // When: Ask about client comparison
        String question = "Compare the performance of our enterprise clients versus our smaller clients. Which segment is more profitable?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // Then: Judge should verify comparison analysis using verified data
        String expectedAnswer = String.format("Each client has 2 orders with total revenues: TestClient_Beta %s, TestClient_Alpha %s, TestClient_Charlie %s",
            betaRevenue, alphaRevenue, charlieRevenue);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly compare client segments")
            .isTrue();
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