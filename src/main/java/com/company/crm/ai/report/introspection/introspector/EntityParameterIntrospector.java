package com.company.crm.ai.report.introspection.introspector;

import com.company.crm.ai.report.introspection.model.AiReportParameterDescriptor;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportInputParameter;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Introspects entity-related report parameters (ENTITY, ENTITY_LIST).
 */
@Component
public class EntityParameterIntrospector implements ReportParameterIntrospector {

    private static final Set<ParameterType> SUPPORTED_TYPES = EnumSet.of(
            ParameterType.ENTITY,
            ParameterType.ENTITY_LIST
    );

    @Override
    public boolean supports(ReportInputParameter parameter) {
        return parameter.getType() != null && SUPPORTED_TYPES.contains(parameter.getType());
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
                parameter.getEntityMetaClass(),
                null,
                parameter.getDefaultValue()
        );
    }
}
