package com.company.crm.ai.report.introspection.introspector;

import com.company.crm.ai.report.introspection.model.AiReportParameterDescriptor;
import io.jmix.reports.entity.ReportInputParameter;

/**
 * Interface for introspecting ReportInputParameter objects to AI-optimized report parameter descriptors.
 */
public interface ReportParameterIntrospector {

    /**
     * Introspects a ReportInputParameter to an AI-optimized report parameter descriptor.
     *
     * @param parameter the report parameter to introspect
     * @return AiReportParameterDescriptor representation of this parameter, null if this introspector cannot handle it
     */
    AiReportParameterDescriptor introspect(ReportInputParameter parameter);

    /**
     * Checks if this introspector can handle the given report parameter type.
     *
     * @param parameter the report parameter to check
     * @return true if this introspector can handle this parameter type
     */
    boolean supports(ReportInputParameter parameter);
}
