package com.company.crm.app.service.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CategoryAccumulator {
    private final String categoryCode;
    private final String categoryName;
    private BigDecimal invoicedAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal openAmount = BigDecimal.ZERO;
    private BigDecimal overdueOpenAmount = BigDecimal.ZERO;
    private BigDecimal dtcNumerator = BigDecimal.ZERO;
    private BigDecimal dtcDenominator = BigDecimal.ZERO;
    private long paymentsCount = 0;
    private long invoicesCount = 0;
    private BigDecimal overpaymentAmount = BigDecimal.ZERO;

    private CategoryAccumulator(String categoryCode, String categoryName) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
    }

    static CategoryAccumulator fromRiskPosition(InvoiceRiskAssessmentService.CategoryRiskPosition position) {
        CategoryAccumulator accumulator = new CategoryAccumulator(position.categoryCode(), position.categoryName());
        accumulator.invoicedAmount = position.invoicedAmount();
        accumulator.paidAmount = position.paidAmount();
        accumulator.openAmount = position.openAmount();
        accumulator.overdueOpenAmount = position.overdueOpenAmount();
        accumulator.dtcNumerator = position.dtcNumerator();
        accumulator.dtcDenominator = position.dtcDenominator();
        accumulator.paymentsCount = position.paymentsCount();
        accumulator.invoicesCount = 1;
        return accumulator;
    }

    static CategoryAccumulator fromOverpayment(String categoryCode, String categoryName, BigDecimal overpaymentAmount) {
        CategoryAccumulator accumulator = new CategoryAccumulator(categoryCode, categoryName);
        accumulator.overpaymentAmount = overpaymentAmount;
        return accumulator;
    }

    static CategoryAccumulator merge(CategoryAccumulator left, CategoryAccumulator right) {
        CategoryAccumulator merged = new CategoryAccumulator(
                left.categoryCode,
                left.categoryName != null ? left.categoryName : right.categoryName
        );
        merged.invoicedAmount = left.invoicedAmount.add(right.invoicedAmount);
        merged.paidAmount = left.paidAmount.add(right.paidAmount);
        merged.openAmount = left.openAmount.add(right.openAmount);
        merged.overdueOpenAmount = left.overdueOpenAmount.add(right.overdueOpenAmount);
        merged.dtcNumerator = left.dtcNumerator.add(right.dtcNumerator);
        merged.dtcDenominator = left.dtcDenominator.add(right.dtcDenominator);
        merged.paymentsCount = left.paymentsCount + right.paymentsCount;
        merged.invoicesCount = left.invoicesCount + right.invoicesCount;
        merged.overpaymentAmount = left.overpaymentAmount.add(right.overpaymentAmount);
        return merged;
    }

    CategoryRiskMetrics toMetrics() {
        Double dtc = null;
        if (dtcDenominator.compareTo(BigDecimal.ZERO) > 0) {
            dtc = dtcNumerator.divide(dtcDenominator, 2, RoundingMode.HALF_UP).doubleValue();
        }

        return new CategoryRiskMetrics(
                categoryCode,
                categoryName,
                invoicedAmount,
                paidAmount,
                openAmount,
                overdueOpenAmount,
                dtc,
                paymentsCount,
                invoicesCount,
                overpaymentAmount
        );
    }
}
