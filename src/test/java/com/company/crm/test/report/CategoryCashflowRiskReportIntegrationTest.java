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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryCashflowRiskReportIntegrationTest extends AbstractTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 1, 31);

    @Autowired
    private ReportRunner reportRunner;

    @Test
    void testReportCsvContainsRiskByCategoryData() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            LocalDate fromDate = REFERENCE_DATE.minusDays(60);
            LocalDate toDate = REFERENCE_DATE;
            LocalDate asOfDate = REFERENCE_DATE;

            Category category = entities.category("Report Cat", "REP");
            CategoryItem item = entities.categoryItem("Report Item", "RI", category, BigDecimal.valueOf(1000), UomType.PIECES);
            Client client = entities.client("Category Report Client");

            Order order = entities.order(client, REFERENCE_DATE.minusDays(45), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(java.util.List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(1000));
            order = dataManager.save(order);

            Invoice invoice = entities.invoice(client, order, BigDecimal.valueOf(1000), InvoiceStatus.PENDING, REFERENCE_DATE.minusDays(40));
            invoice.setDueDate(REFERENCE_DATE.minusDays(30));
            saveWithoutReload(invoice);
            entities.payment(invoice, REFERENCE_DATE.minusDays(10), BigDecimal.valueOf(200));

            Map<String, Object> params = Map.of(
                    "client", client,
                    "fromDate", toDate(fromDate),
                    "toDate", toDate(toDate),
                    "asOfDate", toDate(asOfDate),
                    "includePaid", true
            );

            // when
            ReportOutputDocument document = reportRunner
                    .byReportCode("category-cashflow-risk-report")
                    .withParams(params)
                    .withOutputType(ReportOutputType.CSV)
                    .run();
            String csv = new String(document.getContent(), StandardCharsets.UTF_8);

            // then
            BigDecimal expectedInvoiced = new BigDecimal("1000");
            BigDecimal expectedPaid = new BigDecimal("200");
            BigDecimal expectedOpen = new BigDecimal("800");
            assertThat(csv).contains("Category");
            assertThat(csv).contains("Code");
            assertThat(csv).contains("Invoiced");
            assertThat(csv).contains("Report Cat");
            assertThat(csv).contains("REP");
            assertThat(csv).contains("\"Report Cat\";\"REP\";\"1000.00\";\"200.00\";\"800.00\"");
            assertThat(csv).contains("\"800.00\";\"30.0\";\"1\";\"1\";\"0\"");
            assertThat(expectedOpen).isEqualByComparingTo(expectedInvoiced.subtract(expectedPaid));
        });
    }

    @Test
    void testReportDefaultTemplateProducesCsv() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            LocalDate fromDate = REFERENCE_DATE.minusDays(60);
            LocalDate toDate = REFERENCE_DATE;
            LocalDate asOfDate = REFERENCE_DATE;
            Category category = entities.category("Csv Default Cat", "CSVDEF");
            CategoryItem item = entities.categoryItem("Csv Default Item", "CDI", category, BigDecimal.valueOf(500), UomType.PIECES);
            Client client = entities.client("Csv Default Client");

            Order order = entities.order(client, REFERENCE_DATE.minusDays(20), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(java.util.List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(500));
            order = dataManager.save(order);

            entities.invoice(client, order, BigDecimal.valueOf(500), InvoiceStatus.PENDING, REFERENCE_DATE.minusDays(15));

            Map<String, Object> params = Map.of(
                    "client", client,
                    "fromDate", toDate(fromDate),
                    "toDate", toDate(toDate),
                    "asOfDate", toDate(asOfDate),
                    "includePaid", true
            );

            // when
            ReportOutputDocument document = reportRunner
                    .byReportCode("category-cashflow-risk-report")
                    .withParams(params)
                    .run();

            // then
            String csv = new String(document.getContent(), StandardCharsets.UTF_8);
            assertThat(csv).contains("Category");
            assertThat(csv).contains("Csv Default Cat");
            assertThat(csv).contains("CSVDEF");
            assertThat(csv).contains("\"Csv Default Cat\";\"CSVDEF\";\"500.00\";\"0\";\"500.00\"");
            assertThat(csv).contains("\"0\";\"\";\"0\";\"1\";\"0\"");
        });
    }

    @Test
    void testReportXlsxTemplateProducesXlsx() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            LocalDate fromDate = REFERENCE_DATE.minusDays(60);
            LocalDate toDate = REFERENCE_DATE;
            LocalDate asOfDate = REFERENCE_DATE;
            Category category = entities.category("Xlsx Cat", "XLSX");
            CategoryItem item = entities.categoryItem("Xlsx Item", "XI", category, BigDecimal.valueOf(500), UomType.PIECES);
            Client client = entities.client("Xlsx Client");

            Order order = entities.order(client, REFERENCE_DATE.minusDays(20), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, BigDecimal.valueOf(1));
            order.setOrderItems(java.util.List.of(orderItem));
            order.setTotal(BigDecimal.valueOf(500));
            order = dataManager.save(order);

            entities.invoice(client, order, BigDecimal.valueOf(500), InvoiceStatus.PENDING, REFERENCE_DATE.minusDays(15));

            Map<String, Object> params = Map.of(
                    "client", client,
                    "fromDate", toDate(fromDate),
                    "toDate", toDate(toDate),
                    "asOfDate", toDate(asOfDate),
                    "includePaid", true
            );

            // when
            ReportOutputDocument document = reportRunner
                    .byReportCode("category-cashflow-risk-report")
                    .withTemplateCode("XLSX")
                    .withParams(params)
                    .run();

            // then
            byte[] content = document.getContent();
            assertThat(content).isNotEmpty();
            assertThat(content.length).isGreaterThan(4);
            assertThat(content[0]).isEqualTo((byte) 'P');
            assertThat(content[1]).isEqualTo((byte) 'K');
            String xlsxXmlText = extractXlsxXmlText(content);
            assertThat(xlsxXmlText).contains("Xlsx Cat");
            assertThat(xlsxXmlText).contains("XLSX");
            assertThat(xlsxXmlText).contains("500");
        });
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private String extractXlsxXmlText(byte[] content) {
        StringBuilder xmlText = new StringBuilder();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".xml")) {
                    xmlText.append(new String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8));
                }
                zipInputStream.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not inspect XLSX content", e);
        }
        return xmlText.toString();
    }
}
