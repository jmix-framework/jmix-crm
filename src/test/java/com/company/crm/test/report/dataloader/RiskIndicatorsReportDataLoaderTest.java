package com.company.crm.test.report.dataloader;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.RiskLevel;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.report.dataloader.RiskIndicatorsReportDataLoader;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RiskIndicatorsReportDataLoaderTest extends AbstractTest {

    @Autowired
    private RiskIndicatorsReportDataLoader dataLoader;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    @Test
    void loadData_returnsMediumRiskWhenClientHasOverdueInvoicesInRange() {
        // given
        Client client = entities.client("Risk Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);

        Invoice overdueInvoice1 = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 15));
        Invoice overdueInvoice2 = entities.invoice(client, order, BigDecimal.valueOf(750.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 20));
        Invoice paidInvoice = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 17));
        Invoice overdueOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(300.00), InvoiceStatus.OVERDUE, LocalDate.of(2023, 12, 31));

        BigDecimal expectedOverdueAmount = overdueInvoice1.getTotal().add(overdueInvoice2.getTotal());

        // when
        Map<String, Object> riskData = loadRiskData(client, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // then
        assertRiskData(
                riskData,
                2L,
                expectedOverdueAmount,
                0.0,
                RiskLevel.MEDIUM,
                "risk-medium"
        );
        assertThat(riskData).containsKeys(
                "overdueCount",
                "overdueAmount",
                "avgPaymentDuration",
                "avgPaymentDurationFormatted",
                "riskLevel",
                "riskLevelClass"
        );
        assertThat(paidInvoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(overdueOutOfRange.getDate()).isBefore(LocalDate.of(2024, 1, 1));
    }

    @Test
    void loadData_returnsLowRiskWhenClientHasNoOverdueInvoices() {
        // given
        Client client = entities.client("Safe Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);

        Invoice firstInvoice = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 15));
        Invoice secondInvoice = entities.invoice(client, order, BigDecimal.valueOf(750.00), InvoiceStatus.PAID, LocalDate.of(2024, 1, 20));
        entities.payment(firstInvoice, LocalDate.of(2024, 1, 25), BigDecimal.valueOf(1000.00));
        entities.payment(secondInvoice, LocalDate.of(2024, 1, 30), BigDecimal.valueOf(750.00));

        // when
        Map<String, Object> riskData = loadRiskData(client, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // then
        assertRiskData(
                riskData,
                0L,
                BigDecimal.ZERO,
                10.0,
                RiskLevel.LOW,
                "risk-low"
        );
    }

    @Test
    void loadData_filtersInvoicesByDateRangeBeforeCalculatingRisk() {
        // given
        Client client = entities.client("Date Filter Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);

        Invoice overdueInRange = entities.invoice(client, order, BigDecimal.valueOf(1000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 6, 15));
        Invoice overdueOutOfRange = entities.invoice(client, order, BigDecimal.valueOf(500.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 7, 15));

        // when
        Map<String, Object> riskData = loadRiskData(client, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30));

        // then
        assertRiskData(
                riskData,
                1L,
                overdueInRange.getTotal(),
                0.0,
                RiskLevel.MEDIUM,
                "risk-medium"
        );
        assertThat(overdueOutOfRange.getDate()).isAfter(LocalDate.of(2024, 6, 30));
    }

    @Test
    void loadData_returnsHighRiskCssClassWhenHighRiskThresholdIsExceeded() {
        // given
        Client client = entities.client("Risk Mapping Client");
        Order order = entities.order(client, LocalDate.of(2024, 1, 10), OrderStatus.DONE);
        Invoice overdueInvoice1 = entities.invoice(client, order, BigDecimal.valueOf(2000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 15));
        Invoice overdueInvoice2 = entities.invoice(client, order, BigDecimal.valueOf(2000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 16));
        Invoice overdueInvoice3 = entities.invoice(client, order, BigDecimal.valueOf(2000.00), InvoiceStatus.OVERDUE, LocalDate.of(2024, 1, 17));

        BigDecimal expectedOverdueAmount = overdueInvoice1.getTotal()
                .add(overdueInvoice2.getTotal())
                .add(overdueInvoice3.getTotal());

        // when
        Map<String, Object> riskData = loadRiskData(client, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));

        // then
        assertRiskData(
                riskData,
                3L,
                expectedOverdueAmount,
                0.0,
                RiskLevel.HIGH,
                "risk-high"
        );
    }

    @Test
    void loadData_alwaysReturnsOneFullyPopulatedRow() {
        // given
        Client client = entities.client("Single Row Client");

        // when
        List<Map<String, Object>> result = dataLoader.loadData(null, null, createParams(client, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31)));

        // then
        assertThat(result).hasSize(1);
        assertRiskData(
                result.get(0),
                0L,
                BigDecimal.ZERO,
                0.0,
                RiskLevel.LOW,
                "risk-low"
        );
    }

    private Map<String, Object> loadRiskData(Client client, LocalDate fromDate, LocalDate toDate) {
        List<Map<String, Object>> result = dataLoader.loadData(null, null, createParams(client, fromDate, toDate));
        assertThat(result).hasSize(1);
        return result.get(0);
    }

    private Map<String, Object> createParams(Client client, LocalDate fromDate, LocalDate toDate) {
        return Map.of(
                "client", client,
                "fromDate", java.sql.Date.valueOf(fromDate),
                "toDate", java.sql.Date.valueOf(toDate)
        );
    }

    private void assertRiskData(
            Map<String, Object> riskData,
            long expectedOverdueCount,
            BigDecimal expectedOverdueAmount,
            double expectedAvgPaymentDuration,
            RiskLevel expectedRiskLevel,
            String expectedRiskLevelClass
    ) {
        assertThat(riskData.get("overdueCount")).isEqualTo(expectedOverdueCount);
        assertThat(riskData.get("overdueAmount")).isEqualTo(PriceDataType.defaultFormat(expectedOverdueAmount, datatypeFormatter));
        assertThat(riskData.get("avgPaymentDuration")).isEqualTo(expectedAvgPaymentDuration);
        assertThat(riskData.get("avgPaymentDurationFormatted")).isEqualTo("%.0f days".formatted(expectedAvgPaymentDuration));
        assertThat(riskData.get("riskLevel")).isEqualTo(expectedRiskLevel);
        assertThat(riskData.get("riskLevelClass")).isEqualTo(expectedRiskLevelClass);
    }
}
