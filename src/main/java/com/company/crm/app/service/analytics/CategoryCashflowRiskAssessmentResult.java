package com.company.crm.app.service.analytics;

import java.util.List;

/**
 * Aggregate result for the category cashflow risk report datasets.
 */
public record CategoryCashflowRiskAssessmentResult(
        List<CategoryRiskMetrics> riskByCategory
) {
}
