package com.company.crm.test.client;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.client.Client360ReportService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.RiskLevel;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class Client360ReportServiceRiskTest extends AbstractServiceTest<Client360ReportService> {

    private final LocalDateRange testDateRange = new LocalDateRange(
            LocalDate.now().minusMonths(6),
            LocalDate.now()
    );

    // Risk Level Calculation Tests

    @Test
    void calculateRiskLevel_returnsHighForMultipleOverdue() {
        // given
        Client client = entities.client("High Risk Customer");
        for (int i = 0; i < 4; i++) {
            LocalDate invoiceDate = testDateRange.startDate().plusDays(i * 30L);
            Order order = entities.order(client, invoiceDate, OrderStatus.DONE);
            entities.invoice(client, order, new BigDecimal("2000"), InvoiceStatus.OVERDUE, invoiceDate);
        }
        long overdueCount = countOverdueInvoices(client);
        BigDecimal overdueAmount = sumOverdueAmount(client);
        double avgPaymentDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // when
        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        // then
        assertThat(overdueCount).isEqualTo(4L);
        assertThat(overdueAmount).isEqualByComparingTo("8000");
        assertThat(avgPaymentDuration).isEqualTo(0.0);
        assertThat(riskLevel).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void calculateRiskLevel_returnsMediumForSomeIssues() {
        // given
        Client client = entities.client("Medium Risk Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        entities.invoice(client, order, new BigDecimal("2000"), InvoiceStatus.OVERDUE, testDateRange.startDate());
        long overdueCount = countOverdueInvoices(client);
        BigDecimal overdueAmount = sumOverdueAmount(client);

        // when
        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        // then
        assertThat(overdueCount).isEqualTo(1L);
        assertThat(overdueAmount).isEqualByComparingTo("2000");
        assertThat(riskLevel).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void calculateRiskLevel_returnsLowForGoodCustomer() {
        // given
        Client client = entities.client("Low Risk Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("5000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(25), new BigDecimal("5000"));
        long overdueCount = countOverdueInvoices(client);
        BigDecimal overdueAmount = sumOverdueAmount(client);
        double avgPaymentDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // when
        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        // then
        assertThat(overdueCount).isEqualTo(0L);
        assertThat(overdueAmount).isEqualByComparingTo("0");
        assertThat(avgPaymentDuration).isEqualTo(25.0);
        assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void calculateRiskLevel_returnsHighForLargeOverdueAmount() {
        // given
        Client client = entities.client("Large Overdue Customer");
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        entities.invoice(client, order, new BigDecimal("6000"), InvoiceStatus.OVERDUE, testDateRange.startDate());
        BigDecimal overdueAmount = sumOverdueAmount(client);

        // when
        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        // then
        assertThat(overdueAmount).isEqualByComparingTo("6000");
        assertThat(riskLevel).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void calculateRiskLevel_handlesNullDateRange() {
        // given
        Client client = entities.client("Any Customer");

        // when
        RiskLevel riskLevel = service.calculateRiskLevel(client, null);

        // then
        assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
    }

    // Average Payment Duration Tests

    @Test
    void calculateAveragePaymentDuration_averagesCorrectly() {
        // given
        Client client = entities.client("Duration Test Customer");
        LocalDate invoiceDate = testDateRange.startDate().plusDays(10);
        Order order = entities.order(client, invoiceDate, OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order, new BigDecimal("1000"), InvoiceStatus.PAID, invoiceDate);
        entities.payment(invoice, invoiceDate.plusDays(20), new BigDecimal("1000"));

        // when
        double avgDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // then
        assertThat(avgDuration).isEqualTo(20.0);
    }

    @Test
    void calculateAveragePaymentDuration_returnsZeroWithNoPayments() {
        // given
        Client client = entities.client("No Payments Customer");

        // when
        double avgDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // then
        assertThat(avgDuration).isEqualTo(0.0);
    }

    @Test
    void calculateAveragePaymentDuration_handlesNullDateRange() {
        // given
        Client client = entities.client("Any Customer");

        // when
        double avgDuration = service.calculateAveragePaymentDuration(client, null);

        // then
        assertThat(avgDuration).isEqualTo(0.0);
    }

    // Sales Opportunity Tests

    @Test
    void hasSalesOpportunity_returnsTrueForEngagedCustomerWithoutRecentOrders() {
        // given
        Client client = entities.client("Sales Opportunity Customer");
        LocalDate orderDate = LocalDate.now().minusDays(40);
        Order oldOrder = entities.order(client, orderDate, OrderStatus.DONE, new BigDecimal("5000"));
        Invoice oldInvoice = entities.invoice(client, oldOrder, new BigDecimal("5000"), InvoiceStatus.PAID, orderDate);
        entities.payment(oldInvoice, LocalDate.now().minusDays(15), new BigDecimal("5000"));
        boolean hasPaymentIssues = service.hasPaymentIssues(client, testDateRange);

        // when
        boolean result = service.hasSalesOpportunity(client, testDateRange);

        // then
        assertThat(hasPaymentIssues).isFalse();
        assertThat(result).isTrue();
    }

    @Test
    void hasSalesOpportunity_returnsFalseWithPaymentIssues() {
        // given
        Client client = entities.client("Payment Issues Customer");
        entities.order(client, LocalDate.now().minusDays(15), OrderStatus.DONE, new BigDecimal("1000"));
        Order overdueOrder = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        entities.invoice(client, overdueOrder, new BigDecimal("2000"), InvoiceStatus.OVERDUE, testDateRange.startDate());
        boolean hasPaymentIssues = service.hasPaymentIssues(client, testDateRange);

        // when
        boolean result = service.hasSalesOpportunity(client, testDateRange);

        // then
        assertThat(hasPaymentIssues).isTrue();
        assertThat(result).isFalse();
    }

    @Test
    void hasSalesOpportunity_returnsFalseWithoutRecentActivity() {
        // given
        Client client = entities.client("Inactive Customer");

        // when
        boolean result = service.hasSalesOpportunity(client, testDateRange);

        // then
        assertThat(result).isFalse();
    }

    private long countOverdueInvoices(Client client) {
        return dataManager.loadValue("select count(i) from Invoice i where i.client = :client and i.status = :status", Long.class)
                .parameter("client", client)
                .parameter("status", InvoiceStatus.OVERDUE)
                .one();
    }

    private BigDecimal sumOverdueAmount(Client client) {
        return dataManager.loadValue("select coalesce(sum(i.total), 0) from Invoice i where i.client = :client and i.status = :status", BigDecimal.class)
                .parameter("client", client)
                .parameter("status", InvoiceStatus.OVERDUE)
                .one();
    }

}
