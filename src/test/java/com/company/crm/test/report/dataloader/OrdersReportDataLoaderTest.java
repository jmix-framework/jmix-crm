package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.dataloader.OrdersReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OrdersReportDataLoaderTest extends AbstractTest {

    @Autowired
    private OrdersReportDataLoader dataLoader;

    @Test
    void testLoadDataWithMultipleOrders() {
        // Given
        Client client = entities.client("Test Client");
        dataManager.save(client);

        Order order1 = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        order1.setNumber("ORD-001");
        order1.setTotal(BigDecimal.valueOf(1250.75));
        order1.setComment("First order");

        Order order2 = entities.order(client, LocalDate.of(2024, 1, 20), OrderStatus.NEW);
        order2.setNumber("ORD-002");
        order2.setTotal(BigDecimal.valueOf(750.50));
        order2.setComment("Second order");

        // Order outside date range - should not appear
        Order order3 = entities.order(client, LocalDate.of(2023, 12, 31), OrderStatus.DONE);
        order3.setNumber("ORD-003");
        order3.setTotal(BigDecimal.valueOf(500.00));

        dataManager.save(order1, order2, order3);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(2);

        // Orders should be ordered by date DESC (newest first)
        Map<String, Object> firstOrder = result.get(0);
        Map<String, Object> secondOrder = result.get(1);

        assertThat(firstOrder.get("number")).isEqualTo("ORD-002");
        assertThat(secondOrder.get("number")).isEqualTo("ORD-001");

        // Check all fields are present
        assertThat(firstOrder).containsKeys("number", "date", "dateFormatted", "status", "total", "comment");
    }

    @Test
    void testLoadDataWithNoOrders() {
        // Given
        Client client = entities.client("Empty Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void testLoadDataDateFiltering() {
        // Given
        Client client = entities.client("Date Filter Client");
        dataManager.save(client);

        Order orderInRange = entities.order(client, LocalDate.of(2024, 6, 15), OrderStatus.DONE);
        orderInRange.setNumber("IN-RANGE");

        Order orderBeforeRange = entities.order(client, LocalDate.of(2024, 5, 31), OrderStatus.DONE);
        orderBeforeRange.setNumber("BEFORE-RANGE");

        Order orderAfterRange = entities.order(client, LocalDate.of(2024, 7, 1), OrderStatus.DONE);
        orderAfterRange.setNumber("AFTER-RANGE");

        dataManager.save(orderInRange, orderBeforeRange, orderAfterRange);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("number")).isEqualTo("IN-RANGE");
    }

    @Test
    void testLoadDataMultipleClients() {
        // Given
        Client client1 = entities.client("Client 1");
        Client client2 = entities.client("Client 2");
        dataManager.save(client1, client2);

        Order orderClient1 = entities.order(client1, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        orderClient1.setNumber("CLIENT1-ORDER");

        Order orderClient2 = entities.order(client2, LocalDate.of(2024, 1, 16), OrderStatus.DONE);
        orderClient2.setNumber("CLIENT2-ORDER");

        dataManager.save(orderClient1, orderClient2);

        Map<String, Object> params = createParams(client1,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("number")).isEqualTo("CLIENT1-ORDER");
    }

    private Map<String, Object> createParams(Client client, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("client", client);
        params.put("fromDate", java.sql.Date.valueOf(fromDate));
        params.put("toDate", java.sql.Date.valueOf(toDate));
        return params;
    }
}