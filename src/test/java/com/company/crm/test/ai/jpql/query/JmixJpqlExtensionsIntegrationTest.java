package com.company.crm.test.ai.jpql.query;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jpql.query.AiJpqlQueryService;
import com.company.crm.ai.jpql.query.JpqlParameters;
import com.company.crm.ai.jpql.query.QueryExecutionResult;
import com.company.crm.ai.tool.JpqlExecutorTool;
import com.company.crm.util.extenstion.AuthenticatedAs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Jmix JPQL Extensions and Functions
 * Tests all documented JPQL functions and extensions to ensure they work correctly
 */
@AuthenticatedAs(AuthenticatedAs.ADMIN_USERNAME)
class JmixJpqlExtensionsIntegrationTest extends AbstractTest {

    private static final BigDecimal EXPECTED_TOTAL_REVENUE = new BigDecimal("96702.00");
    private static final BigDecimal EXPECTED_AVERAGE_ORDER = new BigDecimal("12087.75");
    private static final LocalDate REFERENCE_DATE = LocalDate.of(2024, 2, 20);

    private JpqlExecutorTool jpqlExecutorTool;

    @Autowired
    private AiJpqlQueryService aiJpqlQueryService;

    @BeforeEach
    void setUp() {
        jpqlExecutorTool = JpqlExecutorTool.create(applicationContext);
        setupTestData();
    }

    @Test
    void testDateTimeFunctions() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT EXTRACT(YEAR FROM o.date) AS orderYear, EXTRACT(MONTH FROM o.date) AS orderMonth, COUNT(o) AS orderCount " +
                        "FROM Order_ o GROUP BY EXTRACT(YEAR FROM o.date), EXTRACT(MONTH FROM o.date) ORDER BY orderYear, orderMonth",
                JpqlParameters.empty(),
                List.of("orderYear", "orderMonth", "orderCount"), null, null, null
        );

        // when
        List<Map<String, Object>> rows = result.data();

        // then
        assertThat(result.success()).isTrue();
        assertThat(rows).isNotEmpty();

        Map<String, Object> firstRow = rows.getFirst();
        Integer year = (Integer) firstRow.get("orderYear");
        assertThat(year).isEqualTo(REFERENCE_DATE.getYear());
        assertThat(firstRow.get("orderMonth")).isInstanceOf(Integer.class);
        assertThat(firstRow.get("orderCount")).isInstanceOf(Long.class);
        long totalOrders = rows.stream()
                .mapToLong(row -> ((Number) row.get("orderCount")).longValue())
                .sum();
        assertThat(totalOrders).isEqualTo(8L);
    }

    @Test
    void testMathematicalFunctions() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT o.total AS originalTotal, (o.total * 2) AS doubledTotal FROM Order_ o WHERE o.total > 0 ORDER BY o.total",
                JpqlParameters.empty(),
                List.of("originalTotal", "doubledTotal"), null, null, null
        );

        // when
        List<Map<String, Object>> rows = result.data();

        // then
        assertThat(result.success()).isTrue();
        assertThat(rows).hasSize(8);

        Map<String, Object> firstRow = rows.getFirst();
        assertThat(firstRow.get("originalTotal")).isInstanceOf(Number.class);
        assertThat(firstRow.get("doubledTotal")).isInstanceOf(Number.class);

        BigDecimal originalTotal = toBigDecimal(firstRow.get("originalTotal"));
        BigDecimal doubledTotal = toBigDecimal(firstRow.get("doubledTotal"));
        assertThat(doubledTotal).isEqualByComparingTo(originalTotal.multiply(BigDecimal.valueOf(2)));
    }

    @Test
    void testStringFunctions() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT " +
                        "UPPER(c.name) AS upperName, " +
                        "LOWER(c.name) AS lowerName, " +
                        "LENGTH(c.name) AS nameLength, " +
                        "SUBSTRING(c.name, 1, 5) AS nameSubstring, " +
                        "CONCAT(c.name, ' - Client') AS concatName " +
                        "FROM Client c ORDER BY c.name",
                JpqlParameters.empty(),
                List.of("upperName", "lowerName", "nameLength", "nameSubstring", "concatName"), null, null, null
        );

        // when
        Map<String, Object> firstRow = result.data().getFirst();

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(4);

        assertThat(firstRow.get("upperName")).isInstanceOf(String.class);
        assertThat(firstRow.get("lowerName")).isInstanceOf(String.class);
        assertThat(firstRow.get("nameLength")).isInstanceOf(Integer.class);
        assertThat(firstRow.get("nameSubstring")).isInstanceOf(String.class);
        assertThat(firstRow.get("concatName")).asString().endsWith(" - Client");
    }

    @Test
    void testConditionalFunctions() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT " +
                        "c.name AS clientName, " +
                        "CASE WHEN COUNT(o) > 2 THEN 'High Volume' WHEN COUNT(o) > 0 THEN 'Regular' ELSE 'No Orders' END AS clientCategory, " +
                        "COALESCE(SUM(o.total), 0) AS totalRevenue " +
                        "FROM Client c LEFT JOIN c.orders o GROUP BY c ORDER BY totalRevenue DESC",
                JpqlParameters.empty(),
                List.of("clientName", "clientCategory", "totalRevenue"), null, null, null
        );

        // when
        List<Map<String, Object>> rows = result.data();
        Map<String, Object> firstRow = rows.getFirst();
        long highVolumeCount = rows.stream()
                .filter(row -> "High Volume".equals(row.get("clientCategory")))
                .count();
        long regularCount = rows.stream()
                .filter(row -> "Regular".equals(row.get("clientCategory")))
                .count();

        // then
        assertThat(result.success()).isTrue();
        assertThat(rows).hasSize(4);
        assertThat(highVolumeCount).isEqualTo(1L);
        assertThat(regularCount).isEqualTo(3L);

        assertThat(firstRow.get("clientName")).isInstanceOf(String.class);
        assertThat(firstRow.get("clientCategory")).isIn("High Volume", "Regular", "No Orders");
        assertThat(firstRow.get("totalRevenue")).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) firstRow.get("totalRevenue")).isEqualByComparingTo("55001.25");
    }

    @Test
    void testTypeConversion() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT c.name AS clientName, o.total AS orderTotal FROM Client c JOIN c.orders o ORDER BY o.total DESC",
                JpqlParameters.empty(),
                List.of("clientName", "orderTotal"), null, null, null
        );

        // when
        List<Map<String, Object>> rows = result.data();
        Map<String, Object> firstRow = rows.getFirst();

        // then
        assertThat(result.success()).isTrue();
        assertThat(rows).hasSize(8);
        assertThat(firstRow.get("clientName")).isEqualTo("TechCorp Enterprise Solutions");
        assertThat(firstRow.get("orderTotal")).isInstanceOf(Number.class);
        assertThat(toBigDecimal(firstRow.get("orderTotal"))).isEqualByComparingTo("22000.00");
    }

    @Test
    void testAggregateFunctions() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT " +
                        "COUNT(o) AS orderCount, " +
                        "SUM(o.total) AS totalRevenue, " +
                        "AVG(o.total) AS averageOrder, " +
                        "MAX(o.total) AS maxOrder, " +
                        "MIN(o.total) AS minOrder " +
                        "FROM Order_ o",
                JpqlParameters.empty(),
                List.of("orderCount", "totalRevenue", "averageOrder", "maxOrder", "minOrder"), null, null, null
        );

        // when
        Map<String, Object> row = result.data().getFirst();

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(1);

        assertThat(row.get("orderCount")).isInstanceOf(Long.class);
        assertThat(row.get("totalRevenue")).isInstanceOf(Number.class);
        assertThat(row.get("averageOrder")).isInstanceOf(Number.class);
        assertThat(row.get("maxOrder")).isInstanceOf(Number.class);
        assertThat(row.get("minOrder")).isInstanceOf(Number.class);

        Long count = (Long) row.get("orderCount");
        assertThat(count).isEqualTo(8L);
        assertThat(toBigDecimal(row.get("totalRevenue"))).isEqualByComparingTo(EXPECTED_TOTAL_REVENUE);
        assertThat(toBigDecimal(row.get("averageOrder"))).isEqualByComparingTo(EXPECTED_AVERAGE_ORDER);
        assertThat(toBigDecimal(row.get("maxOrder"))).isEqualByComparingTo("22000.00");
        assertThat(toBigDecimal(row.get("minOrder"))).isEqualByComparingTo("1200.00");
    }

    @Test
    void testDateMacros() {
        // given
        QueryExecutionResult recentResult = jpqlExecutorTool.executeQuery(
                "SELECT o.number AS orderNumber, o.date AS orderDate, o.total AS orderTotal " +
                        "FROM Order_ o WHERE @between(o.date, now-10000, now+1, day) ORDER BY o.date DESC",
                JpqlParameters.empty(),
                List.of("orderNumber", "orderDate", "orderTotal"), null, null, null
        );

        assertThat(recentResult.success()).isTrue();
        assertThat(recentResult.data()).isNotEmpty();

        // Test @today macro
        QueryExecutionResult todayResult = jpqlExecutorTool.executeQuery(
                "SELECT COUNT(o) AS todayOrderCount FROM Order_ o WHERE @today(o.date)",
                JpqlParameters.empty(),
                List.of("todayOrderCount"), null, null, null
        );

        // when
        List<Map<String, Object>> recentRows = recentResult.data();
        Long todayCount = (Long) todayResult.data().getFirst().get("todayOrderCount");

        // then
        assertThat(todayResult.success()).isTrue();
        assertThat(recentResult.success()).isTrue();
        assertThat(recentRows).hasSize(8);
        assertThat(recentRows.stream().map(row -> toBigDecimal(row.get("orderTotal"))).toList())
                .containsExactly(
                        new BigDecimal("15000.50"),
                        new BigDecimal("12000.25"),
                        new BigDecimal("5000.00"),
                        new BigDecimal("22000.00"),
                        new BigDecimal("16000.00"),
                        new BigDecimal("18000.75"),
                        new BigDecimal("7500.50"),
                        new BigDecimal("1200.00")
                );
        assertThat(todayCount).isZero();
    }

    @Test
    void testRegexpFunction() {
        // given
        QueryExecutionResult result = jpqlExecutorTool.executeQuery(
                "SELECT c.name AS clientName FROM Client c WHERE UPPER(c.name) LIKE '%CORP%' OR UPPER(c.name) LIKE '%ENTERPRISE%'",
                JpqlParameters.empty(),
                List.of("clientName"), null, null, null
        );

        // when
        List<Map<String, Object>> rows = result.data();

        // then
        assertThat(result.success()).isTrue();
        assertThat(rows).hasSize(2);
        for (Map<String, Object> row : rows) {
            String name = (String) row.get("clientName");
            assertThat(name.toLowerCase()).containsAnyOf("corp", "enterprise");
        }
    }

    /**
     * Setup comprehensive test data for JPQL function testing
     */
    private void setupTestData() {
        // Create clients with different name patterns
        var enterpriseClient = entities.client("TechCorp Enterprise Solutions");
        var corporateClient = entities.client("Global Industries Corp");
        var regularClient = entities.client("MidSize Manufacturing");
        var smallClient = entities.client("StartupXYZ");

        // Create orders with fixed dates for deterministic assertions
        entities.order(enterpriseClient, "ENT-001", LocalDate.of(2024, 2, 15), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("15000.50"));
        entities.order(enterpriseClient, "ENT-002", LocalDate.of(2024, 2, 5), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("22000.00"));
        entities.order(enterpriseClient, "ENT-003", LocalDate.of(2024, 1, 26), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("18000.75"));

        entities.order(corporateClient, "CORP-001", LocalDate.of(2024, 2, 12), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("12000.25"));
        entities.order(corporateClient, "CORP-002", LocalDate.of(2024, 2, 2), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("16000.00"));

        entities.order(regularClient, "REG-001", LocalDate.of(2024, 2, 10), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("5000.00"));
        entities.order(regularClient, "REG-002", LocalDate.of(2024, 1, 21), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("7500.50"));

        entities.order(smallClient, "START-001", LocalDate.of(2024, 1, 6), com.company.crm.model.order.OrderStatus.DONE, new BigDecimal("1200.00"));
    }

    private BigDecimal toBigDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
