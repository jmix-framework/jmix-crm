package com.company.crm.ai.jmix.query;

import com.company.crm.AbstractTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Test to verify that OpenAI can handle List<JpqlParameter> correctly
 */
class OpenAiParameterListTest extends AbstractTest {

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
        System.out.println("LLM Response: " + response);
    }

    /**
     * Test tool to verify List<JpqlParameter> works with OpenAI
     */
    public static class ParameterListTool {

        private static final Logger log = LoggerFactory.getLogger(ParameterListTool.class);

        @Tool(description = "Test tool to verify parameter list handling")
        public String testParameterList(
                @ToolParam(description = "List of parameters") List<JpqlParameter> parameters) {

            log.info("=== PARAMETER LIST TOOL CALLED ===");
            log.info("parameters: '{}' (type: {})", parameters, parameters != null ? parameters.getClass().getSimpleName() : "null");

            if (parameters != null) {
                log.info("parameters size: {}", parameters.size());
                for (int i = 0; i < parameters.size(); i++) {
                    JpqlParameter param = parameters.get(i);
                    log.info("  parameters[{}] = name:'{}', value:'{}' (value type: {})",
                            i, param.parameterName(), param.parameterValue(),
                            param.parameterValue() != null ? param.parameterValue().getClass().getSimpleName() : "null");
                }
            }

            log.info("=== END PARAMETER LIST TOOL ===");

            return String.format("Received %d parameters: %s",
                                parameters != null ? parameters.size() : 0, parameters);
        }
    }
}