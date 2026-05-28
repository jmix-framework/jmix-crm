package com.company.crm.ai.report.introspection;

import com.company.crm.ai.config.YamlObjectMapperFactory;
import com.company.crm.ai.report.introspection.model.AiReportModelDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Writes an AiReportModelDescriptor to YAML format using Jackson YAML.
 * Optimized for LLM readability.
 */
@Component
public class AiReportModelDescriptorYamlWriter {

    private final ObjectMapper yamlMapper = YamlObjectMapperFactory.createYamlObjectMapper();

    public AiReportModelDescriptorYamlWriter() {
    }

    public String writeToYaml(AiReportModelDescriptor reportModel) {
        try {
            return yamlMapper.writeValueAsString(reportModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write report model YAML", e);
        }
    }
}
