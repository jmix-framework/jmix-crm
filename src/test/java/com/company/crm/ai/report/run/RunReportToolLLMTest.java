package com.company.crm.ai.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.report.introspection.JmixReportDiscoveryTool;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.model.client.Client;
import com.company.crm.util.ai.LLMJudge;
import com.company.crm.util.ai.LLMJudgeBuilder;
import io.jmix.core.FetchPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for RunReportTool using a real ChatClient to verify tool orchestration.
 */
@EnabledIfEnvironmentVariable(named = "AI_ENABLED", matches = "true")
class RunReportToolLLMTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    @Autowired
    private AiReportModelDescriptorYamlExporter reportYamlExporter;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private ChatClient.Builder  chatClientBuilder;
    @Autowired
    private LLMJudgeBuilder llmJudgeBuilder;

    private ReportChatAssistant reportChatAssistant;
    private LLMJudge llmJudge;

    @BeforeEach
    @Override
    protected void beforeEach() {
        List<String> allowedReports = List.of("client-360-report");
        this.reportChatAssistant = new ReportChatAssistant(
                chatClientBuilder.clone()
                        .defaultSystem("You are a helpful assistant. Use report tools to answer questions.")
                        .build(),
                new JmixReportDiscoveryTool(reportYamlExporter, allowedReports),
                new RunReportTool(executionService, allowedReports)
        );

        this.llmJudge = llmJudgeBuilder
                .systemPrompt("""
                        You are an LLM Judge.
                        Assess only the final answer quality against the user request and criteria.
                        Do not require the response text to prove tool execution.
                        Treat mention of tool names as optional context, not mandatory evidence.
                        """)
                .judgePrompt("""
                        Question: %s
                        AI Response: %s
                        Criteria: %s

                        Evaluate only the final answer quality against the user request and criteria.
                        Do not require the response text to prove tool execution.
                        Treat tool-name mentions as optional context, not mandatory evidence.
                        Use submitJudgement(correct, reasoning).
                        """)
                .build();
    }

    @Test
    void testDiscoveryAndRunReportFlow() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("LLM Test Client");
            String clientId = client.getId().toString();
            String fromDate = LocalDate.now().minusDays(30).toString();
            String toDate = LocalDate.now().toString();

            String question = """
                    Discover the reports, find the one for client 360 overview, and run it for client ID %s from %s to %s. Summarize the report.
                    """.formatted(clientId, fromDate, toDate);

            // when
            String response = reportChatAssistant.ask(question);

            // then
            assertThat(response).containsIgnoringCase("Client 360");
            assertThat(response).containsIgnoringCase("LLM Test Client");
            assertThat(response).contains("client-360-report");

            llmJudge.evaluateAnwserWithJudge(question, response, """
                    Evaluate only response quality against the user request.
                    Do not require explicit proof of tool execution inside the response text.
                    Tool-name mentions are optional context, not mandatory evidence.
                    """);
        });
    }

    @Test
    void testRunReport_withCitation_inLLMResponse() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            AiConversation conversation = aiConversationService.createNewConversation("LLM Citation Test");
            String conversationId = conversation.getId().toString();
            
            Client client = entities.client("Citation Test Client");
            String clientId = client.getId().toString();
            String fromDate = LocalDate.now().minusDays(30).toString();
            String toDate = LocalDate.now().toString();

            String question = """
                    Run the client-360-report for client ID %s from %s to %s. \
                    In your response, you MUST include the download link for the report provided by the tool.\
                    """.formatted(clientId, fromDate, toDate);

            // when
            String response = reportChatAssistant.ask(question, UUID.fromString(conversationId));

            // then
            assertThat(response).contains(clientId);
            assertThat(response).containsAnyOf(
                    "[View Report Attachments](/ai-conversations/" + conversationId + ")",
                    "/ai-conversations/" + conversationId
            );


            // Verify persistence
            AiConversation reloadedConv = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .fetchPlan(fp -> fp.add("attachments", sub -> sub.addFetchPlan(FetchPlan.BASE)))
                    .one();
            assertThat(reloadedConv.getAttachments()).hasSize(1);
        });
    }

    private static class ReportChatAssistant {
        private static final UUID DEFAULT_CONVERSATION_ID =
                UUID.fromString("00000000-0000-0000-0000-000000000000");

        private final ChatClient chatClient;
        private final Object[] tools;

        private ReportChatAssistant(ChatClient chatClient, JmixReportDiscoveryTool discoveryTool, RunReportTool runReportTool) {
            this.chatClient = chatClient;
            this.tools = new Object[]{discoveryTool, runReportTool};
        }

        private String ask(String question) {
            return ask(question, DEFAULT_CONVERSATION_ID);
        }

        private String ask(String question, UUID conversationId) {
            return chatClient.prompt()
                    .user(question)
                    .toolContext(Map.of("conversationId", conversationId))
                    .tools(tools)
                    .call()
                    .content();
        }
    }
}
