package com.company.crm.ai.report.introspection.model;

import java.util.List;

/**
 * AI-optimized descriptor representing a single report.
 */
public record AiReportDescriptor(String code,
                                 String name,
                                 String description,
                                 String group,
                                 List<AiReportTemplateDescriptor> templates,
                                 List<AiReportParameterDescriptor> parameters) {
}
