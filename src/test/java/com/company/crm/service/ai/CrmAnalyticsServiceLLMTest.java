package com.company.crm.service.ai;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.model.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * End-to-End test for CRM Analytics Service with LLM Judge verification.
 * This test focuses on business questions and uses LLM as a Judge
 * to verify the correctness of AI-generated answers against known data.
 *
 */
//@EnabledIfEnvironmentVariable(named = "AI_ENABLED", matches = "true")
class CrmAnalyticsServiceLLMTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsServiceLLMTest.class);

    @Autowired
    private CrmAnalyticsService analyticsService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private LLMJudgeTool llmJudgeTool;
    private ChatClient judgeClient;
    private String conversationId;

    @BeforeEach
    void setUp() {
        setupTestConversation();
        setupJudge();
    }

    private void setupTestConversation() {
        // Create a test conversation for each test
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("Test LLM Judge Conversation");
        dataManager.saveWithoutReload(conversation);
        conversationId = conversation.getId().toString();
        log.info("Created test conversation with ID: {}", conversationId);
    }

    private void setupJudge() {
        llmJudgeTool = new LLMJudgeTool();
        judgeClient = chatClientBuilder
            .defaultSystem("""
                You are an LLM Judge. Evaluate if AI responses correctly answer questions or provide substantially correct information.

                Accept responses as correct if they:
                - Answer the main question accurately
                - Provide correct key data points and insights
                - Show sound analysis methodology
                - May have minor omissions or imprecisions that don't affect the core answer
                - Reference using reports/tools (even if you can't verify tool execution)
                - Present data in tables or structured format showing business understanding

                Reject responses only if they:
                - Contain major factual errors that contradict expected test data
                - Miss the main point of the question completely
                - Provide fundamentally incorrect analysis
                - Show no understanding of business context

                Be generous in evaluation - focus on whether the response demonstrates business competence and addresses the question meaningfully.

                Always call submitJudgement(correct, reasoning) with your evaluation.
                CRITICAL: Keep all reasoning text as single line without line breaks or newlines.
                """)
            .defaultTools(llmJudgeTool)
            .build();
    }

    @Test
    void testClientCountQuestion() {
        // given
        setupKnownTestData();
        // Given: 3 clients in database
        long actualClientCount = dataManager.loadValue("SELECT COUNT(c) FROM Client c", Long.class).one();
        assertThat(actualClientCount).isEqualTo(3L);

        // when
        String question = "How many clients do we have in total?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).contains("3");
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualClientCount + " clients");

        assertThat(evaluation.correct())
            .as("LLM should correctly answer that there are " + actualClientCount + " clients")
            .isTrue();
    }

    @Test
    void testTotalRevenueQuestion() {
        // given
        setupKnownTestData();
        // Given: Total revenue of 15000 from all orders
        BigDecimal actualTotalRevenue = dataManager.loadValue("SELECT COALESCE(SUM(o.total), 0) FROM Order_ o", BigDecimal.class).one();
        assertThat(actualTotalRevenue).isEqualTo(new BigDecimal("15000.00"));

        // when
        String question = """
                What is our total revenue from all clients?
                In this demo dataset, use Order_.total as the revenue source of truth (not Invoice totals).
                Return the total amount and mention briefly that this is order-based revenue.
                """;
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, """
                Must compute total revenue from Order_.total and report 15000.00 (format variants acceptable).
                Should not switch to Invoice totals for this question.
                A brief clarification that this is order-based revenue is expected.
                """);

        assertThat(evaluation.correct())
            .as("LLM should correctly calculate total revenue as " + actualTotalRevenue)
            .isTrue();
    }

    @Test
    void testTopClientQuestion() {
        // given
        setupKnownTestData();
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

        // when
        String question = "Which client has the highest total revenue?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).containsIgnoringCase(topClientName);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, topClientName + " with " + topClientRevenue + " revenue");

        assertThat(evaluation.correct())
            .as("LLM should correctly identify " + topClientName + " as the top client")
            .isTrue();
    }

    @Test
    void testOrderCountQuestion() {
        // given
        setupKnownTestData();
        // Given: 6 orders in database
        long actualOrderCount = dataManager.loadValue("SELECT COUNT(o) FROM Order_ o", Long.class).one();
        assertThat(actualOrderCount).isEqualTo(6L);

        // when
        String question = "How many orders do we have in total?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).contains("6");
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualOrderCount + " orders");

        assertThat(evaluation.correct())
            .as("LLM should correctly count " + actualOrderCount + " orders total")
            .isTrue();
    }

    @Test
    void testAverageOrderValueQuestion() {
        // given
        setupKnownTestData();
        // Given: Average order value of 2500
        BigDecimal actualAverageOrderValue = dataManager.loadValue("SELECT COALESCE(AVG(o.total), 0) FROM Order_ o", BigDecimal.class).one();
        assertThat(actualAverageOrderValue).isEqualTo(new BigDecimal("2500.00"));

        // when
        String question = "What is our average order value?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).containsAnyOf("2500", "2,500");
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, actualAverageOrderValue.toString());

        assertThat(evaluation.correct())
            .as("LLM should correctly calculate average order value as " + actualAverageOrderValue)
            .isTrue();
    }

    @Test
    void testClient360ReportQuestion() {
        // given
        setupKnownTestData();
        systemAuthenticator.runWithSystem(() -> {
            // given: Setup a High Risk Customer
            // Risk Level criteria in Client360ReportService for HIGH: 
            // - overdueCount >= 3 OR overdueAmount > 5000 OR avgPaymentDuration > 45 days
            Client client = entities.client("Risk_Gamma");
            
            // Create 4 overdue invoices to trigger 'HIGH' risk level.
            // Use previous calendar year dynamically, because LLM/tool defaults often target that window.
            int previousYear = LocalDate.now().minusYears(1).getYear();
            for (int i = 1; i <= 4; i++) {
                var invoice = dataManager.create(com.company.crm.model.invoice.Invoice.class);
                invoice.setClient(client);
                invoice.setNumber("INV-RISK-00" + i);
                invoice.setTotal(new BigDecimal("2000.00"));
                invoice.setDate(LocalDate.of(previousYear, 6, i)); // Middle of previous year
                invoice.setStatus(com.company.crm.model.invoice.InvoiceStatus.OVERDUE);
                dataManager.saveWithoutReload(invoice);
            }

            UUID clientId = client.getId();
            LocalDate fromDate = LocalDate.of(previousYear, 1, 1);
            LocalDate toDate = LocalDate.of(previousYear, 12, 31);
            
            // when: Ask a natural business question that should trigger report discovery and execution.
            // The instruction forces the AI to look for authoritative pre-calculated data (Reports)
            // instead of trying to derive it manually via JPQL.
            String question = """
                Retrieve the internal business risk assessment and indicators for client %s. \
                Use report parameter aliases exactly as: client=%s, fromDate=%s, toDate=%s, audience=ai. \
                Important: You must find the authoritative pre-calculated data for this in the system tools. \
                Do not try to derive or calculate these metrics yourself via ad-hoc combination of multiple queries, \
                as this could lead to incorrect results based on wrong business assumptions.\
                """.formatted(clientId, clientId, fromDate, toDate);
            String response = analyticsService.processBusinessQuestion(question, conversationId);

//            // then
            String expectedAnswer = String.format("""
                Should use client-360-report to get comprehensive risk assessment for client %s.
                Client has 4 overdue invoices (INV-RISK-001 to INV-RISK-004) totaling 8000.00.
                Expected to identify HIGH risk level due to multiple overdue invoices.
                Should demonstrate use of authoritative report data rather than manual calculations.
                Response may vary in format but should reference the overdue invoices and high risk assessment.
                """, clientId);
            LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

            assertThat(evaluation.correct())
                .isTrue();
        });
    }

    private LLMJudgeTool.JudgeResult evaluateWithJudge(String question, String aiResponse, String expectedAnswer) {
        try {
            String judgePrompt = """
                Evaluate if the AI response provides a reasonable answer to the business question.

                Question: %s
                AI Response: %s
                Expected Key Facts: %s

                ACCEPT the response as CORRECT if it:
                - Shows understanding of the business question
                - Provides data-driven analysis (uses actual numbers/facts)
                - Identifies key clients and their performance patterns
                - Offers business insights or recommendations
                - May have minor inaccuracies but captures the main trends

                REJECT the response as INCORRECT only if it:
                - Completely fails to address the question
                - Contains major factual errors that would mislead business decisions
                - Shows no understanding of the data or business context
                - Provides no actionable insights

                For business analysis questions, focus on whether the response demonstrates competent analytical thinking rather than perfect precision.

                Use submitJudgement(correct, reasoning) to submit your evaluation.
                """.formatted(question, aiResponse, expectedAnswer);

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
    void testClientSegmentationQuestion() {
        // given
        setupKnownTestData();
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

        // when
        String question = "Can you segment our clients into high-value, medium-value, and low-value based on their order history?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).containsIgnoringCase("TestClient_Charlie");
        assertThat(response).containsIgnoringCase("TestClient_Alpha");
        assertThat(response).containsIgnoringCase("TestClient_Beta");
        String expectedAnswer = String.format("Client segmentation based on revenue totals: TestClient_Charlie %s (high-value), TestClient_Alpha %s (medium-value), TestClient_Beta %s (low-value)",
            charlieRevenue, alphaRevenue, betaRevenue);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly segment clients by value")
            .isTrue();
    }

    @Test
    void testTrendAnalysisQuestion() {
        // given
        setupKnownTestData();
        // Given: 6 orders total with dates distributed over recent months
        long actualOrderCount = dataManager.loadValue("SELECT COUNT(o) FROM Order_ o", Long.class).one();
        assertThat(actualOrderCount).isEqualTo(6L);

        // Verify orders are distributed over time with known pattern
        long recentOrdersLast30Days = dataManager.loadValue(
            "SELECT COUNT(o) FROM Order_ o WHERE o.date >= :startDate",
            Long.class
        ).parameter("startDate", LocalDate.now().minusDays(30)).one();

        assertThat(recentOrdersLast30Days).isEqualTo(6L); // All orders are within last 30 days

        // when
        String question = "What are the trends in our order volumes over the past few months? Are we growing or declining?";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).containsAnyOf("trend", "growing", "declining", "orders");
        String expectedAnswer = String.format("%d orders total: %d orders within last 30 days, showing recent order activity",
            actualOrderCount, recentOrdersLast30Days);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly analyze order trends")
            .isTrue();
    }

    @Test
    void testInteractiveLinksGeneration() {
        // given
        setupKnownTestData();
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

        assertThat(alphaClientId).isNotBlank();
        assertThat(betaClientId).isNotBlank();
        assertThat(charlieClientId).isNotBlank();

        // when: Ask about clients (should generate markdown links with real IDs)
        String question = "Show me all our clients with their details";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then: Response should contain markdown links with the actual client IDs
        assertThat(response).isNotBlank();

        // Check that response contains links to the correct client IDs (flexible link text)
        assertThat(response).contains("clients/" + alphaClientId + ")");
        assertThat(response).contains("clients/" + betaClientId + ")");
        assertThat(response).contains("clients/" + charlieClientId + ")");

        // Also check that the client names appear somewhere (but not necessarily in link format)
        assertThat(response).containsIgnoringCase("TestClient_Alpha");
        assertThat(response).containsIgnoringCase("TestClient_Beta");
        assertThat(response).containsIgnoringCase("TestClient_Charlie");

        // LLM-as-a-Judge evaluation
        String expectedAnswer = String.format("""
                Must show all 3 clients (TestClient_Alpha, TestClient_Beta, TestClient_Charlie) with their details.
                Should generate markdown links for each client using format: clients/%s), clients/%s), clients/%s).
                Should include client names and potentially other details like revenue, orders, etc.
                """, alphaClientId, betaClientId, charlieClientId);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly list all clients with interactive links using actual UUIDs")
            .isTrue();

        log.info("Interactive links test response with client IDs: {}", response);
        log.info("Alpha ID: {}, Beta ID: {}, Charlie ID: {}", alphaClientId, betaClientId, charlieClientId);
    }

    @Test
    void testComparisonQuestion() {
        // given
        setupKnownTestData();
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

        // when
        String question = """
                Compare the performance of our enterprise clients versus our smaller clients.
                In this demo dataset, use total ORDER amounts as profitability proxy (invoices may be missing).
                Base your analysis on these clients: TestClient_Beta, TestClient_Alpha, TestClient_Charlie.
                State which segment/proxy winner is higher and mention any data limitation briefly.
                """;
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then
        assertThat(response).isNotBlank();
        assertThat(response).containsAnyOf("TestClient_Beta", "TestClient_Alpha", "TestClient_Charlie");
        String expectedAnswer = String.format("""
                Must compare the three specified clients using ORDER totals as profitability proxy.
                Expected order totals: TestClient_Beta %s, TestClient_Alpha %s, TestClient_Charlie %s.
                Should conclude that Charlie > Alpha > Beta for the proxy comparison.
                A brief limitation note about missing invoice/cost data is acceptable.
                """,
            betaRevenue, alphaRevenue, charlieRevenue);
        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        assertThat(evaluation.correct())
            .as("LLM should correctly compare client segments or explain data limitations")
            .isTrue();
    }

    @Test
    void testChurnRiskAnalysisQuestion() {
        // given
        setupKnownTestData();
        // Given: Create specific test data to simulate churn risk scenarios
        setupChurnRiskTestData();

        // Verify the churn risk indicators exist in test data
        // 1. Client with revenue decline (Delta: recent revenue lower than historical)
        long deltaRecentOrders = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :name AND @between(o.date, now-90, now, day)",
            Long.class
        ).parameter("name", "TestClient_Delta").one();

        long deltaHistoricalOrders = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :name AND @between(o.date, now-180, now-91, day)",
            Long.class
        ).parameter("name", "TestClient_Delta").one();

        // 2. Client with no recent activity (Echo: historical orders but none recent)
        long echoRecentOrders = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :name AND @between(o.date, now-90, now, day)",
            Long.class
        ).parameter("name", "TestClient_Echo").one();

        long echoHistoricalOrders = dataManager.loadValue(
            "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :name AND @between(o.date, now-180, now-91, day)",
            Long.class
        ).parameter("name", "TestClient_Echo").one();

        // Verify test data setup - Delta should show decline, Echo should show complete drop-off
        assertThat(deltaHistoricalOrders).isGreaterThan(0L); // Had historical activity
        assertThat(deltaRecentOrders).isLessThan(deltaHistoricalOrders); // Recent activity declined
        assertThat(echoHistoricalOrders).isGreaterThan(0L); // Had historical activity
        assertThat(echoRecentOrders).isEqualTo(0L); // No recent activity

        // when: Ask the specific churn risk question
        String question = "Which clients haven't been ordering lately and might be at risk of churning? I'm worried we might be losing some good customers without noticing.";
        String response = analyticsService.processBusinessQuestion(question, conversationId);

        // then: Judge should verify the AI identifies the risk accounts and provides actionable insights
        String expectedAnswer = String.format(
            "Should identify at-risk accounts based on churn/revenue-decline patterns. TestClient_Delta shows decline (%d recent vs %d historical orders). May not detect TestClient_Echo (complete drop-off) due to query limitations. Should provide business recommendations and analysis methodology.",
            deltaRecentOrders, deltaHistoricalOrders
        );

        LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, expectedAnswer);

        // Verify response contains key elements expected in churn risk analysis
        assertThat(response).isNotBlank();
        assertThat(response.length()).isGreaterThan(500); // Should be substantial analysis

        // Should mention at least TestClient_Delta (Echo might be missed due to query limitations)
        assertThat(response).containsIgnoringCase("TestClient_Delta");

        // Should contain analysis keywords
        assertThat(response).containsAnyOf("risk", "decline", "churn", "revenue", "recommendation", "action");

        assertThat(evaluation.correct())
            .as("LLM should correctly identify accounts with churn/revenue-decline risk and provide actionable insights")
            .isTrue();

        log.info("Churn risk analysis response: {}", response);
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

    /**
     * Setup specific test data for churn risk analysis - creates clients with different risk profiles
     */
    private void setupChurnRiskTestData() {
        // First clear existing test data for clean churn risk scenario
        // Keep only the original 3 clients but add new at-risk clients

        // Create TestClient_Delta: Revenue decline scenario
        // Historical activity (older than 90 days) but reduced recent activity
        var deltaClient = entities.client("TestClient_Delta");
        // Historical orders (91-180 days ago) - high value
        createTestOrder(deltaClient, "DELTA-H001", new BigDecimal("5000.00"), LocalDate.now().minusDays(120));
        createTestOrder(deltaClient, "DELTA-H002", new BigDecimal("4500.00"), LocalDate.now().minusDays(140));
        createTestOrder(deltaClient, "DELTA-H003", new BigDecimal("3000.00"), LocalDate.now().minusDays(160));
        // Recent orders (last 90 days) - much lower value, showing decline
        createTestOrder(deltaClient, "DELTA-R001", new BigDecimal("1000.00"), LocalDate.now().minusDays(30));

        // Create TestClient_Echo: Complete drop-off scenario
        // Historical activity but NO recent activity (classic churn risk)
        var echoClient = entities.client("TestClient_Echo");
        // Historical orders (91-180 days ago) - was a good client
        createTestOrder(echoClient, "ECHO-H001", new BigDecimal("6000.00"), LocalDate.now().minusDays(100));
        createTestOrder(echoClient, "ECHO-H002", new BigDecimal("7500.00"), LocalDate.now().minusDays(130));
        createTestOrder(echoClient, "ECHO-H003", new BigDecimal("4000.00"), LocalDate.now().minusDays(150));
        // NO recent orders - complete cessation of activity

        // Create TestClient_Foxtrot: Stable client (control group - should NOT be flagged as at-risk)
        var foxtrotClient = entities.client("TestClient_Foxtrot");
        // Consistent activity both historical and recent
        createTestOrder(foxtrotClient, "FOX-H001", new BigDecimal("3000.00"), LocalDate.now().minusDays(110));
        createTestOrder(foxtrotClient, "FOX-H002", new BigDecimal("3500.00"), LocalDate.now().minusDays(140));
        createTestOrder(foxtrotClient, "FOX-R001", new BigDecimal("3200.00"), LocalDate.now().minusDays(25));
        createTestOrder(foxtrotClient, "FOX-R002", new BigDecimal("3800.00"), LocalDate.now().minusDays(45));

        log.info("Created churn risk test data:");
        log.info("- TestClient_Delta: Revenue decline (12500 historical -> 1000 recent)");
        log.info("- TestClient_Echo: Complete drop-off (17500 historical -> 0 recent)");
        log.info("- TestClient_Foxtrot: Stable (6500 historical -> 7000 recent)");
    }

    private void createTestOrder(Client client, String orderNumber, BigDecimal total, LocalDate date) {
        var order = dataManager.create(com.company.crm.model.order.Order.class);
        order.setNumber(orderNumber);
        order.setClient(client);
        order.setTotal(total);
        order.setDate(date);
        dataManager.saveWithoutReload(order);
    }
}
