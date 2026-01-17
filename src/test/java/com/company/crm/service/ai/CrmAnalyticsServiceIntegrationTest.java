package com.company.crm.service.ai;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.ai.CrmAnalyticsService;
import com.company.crm.model.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Integration test for CRM Analytics Service focusing on LLM-based question answering.
 * This test creates known test data and verifies that human-readable questions
 * are correctly interpreted and answered by the AI system.
 */
class CrmAnalyticsServiceIntegrationTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsServiceIntegrationTest.class);

    @Autowired
    private CrmAnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    @Test
    void testAnalytics_ClientCountQuestion() {
        String question = "How many clients do we have in total?";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Analytics result should not be null").isNotNull();
        assertThat(result.trim()).as("Analytics result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention clients or count").containsAnyOf("client", "total", "count", "5");
        assertThat(result.length()).as("Result should be a reasonable response").isGreaterThan(50);

        log.info("Client count question result: {}", result);
    }

    @Test
    void testAnalytics_RecentOrdersQuestion() {
        String question = "How many orders have we received in the last month?";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Analytics result should not be null").isNotNull();
        assertThat(result.trim()).as("Analytics result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention recent orders").containsAnyOf("order", "month", "recent");
        assertThat(result.length()).as("Result should be a comprehensive response").isGreaterThan(50);

        log.info("Recent orders question result: {}", result);
    }


    @Test
    void testAnalytics_TopClientsQuestion() {
        String question = "Which are our top 3 clients by revenue? Please give me their names and total order values.";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Top clients result should not be null").isNotNull();
        assertThat(result.trim()).as("Top clients result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention top clients or revenue").containsAnyOf("client", "revenue", "top", "enterprise", "techcorp", "global");
        assertThat(result.length()).as("Result should be a comprehensive analysis").isGreaterThan(100);

        log.info("Top clients question result: {}", result);
    }

    @Test
    void testAnalytics_ClientPerformanceAnalysis() {
        String question = "Which clients have the highest order volumes this quarter? Please analyze their performance trends.";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Performance analysis result should not be null").isNotNull();
        assertThat(result.trim()).as("Performance analysis result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention performance or clients").containsAnyOf("client", "order", "performance", "volume", "quarter");
        assertThat(result.length()).as("Result should be a comprehensive analysis").isGreaterThan(100);

        log.info("Client performance analysis result: {}", result);
    }

    @Test
    void testAnalytics_RevenueAnalysisQuestion() {
        String question = "What is our total revenue for this quarter, and how does it compare to our average order value?";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Revenue analysis result should not be null").isNotNull();
        assertThat(result.trim()).as("Revenue analysis result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention revenue or financial metrics").containsAnyOf("revenue", "total", "quarter", "average", "order", "value");
        assertThat(result.length()).as("Result should be a comprehensive analysis").isGreaterThan(100);

        log.info("Revenue analysis question result: {}", result);
    }

    @Test
    void testAnalytics_ClientSegmentationQuestion() {
        String question = "Can you segment our clients into high-value, medium-value, and low-value based on their order history?";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Client segmentation result should not be null").isNotNull();
        assertThat(result.trim()).as("Client segmentation result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention client segmentation").containsAnyOf("client", "segment", "high", "medium", "low", "value");
        assertThat(result.length()).as("Result should be a comprehensive analysis").isGreaterThan(100);

        log.info("Client segmentation question result: {}", result);
    }

    @Test
    void testAnalytics_TrendAnalysisQuestion() {
        String question = "What are the trends in our order volumes over the past few months? Are we growing or declining?";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Trend analysis result should not be null").isNotNull();
        assertThat(result.trim()).as("Trend analysis result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention trends or growth").containsAnyOf("trend", "order", "volume", "month", "grow", "declin");
        assertThat(result.length()).as("Result should be a comprehensive analysis").isGreaterThan(100);

        log.info("Trend analysis question result: {}", result);
    }

    @Test
    void testAnalytics_ComparisonQuestion() {
        String question = "Compare the performance of our enterprise clients versus our smaller clients. Which segment is more profitable?";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Comparison result should not be null").isNotNull();
        assertThat(result.trim()).as("Comparison result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention comparison or performance").containsAnyOf("enterprise", "client", "compare", "performance", "profitable", "segment");
        assertThat(result.length()).as("Result should be a comprehensive analysis").isGreaterThan(100);

        log.info("Comparison question result: {}", result);
    }

    @Test
    void testAnalytics_DateParameterQuestion() {
        // Test LLM understanding of JPQL date parameters (:param syntax)
        String question = "Show me all orders from the last 30 days with their order numbers and totals. " +
                         "IMPORTANT: You MUST use JPQL parameters (like :startDate) in your query - do NOT load all data and filter afterwards. " +
                         "Use the parameter feature of the JPQL tool to filter at database level.";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Date parameter result should not be null").isNotNull();
        assertThat(result.trim()).as("Date parameter result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention recent orders or dates").containsAnyOf("order", "recent", "30", "day");
        assertThat(result.length()).as("Result should be a comprehensive response").isGreaterThan(50);

        log.info("Date parameter question result: {}", result);
    }

    @Test
    void testAnalytics_MinimumValueParameterQuestion() {
        // Test LLM understanding of JPQL value parameters
        String question = "Find all orders with a value of at least 10000 euros. Show me the client names and order totals. " +
                         "CRITICAL: You MUST use JPQL WHERE clause with parameters (like :minValue) - never load all orders and filter in code. " +
                         "Use database-level filtering with the parameters feature.";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Value parameter result should not be null").isNotNull();
        assertThat(result.trim()).as("Value parameter result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention high-value orders").containsAnyOf("order", "client", "10000", "value", "euro");
        assertThat(result.length()).as("Result should be a comprehensive response").isGreaterThan(50);

        log.info("Minimum value parameter question result: {}", result);
    }

    @Test
    void testAnalytics_StringPatternParameterQuestion() {
        // Test LLM understanding of JPQL LIKE parameters with patterns
        String question = "Show me all clients whose name contains 'Corp' or 'Enterprise' and count their orders. " +
                         "MANDATORY: Use JPQL WHERE clause with LIKE parameters (e.g., :pattern) - do not fetch all clients and search afterwards. " +
                         "Filter must happen at database level using the query tool's parameter functionality.";
        String result = analyticsService.processBusinessQuestion(question);

        assertThat(result).as("Pattern parameter result should not be null").isNotNull();
        assertThat(result.trim()).as("Pattern parameter result should not be empty").isNotEmpty();
        assertThat(result.toLowerCase()).as("Result should mention corp/enterprise clients").containsAnyOf("corp", "enterprise", "client", "count", "order");
        assertThat(result.length()).as("Result should be a comprehensive response").isGreaterThan(50);

        log.info("String pattern parameter question result: {}", result);
    }

    /**
     * Setup comprehensive test data for AI-driven analytics questions.
     * This creates a realistic dataset with diverse clients and order patterns
     * that can be used to test various analytical questions.
     */
    private void setupTestData() {
        var enterpriseClient1 = entities.client("TechCorp Enterprise Solutions");
        createTestOrder(enterpriseClient1, "ENT-001", new BigDecimal("15000.00"), LocalDate.now().minusDays(5));
        createTestOrder(enterpriseClient1, "ENT-002", new BigDecimal("22000.00"), LocalDate.now().minusDays(15));
        createTestOrder(enterpriseClient1, "ENT-003", new BigDecimal("18000.00"), LocalDate.now().minusDays(25));

        var enterpriseClient2 = entities.client("Global Industries Corp");
        createTestOrder(enterpriseClient2, "GLB-001", new BigDecimal("12000.00"), LocalDate.now().minusDays(8));
        createTestOrder(enterpriseClient2, "GLB-002", new BigDecimal("16000.00"), LocalDate.now().minusDays(18));

        // Create mid-size clients
        var midClient1 = entities.client("MidSize Manufacturing");
        createTestOrder(midClient1, "MID-001", new BigDecimal("5000.00"), LocalDate.now().minusDays(10));
        createTestOrder(midClient1, "MID-002", new BigDecimal("7500.00"), LocalDate.now().minusDays(20));
        createTestOrder(midClient1, "MID-003", new BigDecimal("6200.00"), LocalDate.now().minusWeeks(6));

        var midClient2 = entities.client("Regional Services LLC");
        createTestOrder(midClient2, "REG-001", new BigDecimal("4500.00"), LocalDate.now().minusDays(12));
        createTestOrder(midClient2, "REG-002", new BigDecimal("3800.00"), LocalDate.now().minusMonths(2));

        // Create smaller clients
        var smallClient = entities.client("StartupXYZ");
        createTestOrder(smallClient, "SML-001", new BigDecimal("1200.00"), LocalDate.now().minusDays(30));
        createTestOrder(smallClient, "SML-002", new BigDecimal("800.00"), LocalDate.now().minusMonths(3));

        log.info("Created comprehensive test data with 5 clients and various order patterns for AI analysis");
    }

    /**
     * Create a test order with specific values and date
     */
    private void createTestOrder(Client client, String orderNumber, BigDecimal total, LocalDate date) {
        var order = dataManager.create(com.company.crm.model.order.Order.class);
        order.setNumber(orderNumber);
        order.setClient(client);
        order.setTotal(total);
        order.setDate(date);
        dataManager.save(order);
    }
}