package com.company.crm.ai.jpql.query;

/**
 * Record representing a JPQL parameter for OpenAI-compatible tool calls
 */
public record JpqlParameter(
        String parameterName,
        Object parameterValue
) {
}