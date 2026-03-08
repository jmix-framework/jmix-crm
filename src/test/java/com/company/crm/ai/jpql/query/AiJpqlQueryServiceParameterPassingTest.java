package com.company.crm.ai.jpql.query;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiJpqlQueryServiceParameterPassingTest extends AbstractTest {

    @Autowired
    private AiJpqlQueryService aiJpqlQueryService;

    @Test
    void shouldExecuteQueryWithClientIdParameter() {
        // given
        String clientName = "TEST-CLIENT-" + UUID.randomUUID();
        Client client = systemAuthenticator.withSystem(() -> entities.client(clientName));
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId";
        Map<String, Object> parameters = Map.of("clientId", client.getId().toString());
        List<String> selectAliases = List.of("clientName");

        // when
        QueryExecutionResult result = withManager(() ->
            aiJpqlQueryService.executeJpqlQuery(jpqlQuery, JpqlParameters.fromMap(parameters), selectAliases, 0, 5)
        );

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0)).containsEntry("clientName", clientName);
    }

    @Test
    void shouldExecuteQueryWithClientIdParameterAsUuid() {
        // given
        String clientName = "TEST-CLIENT-UUID-" + UUID.randomUUID();
        Client client = systemAuthenticator.withSystem(() -> entities.client(clientName));
        String jpqlQuery = "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId";
        Map<String, Object> parameters = Map.of("clientId", client.getId()); // UUID object directly
        List<String> selectAliases = List.of("clientName");

        // when
        QueryExecutionResult result = withManager(() ->
            aiJpqlQueryService.executeJpqlQuery(jpqlQuery, JpqlParameters.fromMap(parameters), selectAliases, 0, 5)
        );

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0)).containsEntry("clientName", clientName);
    }
}