package com.company.crm.ai.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public final class YamlObjectMapperFactory {

    private YamlObjectMapperFactory() {
        // utility class
    }

    public static ObjectMapper createYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);

        ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);
        yamlMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
        return yamlMapper;
    }
}
