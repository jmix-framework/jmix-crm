package com.company.crm.test.ai.service;

import com.company.crm.AbstractAiTest;
import com.company.crm.ai.tool.JpqlExecutorTool;
import com.company.crm.model.client.Client;
import com.company.crm.security.role.UiMinimalRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrmAnalyticsServicePermissionsLLMTest extends AbstractAiTest {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private JpqlQueryProbe queryProbe;

    @BeforeEach
    void setUp() {
        this.queryProbe = new JpqlQueryProbe(
                chatClientBuilder.clone()
                        .defaultSystem("""
                                You are a deterministic integration-test assistant.
                                You must call executeQuery exactly once with the arguments provided by the user.
                                After executeQuery returns, you must call submitQueryProbeResult exactly once.
                                Do not rewrite or normalize jpqlQuery.
                                Do not rename, remove, or add parameter keys.
                                Use the provided parameters map exactly as-is.
                                Never pass parameters as null.
                                If the provided map contains clientId, you MUST pass clientId in executeQuery parameters.
                                For submitQueryProbeResult:
                                - success must match executeQuery.success
                                - errorMessage must match executeQuery.errorMessage, or empty string if none
                                - firstValue must be the first row's value for the first select alias, converted to string, or empty string if unavailable
                                Do not add extra text outside the required tool calls.
                                """)
                        .build(),
                JpqlExecutorTool.create(applicationContext),
                objectMapper
        );
    }

    @Test
    void managerCanReadClientThroughJpqlToolCallback() {
        // given
        String expectedClientName = "LLM-AUTH-" + UUID.randomUUID();
        Client client = systemAuthenticator.withSystem(() -> entities.client(expectedClientName));

        // when
        QueryProbeResult result = withManager(() -> queryProbe.execute(
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
        QueryProbeResult result = withUser(uiMinimalUser, () -> queryProbe.execute(
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
        QueryProbeResult result = withUser("admin", () -> queryProbe.execute(
                "SELECT c.name AS clientName FROM Client c WHERE c.id = :clientId",
                Map.of("clientId", client.getId().toString()),
                List.of("clientName")
        ));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.firstValue()).isEqualTo(expectedClientName);
        assertThat(result.errorMessage()).isEmpty();
    }

    private record QueryProbeResult(boolean success, String errorMessage, String firstValue) {
    }

    private record JpqlQueryProbe(ChatClient chatClient, JpqlExecutorTool jpqlExecutorTool, ObjectMapper objectMapper) {

        private QueryProbeResult execute(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases) {
            try {
                QueryProbeCollectorTool collectorTool = new QueryProbeCollectorTool();
                String prompt = buildExecuteQueryPrompt(jpqlQuery, parameters, selectAliases);

                chatClient.prompt()
                        .user(prompt)
                        .tools(jpqlExecutorTool, collectorTool)
                        .toolContext(Map.of("testProbe", true))
                        .call()
                        .content();

                QueryProbeResult result = collectorTool.getResult();
                if (result == null) {
                    return new QueryProbeResult(false, "Query probe did not submit result", "");
                }
                return result;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to execute JPQL query through LLM callback", e);
            }
        }

        private String buildExecuteQueryPrompt(String jpqlQuery, Map<String, Object> parameters, List<String> selectAliases) throws Exception {
            return """
                    Call executeQuery exactly once with these exact arguments:
                    - jpqlQuery: %s
                    - parameters: %s
                    - selectAliases: %s
                    - offset: 0
                    - limit: 5
                    
                    Rules for executeQuery:
                    - Keep jpqlQuery byte-for-byte unchanged.
                    - Keep parameter keys unchanged (e.g. clientId stays clientId).
                    - Do not add or remove parameters.
                    - parameters must not be null.
                    - If parameters contains clientId, pass clientId exactly as provided.
                    
                    After executeQuery returns, call submitQueryProbeResult exactly once with:
                    - success from executeQuery.success
                    - errorMessage from executeQuery.errorMessage, or empty string if none
                    - firstValue from the first returned row under the first select alias, converted to string, or empty string if unavailable
                    
                    Do not produce any plain-text answer.
                    """.formatted(
                    objectMapper.writeValueAsString(jpqlQuery),
                    objectMapper.writeValueAsString(parameters),
                    objectMapper.writeValueAsString(selectAliases)
            );
        }
    }

    private static class QueryProbeCollectorTool {
        private QueryProbeResult result;

        @Tool(name = "submitQueryProbeResult", description = """
                Submit the final executeQuery outcome for this test callback.
                Use success from executeQuery.success.
                Use errorMessage from executeQuery.errorMessage, or empty string if none.
                Use firstValue from the first returned row under the first select alias, converted to string, or empty string if unavailable.
                """)
        public void submitQueryProbeResult(
                @ToolParam(description = "Whether executeQuery completed successfully") boolean success,
                @ToolParam(description = "The executeQuery error message, or empty string if none") String errorMessage,
                @ToolParam(description = "The first row's value for the first selected alias, or empty string if unavailable") String firstValue) {
            this.result = new QueryProbeResult(success, sanitize(errorMessage), sanitize(firstValue));
        }

        private QueryProbeResult getResult() {
            return result;
        }

        private String sanitize(String value) {
            if (value == null) {
                return "";
            }
            return value.replaceAll("[\r\n\t]", " ").trim();
        }
    }
}
