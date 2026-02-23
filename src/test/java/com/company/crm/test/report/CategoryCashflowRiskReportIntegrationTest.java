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
import io.jmix.reports.entity.ReportOutputType;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryCashflowRiskReportIntegrationTest extends AbstractTest {

    @Autowired
    private ReportRunner reportRunner;

    @Test
    void testReportCsvContainsRiskByCategoryData() {
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

            String csv = generateCsv(client, LocalDate.now().minusDays(60), LocalDate.now(), LocalDate.now(), true);

            assertThat(csv).contains("Category");
            assertThat(csv).contains("Code");
            assertThat(csv).contains("Invoiced");
            assertThat(csv).contains("Report Cat");
            assertThat(csv).contains("REP");
        });
    }

    @Test
    void testReportDefaultTemplateProducesCsv() {
        systemAuthenticator.runWithSystem(() -> {
            Category category = entities.category("Csv Default Cat", "CSVDEF");
            CategoryItem item = entities.categoryItem("Csv Default Item", "CDI", category, BigDecimal.valueOf(500), UomType.PIECES);
            Client client = entities.client("Csv Default Client");

            Order order = entities.order(client, LocalDate.now().minusDays(20), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(java.util.List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(500));
            order = dataManager.save(order);

            entities.invoice(client, order, BigDecimal.valueOf(500), InvoiceStatus.PENDING, LocalDate.now().minusDays(15));

            ReportOutputDocument document = reportRunner
                    .byReportCode("category-cashflow-risk-report")
                    .withParams(reportParams(client, LocalDate.now().minusDays(60), LocalDate.now(), LocalDate.now(), true))
                    .run();

            String csv = new String(document.getContent(), StandardCharsets.UTF_8);
            assertThat(csv).contains("Category");
            assertThat(csv).contains("Csv Default Cat");
        });
    }

    @Test
    void testReportXlsxTemplateProducesXlsx() {
        systemAuthenticator.runWithSystem(() -> {
            Category category = entities.category("Xlsx Cat", "XLSX");
            CategoryItem item = entities.categoryItem("Xlsx Item", "XI", category, BigDecimal.valueOf(500), UomType.PIECES);
            Client client = entities.client("Xlsx Client");

            Order order = entities.order(client, LocalDate.now().minusDays(20), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(java.util.List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(500));
            order = dataManager.save(order);

            entities.invoice(client, order, BigDecimal.valueOf(500), InvoiceStatus.PENDING, LocalDate.now().minusDays(15));

            ReportOutputDocument document = reportRunner
                    .byReportCode("category-cashflow-risk-report")
                    .withTemplateCode("XLSX")
                    .withParams(reportParams(client, LocalDate.now().minusDays(60), LocalDate.now(), LocalDate.now(), true))
                    .run();

            byte[] content = document.getContent();
            assertThat(content).isNotNull();
            assertThat(content.length).isGreaterThan(4);
            assertThat(content[0]).isEqualTo((byte) 'P');
            assertThat(content[1]).isEqualTo((byte) 'K');
        });
    }

    private String generateCsv(Client client,
                               LocalDate fromDate,
                               LocalDate toDate,
                               LocalDate asOfDate,
                               boolean includePaid) {
        ReportOutputDocument document = reportRunner
                .byReportCode("category-cashflow-risk-report")
                .withParams(reportParams(client, fromDate, toDate, asOfDate, includePaid))
                .withOutputType(ReportOutputType.CSV)
                .run();
        return new String(document.getContent(), StandardCharsets.UTF_8);
    }

    private Map<String, Object> reportParams(Client client,
                                             LocalDate fromDate,
                                             LocalDate toDate,
                                             LocalDate asOfDate,
                                             boolean includePaid) {
        return Map.of(
                "client", client,
                "fromDate", toDate(fromDate),
                "toDate", toDate(toDate),
                "asOfDate", toDate(asOfDate),
                "includePaid", includePaid
        );
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
