package com.company.crm.ai.report.run;

import java.util.List;
import java.util.Map;

/**
 * Result of report parameter conversion
 */
public record ReportParameterConversionResult(
        boolean success,
        Map<String, Object> convertedParameters,
        List<ReportValidationError> errors,
        boolean hasConversionErrors
) {
    public static ReportParameterConversionResult success(Map<String, Object> convertedParameters) {
        return new ReportParameterConversionResult(true, convertedParameters, List.of(), false);
    }

    public static ReportParameterConversionResult failed(List<ReportValidationError> errors, boolean hasConversionErrors) {
        return new ReportParameterConversionResult(false, Map.of(), errors, hasConversionErrors);
    }
}
