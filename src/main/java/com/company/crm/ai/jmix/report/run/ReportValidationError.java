package com.company.crm.ai.jmix.report.run;

/**
 * Detailed validation error for report parameters
 */
public record ReportValidationError(String parameterAlias, String errorMessage) {
}
