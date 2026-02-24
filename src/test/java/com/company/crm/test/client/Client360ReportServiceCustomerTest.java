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
        // given
        Client client = entities.client("High Value Corp");
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE, new BigDecimal("55000"));
        Invoice invoice = entities.invoice(client, order, new BigDecimal("55000"), InvoiceStatus.PAID, LocalDate.now());
        entities.payment(invoice, LocalDate.now().plusDays(15), new BigDecimal("55000"));
        BigDecimal totalInvoiced = sumInvoices(client);

        // when
        boolean result = service.isHighValueCustomer(client, null);

        // then
        assertThat(totalInvoiced).isEqualByComparingTo("55000");
        assertThat(result).isTrue();
    }

    @Test
    void isHighValueCustomer_returnsFalseWhenTotalInvoicesBelowThreshold() {
        // given
        Client client = entities.client("Small Corp");
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE, new BigDecimal("25000"));
        Invoice invoice = entities.invoice(client, order, new BigDecimal("25000"), InvoiceStatus.PAID, LocalDate.now());
        entities.payment(invoice, LocalDate.now().plusDays(15), new BigDecimal("25000"));
        BigDecimal totalInvoiced = sumInvoices(client);

        // when
        boolean result = service.isHighValueCustomer(client, null);

        // then
        assertThat(totalInvoiced).isEqualByComparingTo("25000");
        assertThat(result).isFalse();
    }

    @Test
    void isHighValueCustomer_returnsFalseWhenExactlyAtThreshold() {
        // given
        Client client = entities.client("Threshold Corp");
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE, new BigDecimal("50000"));
        Invoice invoice = entities.invoice(client, order, new BigDecimal("50000"), InvoiceStatus.PAID, LocalDate.now());
        entities.payment(invoice, LocalDate.now().plusDays(15), new BigDecimal("50000"));
        BigDecimal totalInvoiced = sumInvoices(client);

        // when
        boolean result = service.isHighValueCustomer(client, null);

        // then
        // Threshold contract: strictly greater than 50_000 is required.
        assertThat(totalInvoiced).isEqualByComparingTo("50000");
        assertThat(result).isFalse();
    }

    // New Customer Tests

    @Test
    void isNewCustomer_returnsTrueForRecentlyCreatedClient() {
        // given
        Client client = entities.client("New Customer");

        // when
        boolean result = service.isNewCustomer(client);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isNewCustomer_returnsFalseForOldClient() {
        // given
        Client client = entities.client("Old Customer", 61);

        // when
        boolean result = service.isNewCustomer(client);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isNewCustomer_returnsFalseAtThresholdBoundary() {
        // given
        Client client = entities.client("Boundary Customer", 31);

        // when
        boolean result = service.isNewCustomer(client);

        // then
        assertThat(result).isFalse();
    }

    // Inactive Customer Tests

    @Test
    void isInactiveCustomer_returnsTrueWithNoActivityAtAll() {
        // given
        Client client = entities.client("No Activity Customer");

        // when
        boolean result = service.isInactiveCustomer(client, testDateRange);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isInactiveCustomer_returnsFalseWithRecentActivity() {
        // given
        Client client = entities.client("Active Customer");
        entities.order(client, LocalDate.now().minusDays(30), OrderStatus.DONE, new BigDecimal("1000"));

        // when
        boolean result = service.isInactiveCustomer(client, testDateRange);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void isInactiveCustomer_returnsTrueWhenNoRecentActivity() {
        // given
        Client client = entities.client("Inactive Customer");
        entities.order(client, LocalDate.now().minusDays(120), OrderStatus.DONE, new BigDecimal("1000"));

        // when
        boolean result = service.isInactiveCustomer(client, testDateRange);

        // then
        assertThat(result).isTrue();
    }

    // Frequent Customer Tests

    @Test
    void isFrequentCustomer_returnsTrueForHighOrderFrequency() {
        // given
        Client client = entities.client("Frequent Customer");
        LocalDate startDate = testDateRange.startDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, testDateRange.endDate());
        long daysBetweenOrders = Math.max(1, daysBetween / 15);
        for (int i = 0; i < 15; i++) {
            LocalDate orderDate = startDate.plusDays(i * daysBetweenOrders);
            entities.order(client, orderDate, OrderStatus.DONE, new BigDecimal("1000"));
        }
        long completedOrders = countCompletedOrders(client, testDateRange);
        double ordersPerYear = calculateOrdersPerYear(completedOrders, testDateRange);

        // when
        boolean result = service.isFrequentCustomer(client, testDateRange);

        // then
        assertThat(completedOrders).isEqualTo(15L);
        assertThat(ordersPerYear).isGreaterThanOrEqualTo(12.0);
        assertThat(result).isTrue();
    }

    @Test
    void isFrequentCustomer_returnsFalseForLowOrderFrequency() {
        // given
        Client client = entities.client("Infrequent Customer");
        LocalDate startDate = testDateRange.startDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, testDateRange.endDate());
        long daysBetweenOrders = Math.max(1, daysBetween / 3);
        for (int i = 0; i < 3; i++) {
            LocalDate orderDate = startDate.plusDays(i * daysBetweenOrders);
            entities.order(client, orderDate, OrderStatus.DONE, new BigDecimal("1000"));
        }
        long completedOrders = countCompletedOrders(client, testDateRange);
        double ordersPerYear = calculateOrdersPerYear(completedOrders, testDateRange);

        // when
        boolean result = service.isFrequentCustomer(client, testDateRange);

        // then
        assertThat(completedOrders).isEqualTo(3L);
        assertThat(ordersPerYear).isLessThan(12.0);
        assertThat(result).isFalse();
    }

    @Test
    void isFrequentCustomer_returnsFalseWithNullDateRange() {
        // given
        Client client = entities.client("Any Customer");

        // when
        boolean result = service.isFrequentCustomer(client, null);

        // then
        assertThat(result).isFalse();
    }

    // VIP Customer Tests

    @Test
    void isVIPCustomer_requiresHighValueAndFrequentAndGoodPaymentHistory() {
        // given
        Client client = entities.client("VIP Customer Inc");

        Order highValueOrder = entities.order(client, LocalDate.now(), OrderStatus.DONE, new BigDecimal("60000"));
        Invoice highValueInvoice = entities.invoice(client, highValueOrder, new BigDecimal("60000"), InvoiceStatus.PAID, LocalDate.now());
        entities.payment(highValueInvoice, LocalDate.now().plusDays(15), new BigDecimal("60000"));

        LocalDate startDate = testDateRange.startDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, testDateRange.endDate());
        long daysBetweenOrders = Math.max(1, daysBetween / 15);
        for (int i = 0; i < 15; i++) {
            LocalDate orderDate = startDate.plusDays(i * daysBetweenOrders);
            entities.order(client, orderDate, OrderStatus.DONE, new BigDecimal("1000"));
        }

        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("9800"));
        BigDecimal totalInvoiced = sumInvoices(client);
        long completedOrders = countCompletedOrders(client, testDateRange);
        double ordersPerYear = calculateOrdersPerYear(completedOrders, testDateRange);

        // when
        boolean result = service.isVIPCustomer(client, testDateRange);

        // then
        assertThat(totalInvoiced).isGreaterThan(new BigDecimal("50000"));
        assertThat(ordersPerYear).isGreaterThanOrEqualTo(12.0);
        assertThat(result).isTrue();
    }

    @Test
    void isVIPCustomer_returnsFalseWhenNotHighValue() {
        // given
        Client client = entities.client("Low Value VIP Candidate");
        Order lowValueOrder = entities.order(client, LocalDate.now(), OrderStatus.DONE, new BigDecimal("30000"));
        Invoice lowValueInvoice = entities.invoice(client, lowValueOrder, new BigDecimal("30000"), InvoiceStatus.PAID, LocalDate.now());
        entities.payment(lowValueInvoice, LocalDate.now().plusDays(15), new BigDecimal("30000"));
        LocalDate startDate = testDateRange.startDate();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, testDateRange.endDate());
        long daysBetweenOrders = Math.max(1, daysBetween / 15);
        for (int i = 0; i < 15; i++) {
            LocalDate orderDate = startDate.plusDays(i * daysBetweenOrders);
            entities.order(client, orderDate, OrderStatus.DONE, new BigDecimal("1000"));
        }
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("9800"));
        BigDecimal totalInvoiced = sumInvoices(client);

        // when
        boolean result = service.isVIPCustomer(client, testDateRange);

        // then
        assertThat(totalInvoiced).isLessThanOrEqualTo(new BigDecimal("50000"));
        assertThat(result).isFalse();
    }

    // Customer Tenure Tests

    @Test
    void getCustomerTenure_returnsCorrectTenureForNewCustomer() {
        // given
        Client client = entities.client("New Customer");

        // when
        String tenure = service.getCustomerTenure(client);

        // then
        assertThat(tenure).isEqualTo("New Customer");
    }

    @Test
    void getCustomerTenure_returnsCorrectTenureForMonthsOldCustomer() {
        // given
        Client client = entities.client("Month Customer", 95);

        // when
        String tenure = service.getCustomerTenure(client);

        // then
        assertThat(tenure).isEqualTo("3 months");
    }

    @Test
    void getCustomerTenure_returnsCorrectTenureForYearsOldCustomer() {
        // given
        Client client = entities.client("Year Customer", 750);

        // when
        String tenure = service.getCustomerTenure(client);

        // then
        assertThat(tenure).isEqualTo("2 years");
    }

    private BigDecimal sumInvoices(Client client) {
        return dataManager.loadValue("select coalesce(sum(i.total), 0) from Invoice i where i.client = :client", BigDecimal.class)
                .parameter("client", client)
                .one();
    }

    private long countCompletedOrders(Client client, LocalDateRange range) {
        return dataManager.loadValue(
                        "select count(o) from Order_ o where o.client = :client and o.status = :status and o.date between :fromDate and :toDate",
                        Long.class)
                .parameter("client", client)
                .parameter("status", OrderStatus.DONE)
                .parameter("fromDate", range.startDate())
                .parameter("toDate", range.endDate())
                .one();
    }

    private double calculateOrdersPerYear(long orderCount, LocalDateRange range) {
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(range.startDate(), range.endDate());
        double yearsFactor = daysBetween / 365.0;
        return orderCount / Math.max(yearsFactor, 0.1);
    }

}
