package com.company.crm.ai.jmix.introspection.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * AI-optimized property descriptor representing any property (attribute or relation).
 * Uses factory methods for better readability and JsonInclude to eliminate null fields in YAML.
 */
public record AiPropertyDescriptor(
        String caption, String comment, String type, String javaType,
        // Optional fields - only set when relevant for specific property types
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean identifier,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean embedded,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> enumValues,
        @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, String> enumDescriptions,
        @JsonInclude(JsonInclude.Include.NON_NULL) String target,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean optional,
        @JsonInclude(JsonInclude.Include.NON_NULL) String mappedBy
) {
    /**
     * Factory method for datatype properties (String, Integer, Date, etc.)
     */
    public static AiPropertyDescriptor dataProperty(String caption, String comment, String javaType, Boolean identifier) {
        return new AiPropertyDescriptor(caption, comment, "datatype", javaType, identifier, null, null, null, null, null, null);
    }

    /**
     * Factory method for enum properties
     */
    public static AiPropertyDescriptor enumProperty(String caption, String comment, String javaType,
                                                   Map<String, Object> enumValues, Map<String, String> enumDescriptions) {
        return new AiPropertyDescriptor(caption, comment, "enum", javaType, null, null, enumValues, enumDescriptions, null, null, null);
    }

    /**
     * Factory method for relation properties (associations, compositions)
     */
    public static AiPropertyDescriptor relationProperty(String caption, String comment, String type, String javaType,
                                                       String target, Boolean optional, String mappedBy) {
        return new AiPropertyDescriptor(caption, comment, type, javaType, null, null, null, null, target, optional, mappedBy);
    }
}