package com.company.crm.app.service.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryAccumulatorTest {

    @Test
    void fromRiskPosition_mapsValuesToMetrics() {
        // given
        InvoiceRiskAssessmentService.CategoryRiskPosition position =
                new InvoiceRiskAssessmentService.CategoryRiskPosition(
                        "CAT1",
                        "Category 1",
                        new BigDecimal("500.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("250.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("2500.00"),
                        new BigDecimal("250.00"),
                        2L
                );

        // when
        CategoryRiskMetrics metrics = CategoryAccumulator.fromRiskPosition(position).toMetrics();

        // then
        assertThat(metrics.categoryCode()).isEqualTo("CAT1");
        assertThat(metrics.categoryName()).isEqualTo("Category 1");
        assertThat(metrics.invoicedAmount()).isEqualByComparingTo("500.00");
        assertThat(metrics.paidAmount()).isEqualByComparingTo("250.00");
        assertThat(metrics.openAmount()).isEqualByComparingTo("250.00");
        assertThat(metrics.overdueOpenAmount()).isEqualByComparingTo("100.00");
        assertThat(metrics.dtcDaysWeighted()).isEqualTo(10.0);
        assertThat(metrics.paymentsCount()).isEqualTo(2L);
        assertThat(metrics.invoicesCount()).isEqualTo(1L);
        assertThat(metrics.overpaymentAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void fromOverpayment_setsOnlyOverpaymentAndNoDtc() {
        // when
        CategoryRiskMetrics metrics = CategoryAccumulator
                .fromOverpayment("UNASSIGNED", "UNASSIGNED", new BigDecimal("42.00"))
                .toMetrics();

        // then
        assertThat(metrics.categoryCode()).isEqualTo("UNASSIGNED");
        assertThat(metrics.categoryName()).isEqualTo("UNASSIGNED");
        assertThat(metrics.invoicedAmount()).isEqualByComparingTo("0.00");
        assertThat(metrics.paidAmount()).isEqualByComparingTo("0.00");
        assertThat(metrics.openAmount()).isEqualByComparingTo("0.00");
        assertThat(metrics.overdueOpenAmount()).isEqualByComparingTo("0.00");
        assertThat(metrics.dtcDaysWeighted()).isNull();
        assertThat(metrics.paymentsCount()).isEqualTo(0L);
        assertThat(metrics.invoicesCount()).isEqualTo(0L);
        assertThat(metrics.overpaymentAmount()).isEqualByComparingTo("42.00");
    }

    @Test
    void merge_sumsTwoAccumulators() {
        // given
        InvoiceRiskAssessmentService.CategoryRiskPosition first =
                new InvoiceRiskAssessmentService.CategoryRiskPosition(
                        "CAT1",
                        "Category 1",
                        new BigDecimal("500.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("400.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("100.00"),
                        1L
                );
        InvoiceRiskAssessmentService.CategoryRiskPosition second =
                new InvoiceRiskAssessmentService.CategoryRiskPosition(
                        "CAT1",
                        "Category 1",
                        new BigDecimal("300.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("2100.00"),
                        new BigDecimal("300.00"),
                        1L
                );

        // when
        CategoryAccumulator merged = CategoryAccumulator.merge(
                CategoryAccumulator.fromRiskPosition(first),
                CategoryAccumulator.merge(
                        CategoryAccumulator.fromRiskPosition(second),
                        CategoryAccumulator.fromOverpayment("CAT1", "Category 1", new BigDecimal("5.00"))
                )
        );

        CategoryRiskMetrics metrics = merged.toMetrics();

        // then
        BigDecimal expectedInvoicedAmount = new BigDecimal("800.00");
        BigDecimal expectedPaidAmount = new BigDecimal("400.00");
        BigDecimal expectedOpenAmount = new BigDecimal("400.00");
        BigDecimal expectedOverdueOpenAmount = new BigDecimal("200.00");
        Double expectedDtcDaysWeighted = 6.5;
        long expectedPaymentsCount = 2L;
        long expectedInvoicesCount = 2L;
        BigDecimal expectedOverpaymentAmount = new BigDecimal("5.00");

        assertThat(metrics.categoryCode()).isEqualTo("CAT1");
        assertThat(metrics.categoryName()).isEqualTo("Category 1");
        assertThat(metrics.invoicedAmount()).isEqualByComparingTo(expectedInvoicedAmount);
        assertThat(metrics.paidAmount()).isEqualByComparingTo(expectedPaidAmount);
        assertThat(metrics.openAmount()).isEqualByComparingTo(expectedOpenAmount);
        assertThat(metrics.overdueOpenAmount()).isEqualByComparingTo(expectedOverdueOpenAmount);
        assertThat(metrics.dtcDaysWeighted()).isEqualTo(expectedDtcDaysWeighted);
        assertThat(metrics.paymentsCount()).isEqualTo(expectedPaymentsCount);
        assertThat(metrics.invoicesCount()).isEqualTo(expectedInvoicesCount);
        assertThat(metrics.overpaymentAmount()).isEqualByComparingTo(expectedOverpaymentAmount);
    }

    @Test
    void merge_withZeroAccumulator_keepsOriginalMetrics() {
        // given
        InvoiceRiskAssessmentService.CategoryRiskPosition source =
                new InvoiceRiskAssessmentService.CategoryRiskPosition(
                        "CAT1",
                        "Category 1",
                        new BigDecimal("500.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("150.00"),
                        new BigDecimal("900.00"),
                        new BigDecimal("300.00"),
                        3L
                );
        CategoryAccumulator base = CategoryAccumulator.fromRiskPosition(source);
        CategoryAccumulator zero = CategoryAccumulator.fromOverpayment("CAT1", "Category 1", BigDecimal.ZERO);

        // when
        CategoryRiskMetrics metrics = CategoryAccumulator.merge(base, zero).toMetrics();

        // then
        assertThat(metrics.categoryCode()).isEqualTo("CAT1");
        assertThat(metrics.invoicedAmount()).isEqualByComparingTo("500.00");
        assertThat(metrics.paidAmount()).isEqualByComparingTo("200.00");
        assertThat(metrics.openAmount()).isEqualByComparingTo("300.00");
        assertThat(metrics.overdueOpenAmount()).isEqualByComparingTo("150.00");
        assertThat(metrics.dtcDaysWeighted()).isEqualTo(3.0);
        assertThat(metrics.paymentsCount()).isEqualTo(3L);
        assertThat(metrics.invoicesCount()).isEqualTo(1L);
        assertThat(metrics.overpaymentAmount()).isEqualByComparingTo("0.00");
    }
}
