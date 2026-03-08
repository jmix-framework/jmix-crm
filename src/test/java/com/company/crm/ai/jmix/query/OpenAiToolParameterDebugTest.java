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
import java.util.Map;

/**
 * Test to debug how OpenAI handles Map<String, Object> parameters in tool calls
 */
class OpenAiToolParameterDebugTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(OpenAiToolParameterDebugTest.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private DebugTool debugTool;

    @BeforeEach
    void setUp() {
        debugTool = new DebugTool();
        chatClient = chatClientBuilder
                .defaultSystem("You are a test assistant. Call the debug tool with the exact parameters provided.")
                .build();
    }

    @Test
    void testMapParameterPassing() {
        // given
        String prompt = """
                Call the debugTool with these exact parameters:
                - testString: "hello"
                - testMap: {"key1": "value1", "key2": "value2"}
                - testList: ["item1", "item2"]
                """;

        // when
        String response = chatClient.prompt()
                .user(prompt)
                .tools(debugTool)
                .call()
                .content();

        // then
        System.out.println("LLM Response: " + response);
    }

    @Test
    void testJsonStyleParameterPassing() {
        // given
        String prompt = """
                Call the debugTool with this JSON payload:
                {
                  "testString": "hello-json",
                  "testMap": {"clientId": "123-uuid-456", "status": "active"},
                  "testList": ["client", "order"]
                }
                """;

        // when
        String response = chatClient.prompt()
                .user(prompt)
                .tools(debugTool)
                .call()
                .content();

        // then
        System.out.println("LLM Response: " + response);
    }

    @Test
    void testComplexMapParameterPassing() {
        // given
        String prompt = """
                Call the debugTool and pass testMap with exactly these key-value pairs:
                - "clientId": "550e8400-e29b-41d4-a716-446655440000"
                - "amount": "1500.50"
                - "active": true
                """;

        // when
        String response = chatClient.prompt()
                .user(prompt)
                .tools(debugTool)
                .call()
                .content();

        // then
        System.out.println("LLM Response: " + response);
    }

    /**
     * Simple debug tool to see what parameters OpenAI actually passes
     */
    public static class DebugTool {

        private static final Logger log = LoggerFactory.getLogger(DebugTool.class);

        @Tool(description = "Debug tool to inspect parameter types and values")
        public String debugTool(
                @ToolParam(description = "Test string parameter") String testString,
                @ToolParam(description = "Test map parameter") Map<String, Object> testMap,
                @ToolParam(description = "Test list parameter") List<String> testList) {

            log.info("=== DEBUG TOOL CALLED ===");
            log.info("testString: '{}' (type: {})", testString, testString != null ? testString.getClass().getSimpleName() : "null");
            log.info("testMap: '{}' (type: {})", testMap, testMap != null ? testMap.getClass().getSimpleName() : "null");
            log.info("testList: '{}' (type: {})", testList, testList != null ? testList.getClass().getSimpleName() : "null");

            if (testMap != null) {
                log.info("testMap size: {}", testMap.size());
                testMap.forEach((key, value) ->
                    log.info("  testMap['{}'] = '{}' (type: {})", key, value,
                            value != null ? value.getClass().getSimpleName() : "null")
                );
            }

            if (testList != null) {
                log.info("testList size: {}", testList.size());
                for (int i = 0; i < testList.size(); i++) {
                    log.info("  testList[{}] = '{}'", i, testList.get(i));
                }
            }

            log.info("=== END DEBUG TOOL ===");

            return String.format("Received: testString=%s, testMap=%s, testList=%s",
                                testString, testMap, testList);
        }
    }
}