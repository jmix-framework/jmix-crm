package com.company.crm.test.ai.jpql.query;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jpql.query.AiJpqlQueryService;
import com.company.crm.ai.jpql.query.JpqlParameters;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for JpqlQueryService focusing on automatic parameter type conversion
 */
@AuthenticatedAs(AuthenticatedAs.ADMIN_USERNAME)
class AiJpqlQueryServiceTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(AiJpqlQueryServiceTest.class);
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2024, 2, 20);

    @Autowired
    private AiJpqlQueryService aiJpqlQueryService;

    @BeforeEach
    void setUp() {
        // Create test data
        var client1 = entities.client("Test Client 1");
        createTestOrder(client1, "TEST-001", new BigDecimal("1500.50"), REFERENCE_DATE.minusDays(10));

        var client2 = entities.client("Test Client 2");
        createTestOrder(client2, "TEST-002", new BigDecimal("2500.75"), REFERENCE_DATE.minusDays(5));
    }

    @Test
    void testParameterConversion_LocalDateString() {
        // given
        var client = entities.client("LocalDate Param Client");
        createTestOrder(client, "LOCALDATE-001", new BigDecimal("1500.50"), REFERENCE_DATE.minusDays(10));

        String jpql = "SELECT o.number AS orderNumber, o.total AS orderTotal " +
                "FROM Order_ o WHERE o.date >= :startDate AND o.number LIKE 'LOCALDATE-%'";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", REFERENCE_DATE.minusDays(15).toString());

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.fromMap(parameters), Arrays.asList("orderNumber", "orderTotal"), 0, 10);

        // then
        assertThat(result.success()).as("Query should succeed with LocalDate string parameter").isTrue();
        assertThat(result.rowCount()).as("Should return the local test order").isEqualTo(1);
        assertThat(result.data().stream().map(row -> row.get("orderNumber")).toList())
                .containsExactly("LOCALDATE-001");

        log.info("LocalDate parameter conversion test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testParameterConversion_NumericString() {
        // given
        var client = entities.client("Numeric Param Client");
        createTestOrder(client, "NUM-001", new BigDecimal("2500.75"), REFERENCE_DATE.minusDays(5));

        String jpql = "SELECT o.number AS orderNumber, o.total AS orderTotal " +
                "FROM Order_ o WHERE o.total >= :minValue AND o.number LIKE 'NUM-%'";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("minValue", "2000.00");

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.fromMap(parameters), Arrays.asList("orderNumber", "orderTotal"), 0, 10);

        // then
        assertThat(result.success()).as("Query should succeed with numeric string parameter").isTrue();
        assertThat(result.rowCount()).as("Should return the local high-value order").isEqualTo(1);
        assertThat(result.data().stream().map(row -> row.get("orderNumber")).toList())
                .containsExactly("NUM-001");

        log.info("Numeric string parameter test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testParameterConversion_CollectionSizeString() {
        // given
        String jpql = "SELECT c.name AS clientName FROM Client c WHERE SIZE(c.orders) >= :minOrders";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("minOrders", "0");

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.fromMap(parameters), List.of("clientName"), 0, 10);

        // then
        assertThat(result.success()).as("Query should succeed with integer string parameter").isTrue();
        assertThat(result.rowCount()).isEqualTo(2);
        assertThat(result.data().stream().map(row -> row.get("clientName")).toList())
                .containsExactlyInAnyOrder("Test Client 1", "Test Client 2");
    }

    @Test
    void testParameterConversion_StringPattern() {
        // given
        String jpql = "SELECT c.name AS clientName FROM Client c WHERE c.name LIKE :pattern";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("pattern", "%Test%");

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.fromMap(parameters), List.of("clientName"), 0, 10);

        // then
        assertThat(result.success()).as("Query should succeed with string LIKE parameter").isTrue();
        assertThat(result.rowCount()).as("Should return the two seeded clients").isEqualTo(2);
        assertThat(result.data().stream().map(row -> row.get("clientName")).toList())
                .containsExactlyInAnyOrder("Test Client 1", "Test Client 2");

        log.info("String pattern parameter test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testUnknownExtraParameterIsIgnored() {
        // given
        var client = entities.client("Extra Param Client");
        String jpql = "SELECT c.name AS clientName FROM Client c WHERE c.name = :clientName";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("clientName", client.getName());
        parameters.put("unusedParameter", "not referenced by the JPQL");

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(
                jpql,
                JpqlParameters.fromMap(parameters),
                List.of("clientName"),
                0,
                10
        );

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data().stream().map(row -> row.get("clientName")).toList())
                .containsExactly("Extra Param Client");
    }

    @Test
    void testKnownWronglyTypedParameterFallsBackToOriginalValue() {
        // given
        var client = entities.client("2024-01-15");
        String jpql = "SELECT c.name AS clientName FROM Client c WHERE c.name = :clientName";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("clientName", client.getName());

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(
                jpql,
                JpqlParameters.fromMap(parameters),
                List.of("clientName"),
                0,
                10
        );

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data().stream().map(row -> row.get("clientName")).toList())
                .containsExactly("2024-01-15");
    }

    @Test
    void testParameterConversion_MixedTypes() {
        // given
        var client = entities.client("Mixed Param Client");
        createTestOrder(client, "MIX-001", new BigDecimal("1500.50"), REFERENCE_DATE.minusDays(10));
        createTestOrder(client, "MIX-002", new BigDecimal("2500.75"), REFERENCE_DATE.minusDays(5));

        String jpql = "SELECT o.number AS orderNumber, o.total AS orderTotal FROM Order_ o " +
                "WHERE o.date >= :startDate AND o.total >= :minValue AND o.number LIKE 'MIX-%'";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", REFERENCE_DATE.minusDays(20).toString());
        parameters.put("minValue", "1000.00");

        // when
        var result = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.fromMap(parameters), Arrays.asList("orderNumber", "orderTotal"), 0, 10);

        // then
        assertThat(result.success()).as("Query should succeed with mixed parameter types").isTrue();
        assertThat(result.rowCount()).isEqualTo(2);
        assertThat(result.data().stream().map(row -> row.get("orderNumber")).toList())
                .containsExactlyInAnyOrder("MIX-001", "MIX-002");

        log.info("Mixed parameter types test passed: {} rows returned", result.rowCount());
    }

    @Test
    void testPagination() {
        // Test pagination and hasMore detection with unique prefix to avoid demo data interference
        var client = entities.client("Pagination Client");

        var order1 = entities.order(client, REFERENCE_DATE, com.company.crm.model.order.OrderStatus.DONE, BigDecimal.TEN);
        order1.setNumber("PAG-TEST-001");
        dataManager.save(order1);

        var order2 = entities.order(client, REFERENCE_DATE, com.company.crm.model.order.OrderStatus.DONE, BigDecimal.TEN);
        order2.setNumber("PAG-TEST-002");
        dataManager.save(order2);

        String jpql = "SELECT o.number AS orderNumber FROM Order_ o WHERE o.number LIKE 'PAG-TEST-%' ORDER BY o.number ASC";

        // Request limit 1, we have 2 orders with this prefix
        var result = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.empty(), List.of("orderNumber"), 0, 1);

        assertThat(result.success()).isTrue();
        assertThat(result.rowCount()).isEqualTo(1);
        assertThat(result.hasMore()).as("Should have more rows because 2 rows exist but limit is 1").isTrue();
        assertThat(result.offset()).isEqualTo(0);
        assertThat(result.limit()).isEqualTo(1);
        assertThat(result.data().getFirst().get("orderNumber")).isEqualTo("PAG-TEST-001");

        // Request next page
        var secondPageResult = aiJpqlQueryService.executeJpqlQuery(jpql, JpqlParameters.empty(), List.of("orderNumber"), 1, 1);

        assertThat(secondPageResult.success()).isTrue();
        assertThat(secondPageResult.rowCount()).isEqualTo(1);
        assertThat(secondPageResult.hasMore()).as("Should NOT have more rows").isFalse();
        assertThat(secondPageResult.offset()).isEqualTo(1);
        assertThat(secondPageResult.data().getFirst().get("orderNumber")).isEqualTo("PAG-TEST-002");
    }

    /**
     * Create a test order with specific values and date
     */
    private void createTestOrder(Client client, String orderNumber, BigDecimal total, LocalDate date) {
        var order = dataManager.create(com.company.crm.model.order.Order.class);
        order.setClient(client);
        order.setTotal(total);
        order.setDate(date);
        order = dataManager.save(order);
        order.setNumber(orderNumber);
        dataManager.save(order);
    }
}
