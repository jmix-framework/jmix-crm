package com.company.crm.service.ai;

import com.company.crm.AbstractTest;
import com.company.crm.ai.jmix.query.AiJpqlQueryService;
import com.company.crm.ai.jmix.query.JpqlQueryTool;
import com.company.crm.model.client.Client;
import com.company.crm.security.role.UiMinimalRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrmAnalyticsServicePermissionsLLMTest extends AbstractTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;
    @Autowired
    private AiJpqlQueryService aiJpqlQueryService;
    @Autowired
    private ObjectMapper objectMapper;

    private ChatClient chatClient;
    private JpqlQueryTool jpqlQueryTool;

    @BeforeEach
    void setUp() {
        // given
        this.jpqlQueryTool = new JpqlQueryTool(aiJpqlQueryService);
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        You are a deterministic integration-test assistant.
                        You must call executeQuery exactly once with the arguments provided by the user.
                        Then return only one valid JSON object with keys:
                        success, errorMessage, firstValue.
                        Do not return markdown and do not add extra text.
                        """)
                .build();
    }

    @Test
    void managerCanReadClientThroughJpqlToolCallback() {
        // given
        String expectedClientName = "LLM-AUTH-" + UUID.randomUUID();
        Client client = systemAuthenticator.withSystem(() -> entities.client(expectedClientName));

        // when
        QueryProbeResult result = withManager(() -> executeQueryWithLlm(
                "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId",
                Map.of("clientId", client.getId().toString()),
                List.of("clientName")
        ));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.firstValue()).isEqualTo(expectedClientName);
        assertThat(result.errorMessage()).isEmpty();
    }

    @Test
    void uiMinimalCannotReadClientThroughJpqlToolCallback() {
        // given
        String expectedClientName = "LLM-UI-MINIMAL-" + UUID.randomUUID();
        Client client = systemAuthenticator.withSystem(() -> entities.client(expectedClientName));
        var uiMinimalUser = testUsers.ensureUser("ui-minimal-ai-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        testUsers.assignRole(uiMinimalUser.getUsername(), UiMinimalRole.CODE);

        // when
        QueryProbeResult result = withUser(uiMinimalUser, () -> executeQueryWithLlm(
                "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId",
                Map.of("clientId", client.getId().toString()),
                List.of("clientName")
        ));

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.firstValue()).isEmpty();
        assertThat(result.errorMessage())
                .contains("resource: Client")
                .contains("type: entity")
                .contains("action: read");
    }

    @Test
    void adminCanReadClientThroughJpqlToolCallback() {
        // given
        String expectedClientName = "LLM-ADMIN-" + UUID.randomUUID();
        Client client = systemAuthenticator.withSystem(() -> entities.client(expectedClientName));

        // when
        QueryProbeResult result = withUser("admin", () -> executeQueryWithLlm(
                "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId",
                Map.of("clientId", client.getId().toString()),
                List.of("clientName")
        ));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.firstValue()).isEqualTo(expectedClientName);
        assertThat(result.errorMessage()).isEmpty();
    }

    private QueryProbeResult executeQueryWithLlm(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases) {
        try {
            String prompt = """
                    Run executeQuery with the following arguments:
                    jpqlQuery: %s
                    parameters: %s
                    selectAliases: %s
                    offset: 0
                    limit: 5
                    Return JSON only with keys success,errorMessage,firstValue.
                    """.formatted(
                    jpqlQuery,
                    objectMapper.writeValueAsString(parameters),
                    objectMapper.writeValueAsString(selectAliases)
            );

            String rawResponse = chatClient.prompt()
                    .user(prompt)
                    .tools(jpqlQueryTool)
                    .call()
                    .content();

            String json = extractJsonObject(rawResponse);
            JsonNode node = objectMapper.readTree(json);
            return new QueryProbeResult(
                    node.path("success").asBoolean(false),
                    textOrEmpty(node, "errorMessage"),
                    textOrEmpty(node, "firstValue"),
                    rawResponse
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to execute JPQL query through LLM callback", e);
        }
    }

    private String textOrEmpty(JsonNode node, String key) {
        JsonNode child = node.path(key);
        if (child.isMissingNode() || child.isNull()) {
            return "";
        }
        return child.asText("");
    }

    private String extractJsonObject(String rawResponse) {
        if (rawResponse == null) {
            throw new IllegalStateException("LLM returned null response");
        }
        int firstBrace = rawResponse.indexOf('{');
        int lastBrace = rawResponse.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < firstBrace) {
            throw new IllegalStateException("LLM response does not contain JSON object: " + rawResponse);
        }
        return rawResponse.substring(firstBrace, lastBrace + 1);
    }

    private record QueryProbeResult(boolean success, String errorMessage, String firstValue, String rawResponse) {
    }
}
