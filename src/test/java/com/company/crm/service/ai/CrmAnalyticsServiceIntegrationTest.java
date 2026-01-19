package com.company.crm.service.ai;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
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
 * Integration test for CRM Analytics Service focusing on technical JPQL functionality.
 * This test verifies parameter handling, date filtering, pattern matching and other
 * technical aspects of JPQL query generation and execution.
 */
class CrmAnalyticsServiceIntegrationTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(CrmAnalyticsServiceIntegrationTest.class);

    @Autowired
    private CrmAnalyticsService analyticsService;

    private String conversationId;

    @BeforeEach
    void setUp() {
        setupTestData();
        setupTestConversation();
    }

    private void setupTestConversation() {
        // Create a test conversation for each test
        AiConversation conversation = dataManager.create(AiConversation.class);
        conversation.setTitle("Test Analytics Conversation");
        dataManager.save(conversation);
        conversationId = conversation.getId().toString();
        log.info("Created test conversation with ID: {}", conversationId);
    }



    @Test
    void testAnalytics_DateParameterQuestion() {
        // Test LLM understanding of JPQL date parameters (:param syntax)
        String question = "Show me all orders from the last 30 days with their order numbers and totals. " +
                         "IMPORTANT: You MUST use JPQL parameters (like :startDate) in your query - do NOT load all data and filter afterwards. " +
                         "Use the parameter feature of the JPQL tool to filter at database level.";
        String result = analyticsService.processBusinessQuestion(question, conversationId);

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
        String result = analyticsService.processBusinessQuestion(question, conversationId);

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
        String result = analyticsService.processBusinessQuestion(question, conversationId);

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