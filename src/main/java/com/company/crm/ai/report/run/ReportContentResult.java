package com.company.crm.ai.report.run;

/**
 * Type-safe report content conversion result.
 */
public sealed interface ReportContentResult
        permits ReportContentResult.TextContent, ReportContentResult.BinaryUnsupported {

    record TextContent(String content) implements ReportContentResult {
    }

    record BinaryUnsupported(String outputType) implements ReportContentResult {
    }
}
