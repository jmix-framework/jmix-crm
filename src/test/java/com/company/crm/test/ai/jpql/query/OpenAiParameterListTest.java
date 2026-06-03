package com.company.crm.test.ai.jpql.query;

import com.company.crm.AbstractAiTest;
import com.company.crm.ai.jpql.query.JpqlParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test to verify that OpenAI can handle List<JpqlParameter> correctly
 */
class OpenAiParameterListTest extends AbstractAiTest {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private ParameterListTool parameterListTool;

    @BeforeEach
    void setUp() {
        parameterListTool = new ParameterListTool();
        chatClient = chatClientBuilder
                .defaultSystem("You are a test assistant. Call the test tool with the parameters provided.")
                .build();
    }

    @Test
    void testParameterListPassing() {
        // given
        String prompt = """
                Call the testParameterList tool with these parameters:
                - parameters: [
                    {"parameterName": "clientId", "parameterValue": "550e8400-e29b-41d4-a716-446655440000"},
                    {"parameterName": "status", "parameterValue": "active"}
                  ]
                """;

        // when
        String response = chatClient.prompt()
                .user(prompt)
                .tools(parameterListTool)
                .call()
                .content();

        // then
        assertThat(response).isNotBlank();
        assertThat(parameterListTool.getParameters())
                .extracting(JpqlParameter::parameterName, JpqlParameter::parameterValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("clientId", "550e8400-e29b-41d4-a716-446655440000"),
                        org.assertj.core.groups.Tuple.tuple("status", "active")
                );
    }

    /**
     * Test tool to verify List<JpqlParameter> works with OpenAI
     */
    public static class ParameterListTool {

        private List<JpqlParameter> parameters = List.of();

        @Tool(description = "Test tool to verify parameter list handling")
        public String testParameterList(
                @ToolParam(description = "List of parameters") List<JpqlParameter> parameters) {
            this.parameters = parameters != null ? List.copyOf(parameters) : List.of();
            return String.format("Received %d parameters: %s",
                    this.parameters.size(), this.parameters);
        }

        private List<JpqlParameter> getParameters() {
            return parameters;
        }
    }
}
