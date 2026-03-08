package com.company.crm.ai.report.introspection.model;

/**
 * AI-optimized descriptor for a report template.
 */
public record AiReportTemplateDescriptor(String code,
                                         String outputType,
                                         boolean isDefault) {
}
