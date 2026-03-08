package com.company.crm.ai.report.introspection;

import com.company.crm.ai.report.introspection.introspector.JmixReportIntrospector;
import com.company.crm.ai.report.introspection.model.AiReportModelDescriptor;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * High-level service for exporting AI-optimized report descriptors to YAML.
 */
@Component
public class AiReportModelDescriptorYamlExporter {

    private final JmixReportIntrospector introspector;
    private final AiReportModelDescriptorYamlWriter yamlWriter;

    public AiReportModelDescriptorYamlExporter(JmixReportIntrospector introspector,
                                              AiReportModelDescriptorYamlWriter yamlWriter) {
        this.introspector = introspector;
        this.yamlWriter = yamlWriter;
    }

    /**
     * Exports all available reports as AI descriptors in YAML format.
     *
     * @return YAML string representation of all available reports
     */
    public String export() {
        AiReportModelDescriptor reportModel = introspector.introspect();
        return yamlWriter.writeToYaml(reportModel);
    }

    /**
     * Exports specific reports by their codes as AI descriptors in YAML format.
     *
     * @param reportCodes collection of report codes to include
     * @return YAML string representation of requested reports
     */
    public String export(Collection<String> reportCodes) {
        AiReportModelDescriptor reportModel = introspector.introspect(reportCodes);
        return yamlWriter.writeToYaml(reportModel);
    }
}
