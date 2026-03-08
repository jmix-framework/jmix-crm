package com.company.crm.ai.jmix.query;

import org.junit.jupiter.api.Test;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.assertj.core.api.Assertions.assertThat;

class AiJpqlQueryServiceParameterExtractionTest {

    private static final Pattern NAMED_PARAMETER_PATTERN = Pattern.compile("(?<!:):([A-Za-z_][A-Za-z0-9_]*)");

    @Test
    void shouldExtractClientIdParameterFromQuery() {
        // given
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId";

        // when
        Set<String> parameterNames = extractNamedParameters(jpqlQuery);

        // then
        assertThat(parameterNames).containsExactly("clientId");
    }

    @Test
    void shouldExtractMultipleParameters() {
        // given
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId AND c.status = :status";

        // when
        Set<String> parameterNames = extractNamedParameters(jpqlQuery);

        // then
        assertThat(parameterNames).containsExactlyInAnyOrder("clientId", "status");
    }

    @Test
    void shouldIgnoreDoubleColonParameters() {
        // given
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId AND c.created > ::timestamp";

        // when
        Set<String> parameterNames = extractNamedParameters(jpqlQuery);

        // then
        assertThat(parameterNames).containsExactly("clientId");
    }

    private Set<String> extractNamedParameters(String jpqlQuery) {
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
}