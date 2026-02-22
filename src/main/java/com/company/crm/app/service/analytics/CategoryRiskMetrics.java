package com.company.crm.app.service.analytics;

import java.math.BigDecimal;

/**
 * DTO representing risk metrics for a category.
 */
public record CategoryRiskMetrics(
        String categoryCode,
        String categoryName,
        BigDecimal invoicedAmount,
        BigDecimal paidAmount,
        BigDecimal openAmount,
        BigDecimal overdueOpenAmount, // Revenue at Risk (RaR)
        Double dtcDaysWeighted,       // Days to Cash (DTC)
        long paymentsCount,
        long invoicesCount,
        BigDecimal overpaymentAmount
) {
}
