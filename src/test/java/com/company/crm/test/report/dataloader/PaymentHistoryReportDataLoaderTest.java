package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.report.dataloader.PaymentHistoryReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentHistoryReportDataLoaderTest extends AbstractTest {

    @Autowired
    private PaymentHistoryReportDataLoader dataLoader;

    @Test
    void testLoadDataWithMultiplePayments() {
        // Given
        Client client = entities.client("Payment Client");
        dataManager.save(client);

        // Create orders first (needed for invoices)
        Order order1 = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        Order order2 = entities.order(client, LocalDate.of(2024, 1, 20), OrderStatus.DONE);
        dataManager.save(order1, order2);

        Invoice invoice1 = entities.invoice(client, order1, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 10));
        invoice1.setNumber("INV-001");
        Invoice invoice2 = entities.invoice(client, order2, BigDecimal.valueOf(750.50), InvoiceStatus.PAID, LocalDate.of(2024, 1, 20));
        invoice2.setNumber("INV-002");
        dataManager.save(invoice1, invoice2);

        Payment payment1 = entities.payment(invoice1, LocalDate.of(2024, 1, 15), BigDecimal.valueOf(1000.00));
        payment1.setNumber("PAY-001");

        Payment payment2 = entities.payment(invoice2, LocalDate.of(2024, 1, 25), BigDecimal.valueOf(750.50));
        payment2.setNumber("PAY-002");

        Payment payment3 = entities.payment(invoice1, LocalDate.of(2024, 1, 30), BigDecimal.valueOf(250.00));
        payment3.setNumber("PAY-003");

        dataManager.save(payment1, payment2, payment3);

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(3);

        // Payments should be ordered by date DESC (newest first)
        assertThat(result.get(0).get("number")).isEqualTo("PAY-003");
        assertThat(result.get(1).get("number")).isEqualTo("PAY-002");
        assertThat(result.get(2).get("number")).isEqualTo("PAY-001");

        // Check all fields are present
        assertThat(result.get(0)).containsKeys("number", "date", "dateFormatted", "amount", "invoiceNumber");
        assertThat(result.get(0).get("amount")).isInstanceOf(String.class);
        assertThat(((String) result.get(0).get("amount"))).contains("$");
        assertThat(result.get(0).get("invoiceNumber")).isEqualTo("INV-001");
    }

    @Test
    void testLoadDataMaxResults() {
        // Given - Create more than 10 payments
        Client client = entities.client("Max Results Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1500.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 10));
        invoice.setNumber("INV-MAX");
        dataManager.save(invoice);

        // Create 15 payments
        for (int i = 1; i <= 15; i++) {
            Payment payment = entities.payment(invoice, LocalDate.of(2024, 1, i), BigDecimal.valueOf(100.00));
            payment.setNumber("PAY-" + String.format("%03d", i));
            dataManager.save(payment);
        }

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then - Should be limited to 10 results
        assertThat(result).hasSize(10);

        // Should get the latest 10 payments (15th to 6th)
        assertThat(result.get(0).get("number")).isEqualTo("PAY-015");
        assertThat(result.get(9).get("number")).isEqualTo("PAY-006");
        assertThat(result.stream().map(row -> row.get("number")).distinct()).hasSize(10);
    }

    @Test
    void testLoadDataDateFiltering() {
        // Given
        Client client = entities.client("Date Filter Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 10));
        dataManager.save(invoice);

        Payment paymentInRange = entities.payment(invoice, LocalDate.of(2024, 6, 15), BigDecimal.valueOf(500.00));
        paymentInRange.setNumber("IN-RANGE");

        Payment paymentOutOfRange = entities.payment(invoice, LocalDate.of(2024, 7, 15), BigDecimal.valueOf(300.00));
        paymentOutOfRange.setNumber("OUT-OF-RANGE");

        dataManager.save(paymentInRange, paymentOutOfRange);

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", java.sql.Date.valueOf(LocalDate.of(2024, 6, 1)),
                "toDate", java.sql.Date.valueOf(LocalDate.of(2024, 6, 30))
        );

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("number")).isEqualTo("IN-RANGE");
    }

    @Test
    void testLoadDataWithNoPayments() {
        // Given
        Client client = entities.client("No Payments Client");
        dataManager.save(client);

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", java.sql.Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).isEmpty();
    }

}
