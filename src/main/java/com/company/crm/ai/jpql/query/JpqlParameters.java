package com.company.crm.ai.jpql.query;

import java.util.List;
import java.util.Map;

/**
 * Wrapper record for JPQL parameters that provides factory methods for convenient creation.
 */
public record JpqlParameters(
        List<JpqlParameter> parameters
) {

    /**
     * Create empty parameters list
     */
    public static JpqlParameters empty() {
        return new JpqlParameters(List.of());
    }

    /**
     * Create parameters from a Map
     */
    public static JpqlParameters fromMap(Map<String, Object> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return empty();
        }
        List<JpqlParameter> params = parameterMap.entrySet().stream()
                .map(entry -> new JpqlParameter(entry.getKey(), entry.getValue()))
                .toList();
        return new JpqlParameters(params);
    }
}