package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.dataloader.InvoiceStatusBreakdownReportDataLoader;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceStatusBreakdownReportDataLoaderTest extends AbstractTest {

    @Autowired
    private InvoiceStatusBreakdownReportDataLoader dataLoader;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void testLoadDataWithVariousStatuses() {
        // given
        Client client = entities.client("Status Client");

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);

        // Create invoices with different statuses
        Invoice invoice1 = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.NEW, LocalDate.of(2024, 1, 15));
        Invoice invoice2 = entities.invoice(client, order, BigDecimal.valueOf(750.00), InvoiceStatus.NEW, LocalDate.of(2024, 1, 16));
        Invoice invoice3 = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 17));
        Invoice invoice4 = entities.invoice(client, order, BigDecimal.valueOf(300.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 18));

        // Invoice outside date range - should not be included
        Invoice invoiceOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(200.00), InvoiceStatus.NEW, LocalDate.of(2023, 12, 31));

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        // Should return one row for each InvoiceStatus enum value
        assertThat(result).hasSize(InvoiceStatus.values().length);

        // Find the breakdown for NEW status
        Map<String, Object> newStatus = result.stream()
            .filter(row -> "NEW".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        assertThat(newStatus.get("count")).isEqualTo(2L);
        assertThat(newStatus.get("amount")).isEqualTo(PriceDataType.defaultFormat(invoice1.getTotal().add(invoice2.getTotal()), datatypeFormatter));
        assertThat(newStatus.get("statusFormatted")).isInstanceOf(String.class);

        // Find the breakdown for PAID status
        Map<String, Object> paidStatus = result.stream()
            .filter(row -> "PAID".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        assertThat(paidStatus.get("count")).isEqualTo(1L);
        assertThat(paidStatus.get("amount")).isEqualTo(PriceDataType.defaultFormat(invoice3.getTotal(), datatypeFormatter));

        // Find the breakdown for OVERDUE status
        Map<String, Object> overdueStatus = result.stream()
            .filter(row -> "OVERDUE".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        assertThat(overdueStatus.get("count")).isEqualTo(1L);
        assertThat(overdueStatus.get("amount")).isEqualTo(PriceDataType.defaultFormat(invoice4.getTotal(), datatypeFormatter));

        // Check that all rows have the expected structure
        result.forEach(row -> {
            assertThat(row).containsKeys("status", "statusFormatted", "count", "amount");
            assertThat(row.get("count")).isInstanceOf(Long.class);
            assertThat(row.get("amount")).isInstanceOf(String.class);
        });
    }

    @Test
    void testLoadDataWithNoInvoices() {
        // given
        Client client = entities.client("No Invoices Client");

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        // Should still return one row for each status, but with count 0
        assertThat(result).hasSize(InvoiceStatus.values().length);

        result.forEach(row -> {
            assertThat(row.get("count")).isEqualTo(0L);
            assertThat(row.get("amount")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO, datatypeFormatter));
        });
    }

    @Test
    void testLoadDataDateFiltering() {
        // given
        Client client = entities.client("Date Filter Client");

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);

        // Invoice in date range
        Invoice invoiceInRange = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.NEW, LocalDate.of(2024, 6, 15));

        // Invoice outside date range
        Invoice invoiceOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.NEW, LocalDate.of(2024, 7, 15));

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 6, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 6, 30))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        Map<String, Object> newStatus = result.stream()
            .filter(row -> "NEW".equals(row.get("status")))
            .findFirst()
            .orElseThrow();

        // Should count only the invoice in date range
        assertThat(newStatus.get("count")).isEqualTo(1L);
        assertThat(newStatus.get("amount")).isEqualTo(PriceDataType.defaultFormat(invoiceInRange.getTotal(), datatypeFormatter));
    }

    @Test
    void testLoadDataAlwaysReturnsAllStatuses() {
        // given
        Client client = entities.client("All Statuses Client");

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(InvoiceStatus.values().length);
        assertThat(result.stream().map(row -> (String) row.get("status")).toList())
                .containsExactly(Arrays.stream(InvoiceStatus.values()).map(Enum::name).toArray(String[]::new));
    }

}
