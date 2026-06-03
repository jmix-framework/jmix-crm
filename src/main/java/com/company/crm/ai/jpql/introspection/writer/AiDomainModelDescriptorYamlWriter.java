package com.company.crm.ai.jpql.introspection.writer;

import com.company.crm.ai.config.YamlObjectMapperFactory;
import com.company.crm.ai.jpql.introspection.model.AiDomainModelDescriptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Writes an AiDomainModelDescriptor to YAML format using Jackson YAML.
 */
@Component
public class AiDomainModelDescriptorYamlWriter {

    private final ObjectMapper yamlMapper = YamlObjectMapperFactory.createYamlObjectMapper();

    public AiDomainModelDescriptorYamlWriter() {
    }

    public String writeToYaml(AiDomainModelDescriptor domainModel) {
        try {
            return yamlMapper.writeValueAsString(domainModel);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write YAML", e);
        }
    }

}