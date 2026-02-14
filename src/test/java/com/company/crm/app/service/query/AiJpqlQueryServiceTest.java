package com.company.crm.app.service.query;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jmix.query.AiJpqlQueryService;
import com.company.crm.model.client.Client;
import com.company.crm.util.extenstion.AuthenticatedAs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for JpqlQueryService focusing on automatic parameter type conversion
 */
@AuthenticatedAs(AuthenticatedAs.ADMIN_USERNAME)
class AiJpqlQueryServiceTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(AiJpqlQueryServiceTest.class);

    @Autowired
    private AiJpqlQueryService aiJpqlQueryService;

    @BeforeEach
    void setUp() {
        // Create test data
        var client1 = entities.client("Test Client 1");
        createTestOrder(client1, "TEST-001", new BigDecimal("1500.50"), LocalDate.now().minusDays(10));

        var client2 = entities.client("Test Client 2");
        createTestOrder(client2, "TEST-002", new BigDecimal("2500.75"), LocalDate.now().minusDays(5));
    }

    @Test
    void testParameterConversion_LocalDateString() {
        // Test automatic LocalDate conversion from string parameter
        String jpql = "SELECT o.number AS orderNumber, o.total AS orderTotal FROM Order_ o WHERE o.date >= :startDate";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", LocalDate.now().minusDays(15).toString()); // "2024-01-02" format

        var result = aiJpqlQueryService.executeJpqlQuery(jpql, parameters, Arrays.asList("orderNumber", "orderTotal"));

        assertThat(result.success()).as("Query should succeed with LocalDate string parameter").isTrue();
        assertThat(result.data()).as("Should return order data").isNotEmpty();
        assertThat(result.rowCount()).as("Should return both orders").isGreaterThanOrEqualTo(2);

        log.info("LocalDate parameter conversion test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testParameterConversion_NumericString() {
        // Test that numeric strings are passed through to JPQL engine for type conversion
        String jpql = "SELECT o.number AS orderNumber, o.total AS orderTotal FROM Order_ o WHERE o.total >= :minValue";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("minValue", "2000.00"); // String - JPQL engine will convert based on parameter type

        var result = aiJpqlQueryService.executeJpqlQuery(jpql, parameters, Arrays.asList("orderNumber", "orderTotal"));

        assertThat(result.success()).as("Query should succeed with numeric string parameter").isTrue();
        assertThat(result.data()).as("Should return high-value orders").isNotEmpty();
        assertThat(result.rowCount()).as("Should return at least one order >= 2000").isGreaterThanOrEqualTo(1);

        log.info("Numeric string parameter test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testParameterConversion_CollectionSizeString() {
        // Test that integer strings are passed through to JPQL engine for type conversion
        String jpql = "SELECT c.name AS clientName FROM Client c WHERE SIZE(c.orders) >= :minOrders";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("minOrders", "0"); // String - JPQL engine will convert to integer

        var result = aiJpqlQueryService.executeJpqlQuery(jpql, parameters, Arrays.asList("clientName"));

        assertThat(result.success()).as("Query should succeed with integer string parameter").isTrue();
    }

    @Test
    void testParameterConversion_StringPattern() {
        // Test string parameters remain as strings (LIKE patterns)
        String jpql = "SELECT c.name AS clientName FROM Client c WHERE c.name LIKE :pattern";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("pattern", "%Test%"); // LIKE pattern should remain string

        var result = aiJpqlQueryService.executeJpqlQuery(jpql, parameters, Arrays.asList("clientName"));

        assertThat(result.success()).as("Query should succeed with string LIKE parameter").isTrue();
        assertThat(result.data()).as("Should return matching clients").isNotEmpty();
        assertThat(result.rowCount()).as("Should return test clients").isGreaterThanOrEqualTo(2);

        log.info("String pattern parameter test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testParameterConversion_MixedTypes() {
        // Test mixed parameter types in single query - date converted, number passed through
        String jpql = "SELECT o.number AS orderNumber, o.total AS orderTotal FROM Order_ o " +
                     "WHERE o.date >= :startDate AND o.total >= :minValue";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", LocalDate.now().minusDays(20).toString()); // Will be converted to LocalDate
        parameters.put("minValue", "1000.00"); // Will remain string for JPQL engine to convert

        var result = aiJpqlQueryService.executeJpqlQuery(jpql, parameters, Arrays.asList("orderNumber", "orderTotal"));

        assertThat(result.success()).as("Query should succeed with mixed parameter types").isTrue();

        log.info("Mixed parameter types test passed: {} rows returned", result.rowCount());
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