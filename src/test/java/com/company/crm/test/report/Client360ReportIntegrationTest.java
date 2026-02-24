package com.company.crm.test.report;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import io.jmix.reports.runner.ReportRunner;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class Client360ReportIntegrationTest extends AbstractTest {

    @Autowired
    private ReportRunner reportRunner;

    private static final LocalDate BASE_DATE = LocalDate.of(2025, 2, 1);

    @Test
    void testBasicReportGeneration() {
        // given
        Client client = entities.client("Test Client for Report");
        LocalDate fromDate = BASE_DATE.minusDays(30);
        LocalDate toDate = BASE_DATE;
        Order order = entities.order(client, BASE_DATE.minusDays(10), OrderStatus.DONE);
        order.setTotal(new BigDecimal("1250.00"));
        order = dataManager.save(order);

        Invoice invoice = entities.invoice(client, order, new BigDecimal("1000.00"), InvoiceStatus.PENDING, BASE_DATE.minusDays(9));
        invoice.setDueDate(BASE_DATE.minusDays(2));
        saveWithoutReload(invoice);
        entities.payment(invoice, BASE_DATE.minusDays(1), new BigDecimal("400.00"));

        // when
        String htmlContent = generateReport(client, fromDate, toDate);

        // then
        assertThat(htmlContent).contains("Client 360° Report");
        assertThat(htmlContent).contains(client.getName());
        assertThat(htmlContent).contains("Period:");
        assertThat(htmlContent).contains("Orders (");
        assertThat(htmlContent).contains("$");
        assertThat(htmlContent).contains("Invoice Total");
        assertThat(htmlContent).contains("Amount Paid");
        assertThat(htmlContent).contains("Outstanding");
        assertThat(htmlContent).contains("Orders (1)");
        assertThat(htmlContent).contains("$1,250.00");
        assertThat(htmlContent).contains("$1,000.00");
        assertThat(htmlContent).contains("$400.00");
        assertThat(htmlContent).contains("$600.00");
        assertThat(htmlContent).contains("Risk Level:");
        assertThat(htmlContent).contains("Overdue Invoices");
        assertThat(htmlContent).contains(">1<");
        assertThat(htmlContent).contains("Payment Rate:");
        assertThat(htmlContent).contains("40");
        assertThat(htmlContent).contains("Recent Payments");
        assertThat(htmlContent).contains("Status Distribution");
    }

    private String generateReport(Client client, LocalDate fromDate, LocalDate toDate) {
        Date fromDateAsDate = fromDate != null ?
            Date.from(fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
        Date toDateAsDate = toDate != null ?
            Date.from(toDate.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

        ReportOutputDocument document = reportRunner
            .byReportCode("client-360-report")
            .addParam("client", client)
            .addParam("fromDate", fromDateAsDate)
            .addParam("toDate", toDateAsDate)
            .run();

        return new String(document.getContent(), StandardCharsets.UTF_8);
    }

}
