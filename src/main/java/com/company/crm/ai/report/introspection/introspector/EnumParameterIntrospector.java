package com.company.crm.ai.report.introspection.introspector;

import com.company.crm.ai.report.introspection.model.AiReportParameterDescriptor;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportInputParameter;
import org.springframework.stereotype.Component;

/**
 * Introspects enumeration report parameters.
 */
@Component
public class EnumParameterIntrospector implements ReportParameterIntrospector {

    @Override
    public boolean supports(ReportInputParameter parameter) {
        return parameter.getType() == ParameterType.ENUMERATION;
    }

    @Override
    public AiReportParameterDescriptor introspect(ReportInputParameter parameter) {
        if (!supports(parameter)) {
            return null;
        }

        return new AiReportParameterDescriptor(
                parameter.getAlias(),
                parameter.getName(),
                parameter.getType().toString(),
                Boolean.TRUE.equals(parameter.getRequired()),
                Boolean.TRUE.equals(parameter.getHidden()),
                null,
                parameter.getEnumerationClass(),
                parameter.getDefaultValue()
        );
    }
}
