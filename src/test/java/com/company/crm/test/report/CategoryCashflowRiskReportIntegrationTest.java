package com.company.crm.test.report;

import com.company.crm.AbstractTest;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
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

class CategoryCashflowRiskReportIntegrationTest extends AbstractTest {

    @Autowired
    private ReportRunner reportRunner;

    @Test
    void testReportContainsRiskAndCriticalInvoiceData() {
        systemAuthenticator.runWithSystem(() -> {
            Category category = entities.category("Report Cat", "REP");
            CategoryItem item = entities.categoryItem("Report Item", "RI", category, BigDecimal.valueOf(1000), UomType.PIECES);
            Client client = entities.client("Category Report Client");

            Order order = entities.order(client, LocalDate.now().minusDays(45), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(java.util.List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(1000));
            order = dataManager.save(order);

            Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now().minusDays(40));
            invoice.setDueDate(LocalDate.now().minusDays(30));
            saveWithoutReload(invoice);
            entities.payment(invoice, LocalDate.now().minusDays(10), BigDecimal.valueOf(200));

            String html = generateReport(client, LocalDate.now().minusDays(60), LocalDate.now(), LocalDate.now(), true);

            assertThat(html).contains("Category Cashflow Risk Allocation Report");
            assertThat(html).contains("Critical Invoices");
            assertThat(html).contains("Report Cat");
            assertThat(html).contains(invoice.getNumber());
            assertThat(html).contains("Category Report Client");
        });
    }

    private String generateReport(Client client,
                                  LocalDate fromDate,
                                  LocalDate toDate,
                                  LocalDate asOfDate,
                                  boolean includePaid) {
        ReportOutputDocument document = reportRunner
                .byReportCode("category-cashflow-risk-report")
                .addParam("client", client)
                .addParam("fromDate", toDate(fromDate))
                .addParam("toDate", toDate(toDate))
                .addParam("asOfDate", toDate(asOfDate))
                .addParam("includePaid", includePaid)
                .run();
        return new String(document.getContent(), StandardCharsets.UTF_8);
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
