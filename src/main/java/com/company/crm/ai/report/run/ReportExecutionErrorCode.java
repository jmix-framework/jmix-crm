package com.company.crm.ai.report.run;

/**
 * Machine-readable error codes for report execution failures.
 */
public enum ReportExecutionErrorCode {
    ACCESS_DENIED,
    REPORT_NOT_FOUND,
    TEMPLATE_NOT_FOUND,
    INVALID_OUTPUT_TYPE,
    PARAMETER_VALIDATION_ERROR,
    PARAMETER_CONVERSION_ERROR,
    BINARY_OUTPUT_NOT_SUPPORTED_YET,
    EXECUTION_ERROR
}
