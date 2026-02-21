package com.company.crm.test.client;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.client.Client360ReportService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class Client360ReportServiceCustomerTest extends AbstractServiceTest<Client360ReportService> {

    private final LocalDateRange testDateRange = new LocalDateRange(
            LocalDate.now().minusMonths(6),
            LocalDate.now()
    );

    // High-Value Customer Tests

    @Test
    void isHighValueCustomer_returnsTrueWhenTotalInvoicesExceedThreshold() {
        Client client = entities.client("High Value Corp");
        createInvoiceWithTotal(client, new BigDecimal("55000"));

        boolean result = service.isHighValueCustomer(client, null);

        assertThat(result).isTrue();
    }

    @Test
    void isHighValueCustomer_returnsFalseWhenTotalInvoicesBelowThreshold() {
        Client client = entities.client("Small Corp");
        createInvoiceWithTotal(client, new BigDecimal("25000"));

        boolean result = service.isHighValueCustomer(client, null);

        assertThat(result).isFalse();
    }

    @Test
    void isHighValueCustomer_returnsFalseWhenExactlyAtThreshold() {
        Client client = entities.client("Threshold Corp");
        createInvoiceWithTotal(client, new BigDecimal("50000"));

        boolean result = service.isHighValueCustomer(client, null);

        assertThat(result).isFalse();
    }

    // New Customer Tests

    @Test
    void isNewCustomer_returnsTrueForRecentlyCreatedClient() {
        Client client = entities.client("New Customer");
        // Client created today (default in entities factory)

        boolean result = service.isNewCustomer(client);

        assertThat(result).isTrue();
    }

    @Test
    void isNewCustomer_returnsFalseForOldClient() {
        Client client = entities.client("Old Customer", 61);

        boolean result = service.isNewCustomer(client);

        assertThat(result).isFalse();
    }

    @Test
    void isNewCustomer_returnsFalseAtThresholdBoundary() {
        Client client = entities.client("Boundary Customer", 31);

        boolean result = service.isNewCustomer(client);

        assertThat(result).isFalse();
    }

    // Inactive Customer Tests

    @Test
    void isInactiveCustomer_returnsTrueWithNoActivityAtAll() {
        Client client = entities.client("No Activity Customer");
        // No orders, invoices, or payments created

        boolean result = service.isInactiveCustomer(client, testDateRange);

        assertThat(result).isTrue();
    }

    @Test
    void isInactiveCustomer_returnsFalseWithRecentActivity() {
        Client client = entities.client("Active Customer");
        // Activity 30 days ago
        entities.order(client, LocalDate.now().minusDays(30), OrderStatus.DONE, new BigDecimal("1000"));

        boolean result = service.isInactiveCustomer(client, testDateRange);

        assertThat(result).isFalse();
    }

    @Test
    void isInactiveCustomer_returnsTrueWhenNoRecentActivity() {
        Client client = entities.client("Inactive Customer");
        // Last activity 120 days ago
        entities.order(client, LocalDate.now().minusDays(120), OrderStatus.DONE, new BigDecimal("1000"));

        boolean result = service.isInactiveCustomer(client, testDateRange);

        assertThat(result).isTrue();
    }

    // Frequent Customer Tests

    @Test
    void isFrequentCustomer_returnsTrueForHighOrderFrequency() {
        Client client = entities.client("Frequent Customer");
        createFrequentOrders(client, 15); // 15 orders in 6 months = 30/year

        boolean result = service.isFrequentCustomer(client, testDateRange);

        assertThat(result).isTrue();
    }

    @Test
    void isFrequentCustomer_returnsFalseForLowOrderFrequency() {
        Client client = entities.client("Infrequent Customer");
        createFrequentOrders(client, 3); // 3 orders in 6 months = 6/year

        boolean result = service.isFrequentCustomer(client, testDateRange);

        assertThat(result).isFalse();
    }

    @Test
    void isFrequentCustomer_returnsFalseWithNullDateRange() {
        Client client = entities.client("Any Customer");

        boolean result = service.isFrequentCustomer(client, null);

        assertThat(result).isFalse();
    }

    // VIP Customer Tests

    @Test
    void isVIPCustomer_requiresHighValueAndFrequentAndGoodPaymentHistory() {
        Client client1 = entities.client("VIP Customer Inc");

        // High value: Create invoices totaling >€50k
        createInvoiceWithTotal(client1, new BigDecimal("60000"));

        // Frequent: Create 15+ orders over date range
        createFrequentOrders(client1, 15);

        // Excellent payment: 98% payment rate with fast payment
        Order order = entities.order(client1, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client1, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("9800"));

        Client client = client1;

        boolean result = service.isVIPCustomer(client, testDateRange);

        assertThat(result).isTrue();
    }

    @Test
    void isVIPCustomer_returnsFalseWhenNotHighValue() {
        Client client = entities.client("Low Value VIP Candidate");
        createInvoiceWithTotal(client, new BigDecimal("30000")); // Below high value threshold
        createFrequentOrders(client, 15);
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("9800"));

        boolean result = service.isVIPCustomer(client, testDateRange);

        assertThat(result).isFalse();
    }

    // Customer Tenure Tests

    @Test
    void getCustomerTenure_returnsCorrectTenureForNewCustomer() {
        Client client = entities.client("New Customer");

        String tenure = service.getCustomerTenure(client);

        assertThat(tenure).isEqualTo("New Customer");
    }

    @Test
    void getCustomerTenure_returnsCorrectTenureForMonthsOldCustomer() {
        Client client = entities.client("Month Customer", 95); // ~3 months

        String tenure = service.getCustomerTenure(client);

        assertThat(tenure).isEqualTo("3 months");
    }

    @Test
    void getCustomerTenure_returnsCorrectTenureForYearsOldCustomer() {
        Client client = entities.client("Year Customer", 750); // ~2 years

        String tenure = service.getCustomerTenure(client);

        assertThat(tenure).isEqualTo("2 years");
    }

    // Helper methods for test data creation
    private void createInvoiceWithTotal(Client client, BigDecimal total) {
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE, total);
        Invoice invoice = entities.invoice(client, order, total, InvoiceStatus.PAID, LocalDate.now());
        entities.payment(invoice, LocalDate.now().plusDays(15), total);
    }

    private void createFrequentOrders(Client client, int orderCount) {
        LocalDate startDate = testDateRange.startDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, testDateRange.endDate());
        long daysBetweenOrders = Math.max(1, daysBetween / orderCount);

        for (int i = 0; i < orderCount; i++) {
            LocalDate orderDate = startDate.plusDays(i * daysBetweenOrders);
            entities.order(client, orderDate, OrderStatus.DONE, new BigDecimal("1000"));
        }
    }

}