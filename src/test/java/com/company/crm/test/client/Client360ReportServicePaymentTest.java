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

class Client360ReportServicePaymentTest extends AbstractServiceTest<Client360ReportService> {

    private final LocalDateRange testDateRange = new LocalDateRange(
            LocalDate.now().minusMonths(6),
            LocalDate.now()
    );

    @Test
    void calculatePaymentRate_calculatesCorrectly() {
        double paymentRate = service.calculatePaymentRate(
                new BigDecimal("10000"),
                new BigDecimal("9500")
        );

        assertThat(paymentRate).isEqualTo(95.0);
    }

    @Test
    void calculatePaymentRate_returnsZeroForZeroInvoiced() {
        double paymentRate = service.calculatePaymentRate(
                BigDecimal.ZERO,
                new BigDecimal("1000")
        );

        assertThat(paymentRate).isEqualTo(0.0);
    }

    @Test
    void calculatePaymentRate_handlesNullValues() {
        double paymentRate = service.calculatePaymentRate(null, null);

        assertThat(paymentRate).isEqualTo(0.0);
    }

    @Test
    void hasPaymentIssues_returnsTrueWithOverdueInvoices() {
        Client client = entities.client("Overdue Customer");
        createOverdueInvoices(client, 1);

        boolean result = service.hasPaymentIssues(client, testDateRange);

        assertThat(result).isTrue();
    }

    @Test
    void hasPaymentIssues_returnsFalseWithGoodPaymentBehavior() {
        Client client = entities.client("Good Payment Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("5000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(25), new BigDecimal("5000"));

        boolean result = service.hasPaymentIssues(client, testDateRange);

        assertThat(result).isFalse();
    }

    @Test
    void hasGoodPaymentHistory_returnsTrueWithExcellentPayments() {
        Client client = entities.client("Excellent Payment Customer");
        // 98% payment rate
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("9800"));

        boolean result = service.hasGoodPaymentHistory(client, testDateRange);

        assertThat(result).isTrue();
    }

    @Test
    void hasGoodPaymentHistory_returnsFalseWithPoorPaymentRate() {
        Client client = entities.client("Poor Rate Customer");
        // 85% rate
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("8500"));

        boolean result = service.hasGoodPaymentHistory(client, testDateRange);

        assertThat(result).isFalse();
    }

    // Helper methods for test data creation
    private void createOverdueInvoices(Client client, int count) {
        for (int i = 0; i < count; i++) {
            Order order = entities.order(client, testDateRange.startDate().plusDays(i * 30), OrderStatus.DONE);
            entities.invoice(client, order, new BigDecimal("2000"), InvoiceStatus.OVERDUE, testDateRange.startDate().plusDays(i * 30));
        }
    }

}