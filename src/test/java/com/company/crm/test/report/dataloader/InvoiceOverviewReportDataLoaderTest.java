package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PercentDataType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.report.dataloader.InvoiceOverviewReportDataLoader;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InvoiceOverviewReportDataLoaderTest extends AbstractTest {

    @Autowired
    private InvoiceOverviewReportDataLoader dataLoader;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void testLoadDataWithCompleteFinancialData() {
        // given
        Client client = entities.client("Financial Client");

        // Create orders and invoices
        Order order1 = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);
        Order order2 = entities.order(client, LocalDate.of(2024, 1, 20), OrderStatus.DONE);

        Invoice invoice1 = entities.invoice(client, order1, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 15));
        Invoice invoice2 = entities.invoice(client, order2, BigDecimal.valueOf(750.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 20));

        // Invoice outside date range - should not be counted
        Invoice invoice3 = entities.invoice(client, order1, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2023, 12, 31));

        // Create payments
        Payment payment1 = entities.payment(invoice1, LocalDate.of(2024, 1, 16), BigDecimal.valueOf(1000.00));
        Payment payment2 = entities.payment(invoice2, LocalDate.of(2024, 1, 25), BigDecimal.valueOf(250.00)); // Partial payment

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        Map<String, Object> overview = result.get(0);

        // Should count only invoices in date range (2 invoices)
        assertThat(overview.get("totalInvoiceCount")).isEqualTo(2L);

        // Financial data should include all client invoices/payments (not filtered by date)
        assertThat(overview).containsKeys(
            "totalInvoiceCount", "totalInvoiced", "totalPaid", "outstanding", "paymentRate"
        );

        BigDecimal expectedTotalInvoiced = invoice1.getTotal().add(invoice2.getTotal()).add(invoice3.getTotal());
        BigDecimal expectedTotalPaid = payment1.getAmount().add(payment2.getAmount());
        BigDecimal expectedOutstanding = expectedTotalInvoiced.subtract(expectedTotalPaid);
        BigDecimal expectedPaymentRate = expectedTotalPaid
                .divide(expectedTotalInvoiced, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        assertThat(overview.get("totalInvoiced")).isEqualTo(PriceDataType.defaultFormat(expectedTotalInvoiced, datatypeFormatter));
        assertThat(overview.get("totalPaid")).isEqualTo(PriceDataType.defaultFormat(expectedTotalPaid, datatypeFormatter));
        assertThat(overview.get("outstanding")).isEqualTo(PriceDataType.defaultFormat(expectedOutstanding, datatypeFormatter));
        assertThat(overview.get("paymentRate")).isEqualTo(new PercentDataType().format(expectedPaymentRate));
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
        assertThat(result).hasSize(1);
        Map<String, Object> overview = result.get(0);

        assertThat(overview.get("totalInvoiceCount")).isEqualTo(0L);
        assertThat(overview.get("totalInvoiced")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO, datatypeFormatter));
        assertThat(overview.get("totalPaid")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO, datatypeFormatter));
        assertThat(overview.get("outstanding")).isEqualTo(PriceDataType.defaultFormat(BigDecimal.ZERO, datatypeFormatter));
        assertThat(overview.get("paymentRate")).isEqualTo(new PercentDataType().format(BigDecimal.ZERO));
    }

    @Test
    void testLoadDataDateFiltering() {
        // given
        Client client = entities.client("Date Filter Client");

        Order order = entities.order(client, LocalDate.of(2024, 1, 15), OrderStatus.DONE);

        // Invoice in date range
        Invoice invoiceInRange = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 6, 15));

        // Invoice outside date range
        Invoice invoiceOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2024, 7, 15));

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 6, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 6, 30))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        Map<String, Object> overview = result.get(0);

        // Should count only invoice in date range
        assertThat(overview.get("totalInvoiceCount")).isEqualTo(1L);
        assertThat(overview.get("totalInvoiced")).isEqualTo(PriceDataType.defaultFormat(invoiceInRange.getTotal().add(invoiceOutOfRange.getTotal()), datatypeFormatter));
    }

    @Test
    void testLoadDataAlwaysReturnsOneRow() {
        // given
        Client client = entities.client("Single Row Client");

        Map<String, Object> params = Map.of(
                "client", client,
                "fromDate", Date.valueOf(LocalDate.of(2024, 1, 1)),
                "toDate", Date.valueOf(LocalDate.of(2024, 1, 31))
        );

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsKeys("totalInvoiceCount", "totalInvoiced", "totalPaid", "outstanding", "paymentRate");
    }

}
