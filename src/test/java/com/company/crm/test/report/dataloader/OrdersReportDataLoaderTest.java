package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.dataloader.OrdersReportDataLoader;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrdersReportDataLoaderTest extends AbstractTest {

    @Autowired
    private OrdersReportDataLoader dataLoader;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void testLoadDataWithMultipleOrders() {
        // given
        Client client = entities.client("Test Client");

        Order order1 = entities.order(client, "ORD-001", LocalDate.of(2024, 1, 15), OrderStatus.DONE, BigDecimal.valueOf(1250.75));
        order1.setComment("First order");
        saveWithoutReload(order1);

        Order order2 = entities.order(client, "ORD-002", LocalDate.of(2024, 1, 20), OrderStatus.NEW, BigDecimal.valueOf(750.50));
        order2.setComment("Second order");
        saveWithoutReload(order2);

        // Order outside date range - should not appear
        entities.order(client, "ORD-003", LocalDate.of(2023, 12, 31), OrderStatus.DONE, BigDecimal.valueOf(500.00));

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(2);

        // Orders should be ordered by date DESC (newest first)
        Map<String, Object> firstOrder = result.get(0);
        Map<String, Object> secondOrder = result.get(1);

        assertThat(firstOrder.get("date")).isEqualTo(LocalDate.of(2024, 1, 20));
        assertThat(secondOrder.get("date")).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(firstOrder.get("comment")).isEqualTo("Second order");
        assertThat(secondOrder.get("comment")).isEqualTo("First order");
        assertThat(firstOrder.get("total")).isNotEqualTo(secondOrder.get("total"));

        // Check all fields are present
        assertThat(firstOrder).containsKeys("number", "date", "dateFormatted", "status", "total", "comment");
        assertThat(firstOrder.get("status")).isInstanceOf(String.class);
        assertThat(((String) firstOrder.get("status"))).isNotBlank();
        assertThat(secondOrder.get("status")).isInstanceOf(String.class);
        assertThat(((String) secondOrder.get("status"))).isNotBlank();
        assertThat(firstOrder.get("dateFormatted")).isInstanceOf(String.class);
        assertThat(firstOrder.get("total")).isInstanceOf(String.class);
    }

    @Test
    void testLoadDataWithNoOrders() {
        // given
        Client client = entities.client("Empty Client");

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testLoadDataDateFiltering() {
        // given
        Client client = entities.client("Date Filter Client");
        entities.order(client, "IN-RANGE", LocalDate.of(2024, 6, 15), OrderStatus.DONE, BigDecimal.valueOf(100));
        entities.order(client, "BEFORE-RANGE", LocalDate.of(2024, 5, 31), OrderStatus.DONE, BigDecimal.valueOf(100));
        entities.order(client, "AFTER-RANGE", LocalDate.of(2024, 7, 1), OrderStatus.DONE, BigDecimal.valueOf(100));

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 6, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 6, 30))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("date")).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @Test
    void testLoadDataMultipleClients() {
        // given
        Client client1 = entities.client("Client 1");
        Client client2 = entities.client("Client 2");
        entities.order(client1, "CLIENT1-ORDER", LocalDate.of(2024, 1, 15), OrderStatus.DONE, BigDecimal.valueOf(101));
        entities.order(client2, "CLIENT2-ORDER", LocalDate.of(2024, 1, 16), OrderStatus.DONE, BigDecimal.valueOf(202));

        Map<String, Object> params = Map.of(
                "client", client1,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("date")).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.get(0).get("total")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.valueOf(101), datatypeFormatter));
    }

}
