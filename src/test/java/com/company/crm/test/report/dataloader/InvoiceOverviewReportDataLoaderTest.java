package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.report.dataloader.InvoiceOverviewReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceOverviewReportDataLoaderTest extends AbstractTest {

    @Autowired
    private InvoiceOverviewReportDataLoader dataLoader;

    @Test
    void testLoadDataWithCompleteFinancialData() {
        // Given
        Client client = entities.client("Financial Client");
        dataManager.save(client);

        // Create orders and invoices
        Order order1 = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        Order order2 = entities.order(client, LocalDate.of(2024, 1, 20), OrderStatus.DONE);
        dataManager.save(order1, order2);

        Invoice invoice1 = entities.invoice(client, order1, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 15));
        Invoice invoice2 = entities.invoice(client, order2, BigDecimal.valueOf(750.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 20));

        // Invoice outside date range - should not be counted
        Invoice invoice3 = entities.invoice(client, order1, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2023, 12, 31));

        dataManager.save(invoice1, invoice2, invoice3);

        // Create payments
        Payment payment1 = entities.payment(invoice1, LocalDate.of(2024, 1, 16), BigDecimal.valueOf(1000.00));
        Payment payment2 = entities.payment(invoice2, LocalDate.of(2024, 1, 25), BigDecimal.valueOf(250.00)); // Partial payment
        dataManager.save(payment1, payment2);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> overview = result.get(0);

        // Should count only invoices in date range (2 invoices)
        assertThat(overview.get("totalInvoiceCount")).isEqualTo(2L);

        // Financial data should include all client invoices/payments (not filtered by date)
        assertThat(overview).containsKeys(
            "totalInvoiceCount", "totalInvoiced", "totalPaid", "outstanding", "paymentRate"
        );

        // Verify that all values are properly formatted strings
        assertThat(overview.get("totalInvoiced")).isInstanceOf(String.class);
        assertThat(overview.get("totalPaid")).isInstanceOf(String.class);
        assertThat(overview.get("outstanding")).isInstanceOf(String.class);
        assertThat(overview.get("paymentRate")).isInstanceOf(String.class);
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
        assertThat(result).hasSize(1);
        Map<String, Object> overview = result.get(0);

        assertThat(overview.get("totalInvoiceCount")).isEqualTo(0L);
        assertThat(overview).containsKeys("totalInvoiced", "totalPaid", "outstanding", "paymentRate");
    }

    @Test
    void testLoadDataDateFiltering() {
        // Given
        Client client = entities.client("Date Filter Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        dataManager.save(order);

        // Invoice in date range
        Invoice invoiceInRange = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 6, 15));

        // Invoice outside date range
        Invoice invoiceOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2024, 7, 15));

        dataManager.save(invoiceInRange, invoiceOutOfRange);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> overview = result.get(0);

        // Should count only invoice in date range
        assertThat(overview.get("totalInvoiceCount")).isEqualTo(1L);
    }

    @Test
    void testLoadDataAlwaysReturnsOneRow() {
        // Given
        Client client = entities.client("Single Row Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNotEmpty();
    }

    private Map<String, Object> createParams(Client client, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("client", client);
        params.put("fromDate", java.sql.Date.valueOf(fromDate));
        params.put("toDate", java.sql.Date.valueOf(toDate));
        return params;
    }
}