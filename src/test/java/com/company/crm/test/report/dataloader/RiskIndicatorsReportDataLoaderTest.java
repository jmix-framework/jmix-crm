package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.dataloader.RiskIndicatorsReportDataLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RiskIndicatorsReportDataLoaderTest extends AbstractTest {

    @Autowired
    private RiskIndicatorsReportDataLoader dataLoader;

    @Test
    void testLoadDataWithOverdueInvoices() {
        // Given
        Client client = entities.client("Risk Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        // Create some overdue invoices in date range
        Invoice overdueInvoice1 = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 15));
        Invoice overdueInvoice2 = entities.invoice(client, order, BigDecimal.valueOf(750.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 20));

        // Create a paid invoice (should not count as overdue)
        Invoice paidInvoice = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 17));

        // Create overdue invoice outside date range (should not count)
        Invoice overdueOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(300.00), InvoiceStatus.OVERDUE, LocalDate.of(2023, 12, 31));

        dataManager.save(overdueInvoice1, overdueInvoice2, paidInvoice, overdueOutOfRange);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> riskData = result.get(0);

        // Should count only overdue invoices in date range
        assertThat(riskData.get("overdueCount")).isEqualTo(2L);
        assertThat(riskData.get("overdueAmount")).isInstanceOf(String.class);

        // Check all expected fields are present
        assertThat(riskData).containsKeys(
            "overdueCount", "overdueAmount", "avgPaymentDuration",
            "avgPaymentDurationFormatted", "riskLevel", "riskLevelClass"
        );

        // Check data types
        assertThat(riskData.get("avgPaymentDuration")).isInstanceOf(Double.class);
        assertThat(riskData.get("avgPaymentDurationFormatted")).isInstanceOf(String.class);
        assertThat(riskData.get("riskLevelClass")).isInstanceOf(String.class);

        // Check that avgPaymentDurationFormatted has proper format
        String durationFormatted = (String) riskData.get("avgPaymentDurationFormatted");
        assertThat(durationFormatted).endsWith(" days");
    }

    @Test
    void testLoadDataWithNoOverdueInvoices() {
        // Given
        Client client = entities.client("Safe Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        // Create only paid invoices
        Invoice paidInvoice1 = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 15));
        Invoice paidInvoice2 = entities.invoice(client, order, BigDecimal.valueOf(750.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 20));

        dataManager.save(paidInvoice1, paidInvoice2);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> riskData = result.get(0);

        assertThat(riskData.get("overdueCount")).isEqualTo(0L);
        assertThat(riskData.get("overdueAmount")).isInstanceOf(String.class);
    }

    @Test
    void testLoadDataDateFiltering() {
        // Given
        Client client = entities.client("Date Filter Client");
        dataManager.save(client);

        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        dataManager.save(order);

        // Overdue invoice in date range
        Invoice overdueInRange = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 6, 15));

        // Overdue invoice outside date range
        Invoice overdueOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 7, 15));

        dataManager.save(overdueInRange, overdueOutOfRange);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> riskData = result.get(0);

        // Should count only overdue invoices in date range
        assertThat(riskData.get("overdueCount")).isEqualTo(1L);
    }

    @Test
    void testLoadDataRiskLevelClassMapping() {
        // Given
        Client client = entities.client("Risk Mapping Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> riskData = result.get(0);

        String riskLevelClass = (String) riskData.get("riskLevelClass");

        // Should be one of the expected CSS classes
        assertThat(riskLevelClass).isIn("risk-low", "risk-medium", "risk-high");
    }

    @Test
    void testLoadDataAlwaysReturnsOneRow() {
        // Given
        Client client = entities.client("Single Row Client");
        dataManager.save(client);

        Map<String, Object> params = createParams(client,
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // When
        List<Map<String, Object>> result = dataLoader.loadData(null, null, params);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNotEmpty();
    }

    private Map<String, Object> createParams(Client client, LocalDate fromDate, LocalDate toDate) {
        Map<String, Object> params = new HashMap<>();
        params.put("client", client);
        params.put("fromDate", java.sql.Date.valueOf(fromDate));
        params.put("toDate", java.sql.Date.valueOf(toDate));
        return params;
    }
}