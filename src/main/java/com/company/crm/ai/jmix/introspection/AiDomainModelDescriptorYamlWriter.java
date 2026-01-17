package com.company.crm.ai.jmix.introspection;

import com.company.crm.ai.jmix.introspection.model.AiDomainModelDescriptor;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.springframework.stereotype.Component;

/**
 * Writes an AiDomainModelDescriptor to YAML format using Jackson YAML.
 */
@Component("ai_AiDomainModelDescriptorYamlWriter")
public class AiDomainModelDescriptorYamlWriter {

    private final ObjectMapper yamlMapper;

    public AiDomainModelDescriptorYamlWriter() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

        this.yamlMapper = new ObjectMapper(yamlFactory);
        this.yamlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public String writeToYaml(AiDomainModelDescriptor domainModel) {
        try {
            return yamlMapper.writeValueAsString(domainModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write YAML", e);
        }
    }

}