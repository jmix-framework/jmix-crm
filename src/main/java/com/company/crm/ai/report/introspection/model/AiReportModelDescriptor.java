package com.company.crm.ai.report.introspection.model;

import java.util.Map;

/**
 * Root container for AI-optimized report descriptors.
 * Map of report code to its descriptor.
 */
public record AiReportModelDescriptor(Map<String, AiReportDescriptor> reports) {
}
