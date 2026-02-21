package com.company.crm.test.report;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.util.extenstion.DataCleaner;
import io.jmix.reports.runner.ReportRunner;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Date;

import io.jmix.core.metamodel.datatype.DatatypeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class Client360ReportIntegrationTest extends AbstractTest {

    @Autowired
    private ReportRunner reportRunner;

    private static final LocalDate BASE_DATE = LocalDate.now();

    @Test
    void testBasicReportGeneration() {
        // Create a simple client for testing
        Client client = entities.client("Test Client for Report");

        // Generate report with basic data
        String htmlContent = generateReport(client, BASE_DATE.minusDays(30), BASE_DATE);

        // Core structure assertions
        assertThat(htmlContent).isNotNull();
        assertThat(htmlContent).isNotEmpty();
        assertThat(htmlContent).contains("Client 360° Report");

        // Client information assertions
        assertThat(htmlContent).contains(client.getName());
        assertThat(htmlContent).contains("Report Period:");

        // Essential sections must be present (using correct HTML labels)
        assertThat(htmlContent).contains("Orders (");
        assertThat(htmlContent).contains("€"); // Currency formatting
        assertThat(htmlContent).contains("Invoice Total");    // Korrekte Bezeichnung aus HTML
        assertThat(htmlContent).contains("Amount Paid");      // Korrekte Bezeichnung aus HTML
        assertThat(htmlContent).contains("Outstanding");

        // For a new client with no orders, expect zero values
        assertThat(htmlContent).contains("Orders (0)");
        assertThat(htmlContent).contains("No orders found");

        // Check for zero currency formatting (€ 0,00 is default in template)
        assertThat(htmlContent).contains("€ 0,00");

        // Risk indicators section should be present
        assertThat(htmlContent).contains("Risk Level:");
        assertThat(htmlContent).contains("Overdue Invoices");

        // Payment sections
        assertThat(htmlContent).contains("Payment Rate:");
        assertThat(htmlContent).contains("Recent Payments");

        // Status distribution section
        assertThat(htmlContent).contains("Status Distribution");
    }

    // Helper Methods for Report Generation and Assertions

    private String generateReport(Client client, LocalDate fromDate, LocalDate toDate) {
        // Convert LocalDate to Date for the report runner
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