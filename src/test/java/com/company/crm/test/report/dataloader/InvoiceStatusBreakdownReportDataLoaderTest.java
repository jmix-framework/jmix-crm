package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.dataloader.InvoiceStatusBreakdownReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceStatusBreakdownReportDataLoaderTest extends AbstractTest {

    @Autowired
    private InvoiceStatusBreakdownReportDataLoader dataLoader;

    @Test
    void testLoadDataWithVariousStatuses() {
        // Given
        Client client = entities.client("Status Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        // Create invoices with different statuses
        Invoice invoice1 = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.NEW, LocalDate.of(2024, 1, 15));
        Invoice invoice2 = entities.invoice(client, order, BigDecimal.valueOf(750.00), InvoiceStatus.NEW, LocalDate.of(2024, 1, 16));
        Invoice invoice3 = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 17));
        Invoice invoice4 = entities.invoice(client, order, BigDecimal.valueOf(300.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 18));

        // Invoice outside date range - should not be included
        Invoice invoiceOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(200.00), InvoiceStatus.NEW, LocalDate.of(2023, 12, 31));

        dataManager.save(invoice1, invoice2, invoice3, invoice4, invoiceOutOfRange);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        // Should return one row for each InvoiceStatus enum value
        assertThat(result).hasSize(InvoiceStatus.values().length);

        // Find the breakdown for NEW status
        Map<String, Object> newStatus = result.stream()
            .filter(row -> "NEW".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        assertThat(newStatus.get("count")).isEqualTo(2L);
        assertThat(newStatus.get("amount")).isInstanceOf(String.class);
        assertThat(newStatus.get("statusFormatted")).isInstanceOf(String.class);

        // Find the breakdown for PAID status
        Map<String, Object> paidStatus = result.stream()
            .filter(row -> "PAID".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        assertThat(paidStatus.get("count")).isEqualTo(1L);

        // Find the breakdown for OVERDUE status
        Map<String, Object> overdueStatus = result.stream()
            .filter(row -> "OVERDUE".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        assertThat(overdueStatus.get("count")).isEqualTo(1L);

        // Check that all rows have the expected structure
        result.forEach(row -> {
            assertThat(row).containsKeys("status", "statusFormatted", "count", "amount");
            assertThat(row.get("count")).isInstanceOf(Long.class);
            assertThat(row.get("amount")).isInstanceOf(String.class);
        });
    }

    @Test
    void testLoadDataWithNoInvoices() {
        // Given
        Client client = entities.client("No Invoices Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        // Should still return one row for each status, but with count 0
        assertThat(result).hasSize(InvoiceStatus.values().length);

        result.forEach(row -> {
            assertThat(row.get("count")).isEqualTo(0L);
            assertThat(row.get("amount")).isInstanceOf(String.class);
        });
    }

    @Test
    void testLoadDataDateFiltering() {
        // Given
        Client client = entities.client("Date Filter Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        // Invoice in date range
        Invoice invoiceInRange = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.NEW, LocalDate.of(2024, 6, 15));

        // Invoice outside date range
        Invoice invoiceOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.NEW, LocalDate.of(2024, 7, 15));

        dataManager.save(invoiceInRange, invoiceOutOfRange);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        Map<String, Object> newStatus = result.stream()
            .filter(row -> "NEW".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        // Should count only the invoice in date range
        assertThat(newStatus.get("count")).isEqualTo(1L);
    }

    @Test
    void testLoadDataAlwaysReturnsAllStatuses() {
        // Given
        Client client = entities.client("All Statuses Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(InvoiceStatus.values().length);

        // Check that we have a row for each status
        for (InvoiceStatus status : InvoiceStatus.values()) {
            boolean found = result.stream()
                .anyMatch(row -> status.name().equals(row.get("status")));
            assertThat(found).as("Status %s should be present in breakdown", status.name()).isTrue();
        }
    }

    private Map<String, Object> createParams(Client client, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("client", client);
        params.put("fromDate", java.sql.Date.valueOf(fromDate));
        params.put("toDate", java.sql.Date.valueOf(toDate));
        return params;
    }
}