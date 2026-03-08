package com.company.crm.ai.report.run;

import java.util.List;

/**
 * Result object for report execution
 */
public record ReportExecutionResult(
        boolean success,
        String reportCode,
        String templateCodeUsed,
        String outputType,
        String content,
        ReportExecutionErrorCode errorCode,
        String errorMessage,
        List<ReportValidationError> validationErrors
) {

    public static ReportExecutionResult success(String reportCode, String templateCode, String outputType, String content) {
        return new ReportExecutionResult(true, reportCode, templateCode, outputType, content, null, null, null);
    }

    public static ReportExecutionResult failed(String reportCode, ReportExecutionErrorCode errorCode, String errorMessage) {
        return new ReportExecutionResult(false, reportCode, null, null, null, errorCode, errorMessage, null);
    }

    public static ReportExecutionResult validationError(String reportCode, List<ReportValidationError> validationErrors) {
        return new ReportExecutionResult(false, reportCode, null, null, null, ReportExecutionErrorCode.PARAMETER_VALIDATION_ERROR, "One or more parameters are missing or invalid", validationErrors);
    }

    public static ReportExecutionResult parameterConversionError(String reportCode, List<ReportValidationError> conversionErrors) {
        return new ReportExecutionResult(false, reportCode, null, null, null, ReportExecutionErrorCode.PARAMETER_CONVERSION_ERROR, "One or more parameters could not be converted to the required type", conversionErrors);
    }
}
