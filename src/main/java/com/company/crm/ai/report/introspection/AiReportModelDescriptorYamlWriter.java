package com.company.crm.ai.report.introspection;

import com.company.crm.ai.report.introspection.model.AiReportModelDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.springframework.stereotype.Component;

/**
 * Writes an AiReportModelDescriptor to YAML format using Jackson YAML.
 * Optimized for LLM readability.
 */
@Component
public class AiReportModelDescriptorYamlWriter {

    private final ObjectMapper yamlMapper;

    public AiReportModelDescriptorYamlWriter() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

        this.yamlMapper = new ObjectMapper(yamlFactory);
        this.yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public String writeToYaml(AiReportModelDescriptor reportModel) {
        try {
            return yamlMapper.writeValueAsString(reportModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write report model YAML", e);
        }
    }
}
