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
        Client client1 = entities.client("High Risk Customer");
        createOverdueInvoices(client1, 4); // > 3 overdue invoices
        Client client = client1;

        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        assertThat(riskLevel).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void calculateRiskLevel_returnsMediumForSomeIssues() {
        Client client1 = entities.client("Medium Risk Customer");
        createOverdueInvoices(client1, 1); // 1 overdue invoice
        Client client = client1;

        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        assertThat(riskLevel).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void calculateRiskLevel_returnsLowForGoodCustomer() {
        Client client1 = entities.client("Low Risk Customer");
        Order order = entities.order(client1, testDateRange.startDate(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client1, order, new BigDecimal("5000"), InvoiceStatus.PAID, testDateRange.startDate());
        entities.payment(invoice, testDateRange.startDate().plusDays(25), new BigDecimal("5000"));
        Client client = client1;

        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void calculateRiskLevel_returnsHighForLargeOverdueAmount() {
        Client client = entities.client("Large Overdue Customer");
        // Above €5k threshold
        Order order = entities.order(client, testDateRange.startDate(), OrderStatus.DONE);
        entities.invoice(client, order, new BigDecimal("6000"), InvoiceStatus.OVERDUE, testDateRange.startDate());

        RiskLevel riskLevel = service.calculateRiskLevel(client, testDateRange);

        assertThat(riskLevel).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void calculateRiskLevel_handlesNullDateRange() {
        Client client = entities.client("Any Customer");

        RiskLevel riskLevel = service.calculateRiskLevel(client, null);

        assertThat(riskLevel).isEqualTo(RiskLevel.LOW);
    }

    // Average Payment Duration Tests

    @Test
    void calculateAveragePaymentDuration_averagesCorrectly() {
        // Given: Client with one paid invoice and payment with 20 days duration
        Client client = entities.client("Duration Test Customer");
        LocalDate invoiceDate = testDateRange.startDate().plusDays(10);
        Order order = entities.order(client, invoiceDate, OrderStatus.DONE);

        Invoice invoice = entities.invoice(client, order, new BigDecimal("1000"), InvoiceStatus.PAID, invoiceDate);
        entities.payment(invoice, invoiceDate.plusDays(20), new BigDecimal("1000"));

        // When: Calculate average payment duration
        double avgDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        // Then: Should be 20 days (difference between invoice and payment date)
        assertThat(avgDuration).isEqualTo(20.0);
    }

    @Test
    void calculateAveragePaymentDuration_returnsZeroWithNoPayments() {
        Client client = entities.client("No Payments Customer");

        double avgDuration = service.calculateAveragePaymentDuration(client, testDateRange);

        assertThat(avgDuration).isEqualTo(0.0);
    }

    @Test
    void calculateAveragePaymentDuration_handlesNullDateRange() {
        Client client = entities.client("Any Customer");

        double avgDuration = service.calculateAveragePaymentDuration(client, null);

        assertThat(avgDuration).isEqualTo(0.0);
    }

    // Sales Opportunity Tests

    @Test
    void hasSalesOpportunity_returnsTrueForEngagedCustomerWithoutRecentOrders() {
        Client client1 = entities.client("Sales Opportunity Customer");

        Order oldOrder = entities.order(client1, LocalDate.now().minusDays(40), OrderStatus.DONE, new BigDecimal("5000"));

        Invoice oldInvoice = entities.invoice(client1, oldOrder, new BigDecimal("5000"), InvoiceStatus.PAID, LocalDate.now().minusDays(40));
        entities.payment(oldInvoice, LocalDate.now().minusDays(15), new BigDecimal("5000"));

        Client client = client1;

        boolean result = service.hasSalesOpportunity(client, testDateRange);

        assertThat(result).isTrue();
    }

    @Test
    void hasSalesOpportunity_returnsFalseWithPaymentIssues() {
        Client client = entities.client("Payment Issues Customer");
        entities.order(client, LocalDate.now().minusDays(15), OrderStatus.DONE, new BigDecimal("1000"));
        createOverdueInvoices(client, 1); // Has payment issues

        boolean result = service.hasSalesOpportunity(client, testDateRange);

        assertThat(result).isFalse();
    }

    @Test
    void hasSalesOpportunity_returnsFalseWithoutRecentActivity() {
        Client client = entities.client("Inactive Customer");
        // No recent activity created

        boolean result = service.hasSalesOpportunity(client, testDateRange);

        assertThat(result).isFalse();
    }


    private void createOverdueInvoices(Client client, int count) {
        for (int i = 0; i < count; i++) {
            Order order = entities.order(client, testDateRange.startDate().plusDays(i * 30), OrderStatus.DONE);
            entities.invoice(client, order, new BigDecimal("2000"), InvoiceStatus.OVERDUE, testDateRange.startDate().plusDays(i * 30));
        }
    }

}