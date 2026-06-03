package com.company.crm.test.ai.jpql.query;

import com.company.crm.ai.jpql.query.JpqlNamedParameterParser;
import com.company.crm.ai.jpql.query.JpqlParameter;
import com.company.crm.ai.jpql.query.JpqlParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiJpqlQueryServiceParameterExtractionTest {

    private final JpqlNamedParameterParser parser = new JpqlNamedParameterParser();

    @Test
    void shouldExtractClientIdParameterFromQuery() {
        // given
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId";

        // when
        var parameterNames = parser.extractNames(jpqlQuery);

        // then
        assertThat(parameterNames).containsExactly("clientId");
    }

    @Test
    void shouldExtractMultipleParameters() {
        // given
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId AND c.status = :status";

        // when
        var parameterNames = parser.extractNames(jpqlQuery);

        // then
        assertThat(parameterNames).containsExactlyInAnyOrder("clientId", "status");
    }

    @Test
    void shouldIgnoreDoubleColonParameters() {
        // given
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId AND c.created > ::timestamp";

        // when
        var parameterNames = parser.extractNames(jpqlQuery);

        // then
        assertThat(parameterNames).containsExactly("clientId");
    }

    @Test
    void shouldCheckAndRemoveParametersByName() {
        JpqlParameters parameters = new JpqlParameters(List.of(
                new JpqlParameter("clientId", "1"),
                new JpqlParameter("status", "ACTIVE")
        ));

        assertThat(parser.contains(parameters, "clientId")).isTrue();
        assertThat(parser.contains(parameters, "missing")).isFalse();
        assertThat(parser.without(parameters, "status").parameters())
                .extracting(JpqlParameter::parameterName)
                .containsExactly("clientId");
    }

    @Test
    void shouldExtractUnknownParameterNameFromQueryError() {
        String errorMessage = "Query argument strayParam not found in the list of parameters provided during query execution.";

        assertThat(parser.extractUnknownParameterName(errorMessage)).isEqualTo("strayParam");
        assertThat(parser.extractUnknownParameterName("Different error")).isNull();
    }
}
