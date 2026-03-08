package com.company.crm.ai.report.introspection.model;

/**
 * AI-optimized descriptor for a report input parameter.
 */
public record AiReportParameterDescriptor(String alias,
                                          String name,
                                          String type,
                                          boolean required,
                                          boolean hidden,
                                          String entityMetaClass,
                                          String enumerationClass,
                                          String defaultValue) {
}
