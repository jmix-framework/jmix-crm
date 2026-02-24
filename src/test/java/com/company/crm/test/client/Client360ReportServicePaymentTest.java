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
        // given
        BigDecimal totalInvoiced = new BigDecimal("10000");
        BigDecimal totalPaid = new BigDecimal("9500");

        // when
        double paymentRate = service.calculatePaymentRate(totalInvoiced, totalPaid);

        // then
        assertThat(paymentRate).isEqualTo(95.0);
    }

    @Test
    void calculatePaymentRate_returnsZeroForZeroInvoiced() {
        // given
        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = new BigDecimal("1000");

        // when
        double paymentRate = service.calculatePaymentRate(totalInvoiced, totalPaid);

        // then
        assertThat(paymentRate).isEqualTo(0.0);
    }

    @Test
    void calculatePaymentRate_handlesNullValues() {
        // given
        BigDecimal totalInvoiced = null;
        BigDecimal totalPaid = null;

        // when
        double paymentRate = service.calculatePaymentRate(totalInvoiced, totalPaid);

        // then
        assertThat(paymentRate).isEqualTo(0.0);
    }

    @Test
    void hasPaymentIssues_returnsTrueWithOverdueInvoices() {
        // given
        Client client = entities.client("Overdue Customer");
        Order overdueOrder = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        entities.invoice(client, overdueOrder, new BigDecimal("2000"), InvoiceStatus.OVERDUE, testDateRange.startDate());
        long overdueCount = dataManager.loadValue(
                "select count(i) from Invoice i where i.client = :client and i.status = :status", Long.class)
                .parameter("client", client)
                .parameter("status", InvoiceStatus.OVERDUE)
                .one();
        double avgPaymentDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // when
        boolean result = service.hasPaymentIssues(client, testDateRange);

        // then
        assertThat(overdueCount).isEqualTo(1L);
        assertThat(avgPaymentDuration).isEqualTo(0.0);
        assertThat(result).isTrue();
    }

    @Test
    void hasPaymentIssues_returnsFalseWithGoodPaymentBehavior() {
        // given
        Client client = entities.client("Good Payment Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("5000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(25), new BigDecimal("5000"));
        double avgPaymentDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // when
        boolean result = service.hasPaymentIssues(client, testDateRange);

        // then
        assertThat(avgPaymentDuration).isEqualTo(25.0);
        assertThat(result).isFalse();
    }

    @Test
    void hasGoodPaymentHistory_returnsTrueWithExcellentPayments() {
        // given
        Client client = entities.client("Excellent Payment Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("9800"));
        double paymentRate = service.calculatePaymentRate(new BigDecimal("10000"), new BigDecimal("9800"));
        double avgPaymentDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // when
        boolean result = service.hasGoodPaymentHistory(client, testDateRange);

        // then
        assertThat(paymentRate).isEqualTo(98.0);
        assertThat(avgPaymentDuration).isEqualTo(20.0);
        assertThat(result).isTrue();
    }

    @Test
    void hasGoodPaymentHistory_returnsFalseWithPoorPaymentRate() {
        // given
        Client client = entities.client("Poor Rate Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("10000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(20), new BigDecimal("8500"));
        double paymentRate = service.calculatePaymentRate(new BigDecimal("10000"), new BigDecimal("8500"));
        double avgPaymentDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // when
        boolean result = service.hasGoodPaymentHistory(client, testDateRange);

        // then
        assertThat(paymentRate).isEqualTo(85.0);
        assertThat(avgPaymentDuration).isEqualTo(20.0);
        assertThat(result).isFalse();
    }

}
