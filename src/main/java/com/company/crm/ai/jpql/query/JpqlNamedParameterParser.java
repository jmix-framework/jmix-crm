package com.company.crm.ai.jpql.query;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates the logic for parsing and resolving named parameters in JPQL queries.
 */
@Component
public class JpqlNamedParameterParser {

    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile("(?<!:):([A-Za-z_][A-Za-z0-9_]*)");

    /**
     * Extract named parameters from a JPQL query string.
     */
    public Set<String> extractNames(String jpqlQuery) {
        Set<String> names = new HashSet<>();
        if (jpqlQuery == null || jpqlQuery.isBlank()) {
            return names;
        }
        Matcher matcher = NAMED_PARAMETER_PATTERN.matcher(jpqlQuery);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    /**
     * Check if the JpqlParameters contains a parameter by name.
     */
    public boolean contains(JpqlParameters parameters, String parameterName) {
        if (parameters == null || parameters.parameters().isEmpty()) {
            return false;
        }
        return parameters.parameters().stream()
                .anyMatch(param -> param.parameterName().equals(parameterName));
    }

    /**
     * Filter out a parameter by name, returning a new JpqlParameters instance.
     */
    public JpqlParameters without(JpqlParameters parameters, String parameterName) {
        if (parameters == null) {
            return JpqlParameters.empty();
        }
        List<JpqlParameter> filteredList = parameters.parameters().stream()
                .filter(param -> !param.parameterName().equals(parameterName))
                .toList();
        return new JpqlParameters(filteredList);
    }

    /**
     * Extract an unknown parameter name from a database/query error message.
     */
    public String extractUnknownParameterName(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        String marker = "Query argument ";
        int start = errorMessage.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int from = start + marker.length();
        int to = errorMessage.indexOf(' ', from);
        if (to < 0) {
            return null;
        }
        return errorMessage.substring(from, to).trim();
    }
}
