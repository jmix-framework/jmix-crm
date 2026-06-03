package com.company.crm.test.ai.service;

import com.company.crm.AbstractAiTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.service.CrmAnalyticsService;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.user.User;
import com.company.crm.util.ai.LLMJudge;
import com.company.crm.util.ai.LLMJudgeBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-End test for CRM Analytics Service with LLM Judge verification.
 * This test focuses on business questions and uses LLM as a Judge
 * to verify the correctness of AI-generated answers against known data.
 */
class CrmAnalyticsServiceLLMTest extends AbstractAiTest {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsServiceLLMTest.class);

    @Autowired
    private LLMJudgeBuilder llmJudgeBuilder;
    @Autowired
    private CrmAnalyticsService analyticsService;
    @Autowired
    private AiConversationService aiConversationService;

    private LLMJudge llmJudge;
    private String conversationId;
    private AiConversation conversation;

    @BeforeEach
    void setUp() {
        setupTestConversation();
        setupJudge();
    }

    private static final String DEFAULT_TITLE = "New AI Conversation";

    private void setupTestConversation() {
        AiConversation newConversation = dataManager.create(AiConversation.class);
        newConversation.setTitle(DEFAULT_TITLE);
        conversation = dataManager.save(newConversation);
        conversationId = conversation.getId().toString();
        log.info("Created test conversation with ID: {}", conversationId);
    }

    private void setupJudge() {
        llmJudge = llmJudgeBuilder
                .systemPrompt("""
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
                .judgePrompt("""
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
                        """)
                .build();
    }

    @Nested
    class CoreMetrics {

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void testClientCountQuestion() {
            long actualClientCount = totalClientCount();
            assertThat(actualClientCount).isEqualTo(3L);

            String question = "How many clients do we have in total?";
            String response = askBusinessQuestion(question);

            assertThat(response).contains("3");
            llmJudge.evaluateAnswerWithJudge(question, response, actualClientCount + " clients");
        }

        @Test
        void testTotalRevenueQuestion() {
            BigDecimal actualTotalRevenue = totalOrderRevenue();
            assertThat(actualTotalRevenue).isEqualTo(new BigDecimal("15000.00"));

            String question = """
                    What is our total revenue from all clients?
                    
                    In this demo dataset, use Order_.total as the revenue source of truth (not Invoice totals).
                    Return the total amount and mention briefly that this is order-based revenue.
                    """;
            String response = askBusinessQuestion(question);

            llmJudge.evaluateAnswerWithJudge(question, response, """
                    Must compute total revenue from Order_.total and report 15000.00 (format variants acceptable).
                    
                    Should not switch to Invoice totals for this question.
                    A brief clarification that this is order-based revenue is expected.
                    """);
        }

        @Test
        void testTopClientQuestion() {
            String topClientName = topClientByRevenue();
            assertThat(topClientName).isEqualTo("TestClient_Charlie");

            BigDecimal topClientRevenue = totalRevenueForClient(topClientName);
            assertThat(topClientRevenue).isEqualTo(new BigDecimal("6000.00"));

            String question = "Which client has the highest total revenue?";
            String response = askBusinessQuestion(question);

            assertThat(response).containsIgnoringCase(topClientName);
            llmJudge.evaluateAnswerWithJudge(question, response, topClientName + " with " + topClientRevenue + " revenue");
        }

        @Test
        void testOrderCountQuestion() {
            long actualOrderCount = totalOrderCount();
            assertThat(actualOrderCount).isEqualTo(6L);

            String question = "How many orders do we have in total?";
            String response = askBusinessQuestion(question);

            assertThat(response).contains("6");
            llmJudge.evaluateAnswerWithJudge(question, response, actualOrderCount + " orders");
        }

        @Test
        void testAverageOrderValueQuestion() {
            BigDecimal actualAverageOrderValue = averageOrderValue();
            assertThat(actualAverageOrderValue).isEqualTo(new BigDecimal("2500.00"));

            String question = "What is our average order value?";
            String response = askBusinessQuestion(question);

            assertThat(response).containsAnyOf("2500", "2,500");
            llmJudge.evaluateAnswerWithJudge(question, response, actualAverageOrderValue.toString());
        }
    }

    @Nested
    class ClientValueAnalysis {

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void testClientSegmentationQuestion() {
            BigDecimal charlieRevenue = totalRevenueForClient("TestClient_Charlie");
            BigDecimal alphaRevenue = totalRevenueForClient("TestClient_Alpha");
            BigDecimal betaRevenue = totalRevenueForClient("TestClient_Beta");
            assertThat(charlieRevenue).isEqualTo(new BigDecimal("6000.00"));
            assertThat(alphaRevenue).isEqualTo(new BigDecimal("5000.00"));
            assertThat(betaRevenue).isEqualTo(new BigDecimal("4000.00"));

            String question = """
                    Can you segment all clients by lifetime order revenue using these fixed bands:
                    
                    - high-value: strictly greater than 5000
                    - medium-value: 4500 to 5000 (inclusive)
                    - low-value: strictly less than 4500
                    
                    List every client in exactly one segment.
                    """;
            String response = askBusinessQuestion(question);

            assertThat(response).containsIgnoringCase("TestClient_Charlie");
            assertThat(response).containsIgnoringCase("TestClient_Alpha");
            assertThat(response).containsIgnoringCase("TestClient_Beta");

            String expectedAnswer = String.format("""
                    Must apply the exact fixed bands from the question.
                    
                    Correct classification with current totals is:
                    - TestClient_Charlie (%s): high-value
                    - TestClient_Alpha (%s): medium-value
                    - TestClient_Beta (%s): low-value
                    """, charlieRevenue, alphaRevenue, betaRevenue);
            llmJudge.evaluateAnswerWithJudge(question, response, expectedAnswer);
        }

        @Test
        void testComparisonQuestion() {
            long alphaOrderCount = orderCountForClient("TestClient_Alpha");
            long betaOrderCount = orderCountForClient("TestClient_Beta");
            long charlieOrderCount = orderCountForClient("TestClient_Charlie");
            assertThat(alphaOrderCount).isEqualTo(2L);
            assertThat(betaOrderCount).isEqualTo(2L);
            assertThat(charlieOrderCount).isEqualTo(2L);

            BigDecimal alphaRevenue = totalRevenueForClient("TestClient_Alpha");
            BigDecimal betaRevenue = totalRevenueForClient("TestClient_Beta");
            BigDecimal charlieRevenue = totalRevenueForClient("TestClient_Charlie");
            assertThat(alphaRevenue).isEqualTo(new BigDecimal("5000.00"));
            assertThat(betaRevenue).isEqualTo(new BigDecimal("4000.00"));
            assertThat(charlieRevenue).isEqualTo(new BigDecimal("6000.00"));

            String question = """
                    Compare enterprise vs smaller clients using two explicit views:
                    
                    1) total segment ORDER amount and
                    2) average ORDER amount per client.
                    
                    Important: "per client" means segment total ORDER amount divided by
                    the number of clients in that segment. It does not mean average order value.
                    
                    In this demo dataset, use ORDER totals as profitability proxy (invoices may be missing).
                    
                    Use only these clients:
                    - enterprise segment: TestClient_Charlie
                    - smaller segment: TestClient_Alpha and TestClient_Beta
                    
                    State both winners explicitly (total-segment winner and per-client-average winner) and mention one data limitation.
                    """;
            String response = askBusinessQuestion(question);

            assertThat(response).containsAnyOf("TestClient_Beta", "TestClient_Alpha", "TestClient_Charlie");

            BigDecimal smallerSegmentTotal = alphaRevenue.add(betaRevenue);
            BigDecimal smallerSegmentAverage = smallerSegmentTotal.divide(new BigDecimal("2"));

            String expectedAnswer = String.format("""
                    Must compare the three specified clients using ORDER totals as profitability proxy.
                    
                    Expected order totals: TestClient_Beta %s, TestClient_Alpha %s, TestClient_Charlie %s.
                    
                    Segment totals: enterprise %s, smaller %s.
                    Per-client averages: enterprise %s, smaller %s.
                    
                    Should state that smaller wins on total segment amount, while enterprise wins on per-client average.
                    A brief limitation note about missing invoice/cost data is acceptable.
                    """, betaRevenue, alphaRevenue, charlieRevenue, charlieRevenue, smallerSegmentTotal, charlieRevenue, smallerSegmentAverage);
            llmJudge.evaluateAnswerWithJudge(question, response, expectedAnswer);
        }
    }

    @Nested
    class TrendAnalysis {

        private record TrendMetrics(
                long recentOrdersLast30Days,
                long firstWindowOrderCount,
                long secondWindowOrderCount,
                BigDecimal firstWindowRevenue,
                BigDecimal secondWindowRevenue
        ) {
        }

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void testTrendAnalysisQuestion() {
            long actualOrderCount = totalOrderCount();
            assertThat(actualOrderCount).isEqualTo(6L);

            LocalDate firstWindowStart = LocalDate.now().minusDays(30);
            LocalDate secondWindowStart = LocalDate.now().minusDays(15);
            LocalDate today = LocalDate.now();

            TrendMetrics trendMetrics = loadTrendMetrics(firstWindowStart, secondWindowStart, today);
            assertThat(trendMetrics.recentOrdersLast30Days()).isEqualTo(6L);
            assertThat(trendMetrics.firstWindowOrderCount()).isEqualTo(2L);
            assertThat(trendMetrics.secondWindowOrderCount()).isEqualTo(4L);
            assertThat(trendMetrics.firstWindowRevenue()).isEqualTo(new BigDecimal("5000.00"));
            assertThat(trendMetrics.secondWindowRevenue()).isEqualTo(new BigDecimal("10000.00"));

            String question = """
                    Analyze order momentum over the last 30 days using exact order-date windows, not whole calendar months.
                    
                    Compare these two windows:
                    - %s to %s
                    - %s to %s
                    
                    Tell me whether order volume is improving, weakening, or flat.
                    Compare both order count and order value, and mention briefly if the sample is still small.
                    """.formatted(
                    firstWindowStart,
                    secondWindowStart.minusDays(1),
                    secondWindowStart,
                    today
            );
            String response = askBusinessQuestion(question);

            assertThat(response).containsAnyOf("trend", "improving", "growing", "flat", "orders");

            String expectedAnswer = String.format("""
                            There are %d orders total and %d orders within the last 30 days.
                            
                            First window (%s to %s): %d orders and %s total order value.
                            Second window (%s to %s): %d orders and %s total order value.
                            
                            The response should conclude that recent momentum is improving/accelerating because the second window is stronger than the first, while briefly noting the sample is still small.
                            """,
                    actualOrderCount,
                    trendMetrics.recentOrdersLast30Days(),
                    firstWindowStart,
                    secondWindowStart.minusDays(1),
                    trendMetrics.firstWindowOrderCount(),
                    trendMetrics.firstWindowRevenue(),
                    secondWindowStart,
                    today,
                    trendMetrics.secondWindowOrderCount(),
                    trendMetrics.secondWindowRevenue()
            );
            llmJudge.evaluateAnswerWithJudge(question, response, expectedAnswer);
        }

        private TrendMetrics loadTrendMetrics(LocalDate firstWindowStart, LocalDate secondWindowStart, LocalDate today) {
            long recentOrdersLast30Days = countOrdersSince(firstWindowStart);
            long firstWindowOrderCount = countOrdersBetweenExclusiveEnd(firstWindowStart, secondWindowStart);
            long secondWindowOrderCount = countOrdersBetweenInclusiveEnd(secondWindowStart, today);
            BigDecimal firstWindowRevenue = sumOrdersBetweenExclusiveEnd(firstWindowStart, secondWindowStart);
            BigDecimal secondWindowRevenue = sumOrdersBetweenInclusiveEnd(secondWindowStart, today);

            return new TrendMetrics(
                    recentOrdersLast30Days,
                    firstWindowOrderCount,
                    secondWindowOrderCount,
                    firstWindowRevenue,
                    secondWindowRevenue
            );
        }

        private long countOrdersSince(LocalDate startDate) {
            return dataManager.loadValue(
                    "SELECT COUNT(o) FROM Order_ o WHERE o.date >= :startDate",
                    Long.class
            ).parameter("startDate", startDate).one();
        }

        private long countOrdersBetweenExclusiveEnd(LocalDate fromDate, LocalDate toDateExclusive) {
            return dataManager.loadValue(
                            "SELECT COUNT(o) FROM Order_ o WHERE o.date >= :fromDate AND o.date < :toDate",
                            Long.class
                    ).parameter("fromDate", fromDate)
                    .parameter("toDate", toDateExclusive)
                    .one();
        }

        private long countOrdersBetweenInclusiveEnd(LocalDate fromDate, LocalDate toDateInclusive) {
            return dataManager.loadValue(
                            "SELECT COUNT(o) FROM Order_ o WHERE o.date >= :fromDate AND o.date <= :toDate",
                            Long.class
                    ).parameter("fromDate", fromDate)
                    .parameter("toDate", toDateInclusive)
                    .one();
        }

        private BigDecimal sumOrdersBetweenExclusiveEnd(LocalDate fromDate, LocalDate toDateExclusive) {
            return dataManager.loadValue(
                            "SELECT COALESCE(SUM(o.total), 0) FROM Order_ o WHERE o.date >= :fromDate AND o.date < :toDate",
                            BigDecimal.class
                    ).parameter("fromDate", fromDate)
                    .parameter("toDate", toDateExclusive)
                    .one();
        }

        private BigDecimal sumOrdersBetweenInclusiveEnd(LocalDate fromDate, LocalDate toDateInclusive) {
            return dataManager.loadValue(
                            "SELECT COALESCE(SUM(o.total), 0) FROM Order_ o WHERE o.date >= :fromDate AND o.date <= :toDate",
                            BigDecimal.class
                    ).parameter("fromDate", fromDate)
                    .parameter("toDate", toDateInclusive)
                    .one();
        }
    }

    @Nested
    class ReportingScenarios {

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void testClient360ReportQuestion() {
            systemAuthenticator.runWithSystem(() -> {
                LocalDate fromDate = LocalDate.now().minusYears(1).withDayOfYear(1);
                LocalDate toDate = fromDate.with(TemporalAdjusters.lastDayOfYear());
                UUID clientId = createHighRiskClientWithOverdueInvoices(fromDate);

                String question = """
                        Retrieve the internal business risk assessment and indicators for client %s.
                        
                        Use report parameter aliases exactly as:
                        - client=%s
                        - fromDate=%s
                        - toDate=%s
                        - audience=ai
                        
                        Important: You must find the authoritative pre-calculated data for this in the system tools.
                        Do not try to derive or calculate these metrics yourself via ad-hoc combination of multiple queries,
                        as this could lead to incorrect results based on wrong business assumptions.
                        """.formatted(clientId, clientId, fromDate, toDate);
                String response = askBusinessQuestion(question);

                String expectedAnswer = String.format("""
                        Should use client-360-report to get comprehensive risk assessment for client %s.
                        
                        Client has 4 overdue invoices (INV-RISK-001 to INV-RISK-004) totaling 8000.00.
                        Expected to identify HIGH risk level due to multiple overdue invoices.
                        
                        Should demonstrate use of authoritative report data rather than manual calculations.
                        Response may vary in format but should reference the overdue invoices and high risk assessment.
                        """, clientId);
                llmJudge.evaluateAnswerWithJudge(question, response, expectedAnswer);
            });
        }

        private UUID createHighRiskClientWithOverdueInvoices(LocalDate yearReferenceDate) {
            Client client = entities.client("Risk_Gamma");

            IntStream.rangeClosed(1, 4).forEach(i -> {
                entities.createAndSaveEntity(Invoice.class, invoice -> {
                    invoice.setClient(client);
                    invoice.setNumber("INV-RISK-00" + i);
                    invoice.setTotal(new BigDecimal("2000.00"));
                    invoice.setDate(yearReferenceDate.withMonth(6).withDayOfMonth(i));
                    invoice.setStatus(InvoiceStatus.OVERDUE);
                });
            });

            return client.getId();
        }
    }

    @Nested
    class InteractiveLinks {

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void testInteractiveLinksGeneration() {
            String alphaClientId = clientIdFor("TestClient_Alpha").toString();
            String betaClientId = clientIdFor("TestClient_Beta").toString();
            String charlieClientId = clientIdFor("TestClient_Charlie").toString();
            assertThat(alphaClientId).isNotBlank();
            assertThat(betaClientId).isNotBlank();
            assertThat(charlieClientId).isNotBlank();

            String question = "Show me all our clients with their details";
            String response = askBusinessQuestion(question);

            assertThat(response).contains("clients/" + alphaClientId + ")");
            assertThat(response).contains("clients/" + betaClientId + ")");
            assertThat(response).contains("clients/" + charlieClientId + ")");
            assertThat(response).containsIgnoringCase("TestClient_Alpha");
            assertThat(response).containsIgnoringCase("TestClient_Beta");
            assertThat(response).containsIgnoringCase("TestClient_Charlie");

            String expectedAnswer = String.format("""
                    Must show all 3 clients (TestClient_Alpha, TestClient_Beta, TestClient_Charlie) with their details.
                    
                    Should generate markdown links for each client using format: clients/%s), clients/%s), clients/%s).
                    Should include client names and potentially other details like revenue, orders, etc.
                    """, alphaClientId, betaClientId, charlieClientId);
            llmJudge.evaluateAnswerWithJudge(question, response, expectedAnswer);
        }
    }

    @Nested
    class ChurnRiskAnalysis {

        private record ClientOrderWindow(
                long recentOrders,
                long historicalOrders
        ) {
        }

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void testChurnRiskAnalysisQuestion() {
            setupChurnRiskTestData();
            ClientOrderWindow deltaOrders = loadClientOrderWindow("TestClient_Delta");
            ClientOrderWindow echoOrders = loadClientOrderWindow("TestClient_Echo");
            ClientOrderWindow foxtrotOrders = loadClientOrderWindow("TestClient_Foxtrot");

            assertThat(deltaOrders.historicalOrders()).isGreaterThan(0L);
            assertThat(deltaOrders.recentOrders()).isLessThan(deltaOrders.historicalOrders());
            assertThat(echoOrders.historicalOrders()).isGreaterThan(0L);
            assertThat(echoOrders.recentOrders()).isEqualTo(0L);
            assertThat(foxtrotOrders.historicalOrders()).isGreaterThan(0L);
            assertThat(foxtrotOrders.recentOrders()).isGreaterThan(0L);

            String question = """
                    Identify churn risk using two criteria:
                    
                    1) no orders in the last 90 days, OR
                    2) clear decline in order activity compared to days 91-180.
                    
                    Focus on these clients: TestClient_Delta, TestClient_Echo, TestClient_Foxtrot.
                    Classify each client as risk/not-risk and explain briefly why, then give short mitigation recommendations.
                    """;
            String response = askBusinessQuestion(question);

            String expectedAnswer = String.format("""
                            Should identify at-risk accounts based on both criteria.
                            
                            Expected classification hints:
                            - TestClient_Delta is decline risk (%d recent vs %d historical orders).
                            - TestClient_Echo is inactivity risk (%d recent vs %d historical).
                            - TestClient_Foxtrot should be treated as stable (%d recent vs %d historical) or clearly lower risk.
                            
                            Should provide business recommendations and explain criteria.
                            """,
                    deltaOrders.recentOrders(),
                    deltaOrders.historicalOrders(),
                    echoOrders.recentOrders(),
                    echoOrders.historicalOrders(),
                    foxtrotOrders.recentOrders(),
                    foxtrotOrders.historicalOrders()
            );
            llmJudge.evaluateAnswerWithJudge(question, response, expectedAnswer);

            assertThat(response.length()).isGreaterThan(500);

            log.info("Churn risk analysis response: {}", response);
        }

        private ClientOrderWindow loadClientOrderWindow(String clientName) {
            long recentOrders = recentOrdersLast90DaysForClient(clientName);
            long historicalOrders = historicalOrdersDays91To180ForClient(clientName);

            return new ClientOrderWindow(recentOrders, historicalOrders);
        }

        private long recentOrdersLast90DaysForClient(String clientName) {
            return dataManager.loadValue(
                    "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :name AND @between(o.date, now-90, now, day)",
                    Long.class
            ).parameter("name", clientName).one();
        }

        private long historicalOrdersDays91To180ForClient(String clientName) {
            return dataManager.loadValue(
                    "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :name AND @between(o.date, now-180, now-91, day)",
                    Long.class
            ).parameter("name", clientName).one();
        }
    }

    @Nested
    class ConversationTitleGeneration {

        @BeforeEach
        void prepareKnownDataset() {
            setupKnownTestData();
        }

        @Test
        void businessQuestion_shouldUpdateConversationTitle() {
            String response = askBusinessQuestion("How many clients do we have in total?");
            assertThat(response).isNotBlank();

            AiConversation conversation = dataManager.load(AiConversation.class)
                    .id(UUID.fromString(conversationId))
                    .one();

            assertThat(conversation.getTitle()).isNotEqualTo(DEFAULT_TITLE);
        }
    }

    private void setupKnownTestData() {
        User manager = testUsers.manager();
        var testClient1 = entities.client("TestClient_Alpha");
        testClient1.setAccountManager(manager);
        entities.saveWithoutReload(testClient1);
        createTestOrder(testClient1, "ALPHA-001", new BigDecimal("2000.00"), LocalDate.now().minusDays(5));
        createTestOrder(testClient1, "ALPHA-002", new BigDecimal("3000.00"), LocalDate.now().minusDays(15));

        var testClient2 = entities.client("TestClient_Beta");
        testClient2.setAccountManager(manager);
        entities.saveWithoutReload(testClient2);
        createTestOrder(testClient2, "BETA-001", new BigDecimal("1500.00"), LocalDate.now().minusDays(10));
        createTestOrder(testClient2, "BETA-002", new BigDecimal("2500.00"), LocalDate.now().minusDays(20));

        var testClient3 = entities.client("TestClient_Charlie");
        testClient3.setAccountManager(manager);
        entities.saveWithoutReload(testClient3);
        createTestOrder(testClient3, "CHARLIE-001", new BigDecimal("3500.00"), LocalDate.now().minusDays(8));
        createTestOrder(testClient3, "CHARLIE-002", new BigDecimal("2500.00"), LocalDate.now().minusDays(18));

        log.info("Created test data: 3 clients with 2 orders each, total revenue 15000");
    }

    /**
     * Setup specific test data for churn risk analysis - creates clients with different risk profiles.
     */
    private void setupChurnRiskTestData() {
        var deltaClient = entities.client("TestClient_Delta");
        createTestOrder(deltaClient, "DELTA-H001", new BigDecimal("5000.00"), LocalDate.now().minusDays(120));
        createTestOrder(deltaClient, "DELTA-H002", new BigDecimal("4500.00"), LocalDate.now().minusDays(140));
        createTestOrder(deltaClient, "DELTA-H003", new BigDecimal("3000.00"), LocalDate.now().minusDays(160));
        createTestOrder(deltaClient, "DELTA-R001", new BigDecimal("1000.00"), LocalDate.now().minusDays(30));

        var echoClient = entities.client("TestClient_Echo");
        createTestOrder(echoClient, "ECHO-H001", new BigDecimal("6000.00"), LocalDate.now().minusDays(100));
        createTestOrder(echoClient, "ECHO-H002", new BigDecimal("7500.00"), LocalDate.now().minusDays(130));
        createTestOrder(echoClient, "ECHO-H003", new BigDecimal("4000.00"), LocalDate.now().minusDays(150));

        var foxtrotClient = entities.client("TestClient_Foxtrot");
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
        entities.order(client, orderNumber, date, com.company.crm.model.order.OrderStatus.DONE, total);
    }

    private String askBusinessQuestion(String question) {
        com.company.crm.ai.model.ChatMessage userMessage = aiConversationService.createUserMessage(
                conversation, question, java.util.List.of(), java.util.List.of());
        return analyticsService.processUserMessage(userMessage.getId());
    }

    private long totalClientCount() {
        return dataManager.loadValue("SELECT COUNT(c) FROM Client c", Long.class).one();
    }

    private BigDecimal totalOrderRevenue() {
        return dataManager.loadValue("SELECT COALESCE(SUM(o.total), 0) FROM Order_ o", BigDecimal.class).one();
    }

    private String topClientByRevenue() {
        return dataManager.loadValue(
                "SELECT c.name FROM Client c LEFT JOIN c.orders o GROUP BY c ORDER BY COALESCE(SUM(o.total), 0) DESC",
                String.class
        ).one();
    }

    private long totalOrderCount() {
        return dataManager.loadValue("SELECT COUNT(o) FROM Order_ o", Long.class).one();
    }

    private BigDecimal averageOrderValue() {
        return dataManager.loadValue("SELECT COALESCE(AVG(o.total), 0) FROM Order_ o", BigDecimal.class).one();
    }

    private BigDecimal totalRevenueForClient(String clientName) {
        return dataManager.loadValue(
                "SELECT COALESCE(SUM(o.total), 0) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
                BigDecimal.class
        ).parameter("clientName", clientName).one();
    }

    private long orderCountForClient(String clientName) {
        return dataManager.loadValue(
                "SELECT COUNT(o) FROM Client c LEFT JOIN c.orders o WHERE c.name = :clientName GROUP BY c",
                Long.class
        ).parameter("clientName", clientName).one();
    }

    private UUID clientIdFor(String clientName) {
        return dataManager.loadValue("SELECT c.id FROM Client c WHERE c.name = :name", UUID.class)
                .parameter("name", clientName)
                .one();
    }
}
