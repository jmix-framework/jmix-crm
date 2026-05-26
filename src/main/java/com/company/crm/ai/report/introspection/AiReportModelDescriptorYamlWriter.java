package com.company.crm.ai.report.introspection;

import com.company.crm.ai.report.introspection.model.AiReportModelDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Writes an AiReportModelDescriptor to YAML format using Jackson YAML.
 * Optimized for LLM readability.
 */
@Component
public class AiReportModelDescriptorYamlWriter {

    private final ObjectMapper yamlMapper;

    public AiReportModelDescriptorYamlWriter(@Qualifier("aiYamlObjectMapper") ObjectMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    public String writeToYaml(AiReportModelDescriptor reportModel) {
        try {
            return yamlMapper.writeValueAsString(reportModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write report model YAML", e);
        }
    }
}
