package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.jmix.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.jmix.report.introspection.JmixReportDiscoveryTool;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.model.client.Client;
import com.company.crm.service.ai.LLMJudgeTool;
import io.jmix.core.FetchPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for RunReportTool using a real ChatClient to verify tool orchestration.
 */
@EnabledIfEnvironmentVariable(named = "AI_ENABLED", matches = "true")
class RunReportToolLLMIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    @Autowired
    private AiReportModelDescriptorYamlExporter reportYamlExporter;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private LLMJudgeTool llmJudgeTool;
    private ChatClient judgeClient;

    private JmixReportDiscoveryTool discoveryTool;
    private RunReportTool runReportTool;

    @BeforeEach
    @Override
    protected void beforeEach() {
        List<String> allowedReports = List.of("client-360-report");
        discoveryTool = new JmixReportDiscoveryTool(reportYamlExporter, allowedReports);
        runReportTool = new RunReportTool(executionService, allowedReports);
        
        // Use a fresh builder from the context to avoid state pollution
        this.chatClient = applicationContext.getBean(ChatClient.Builder.class)
                .defaultSystem("You are a helpful assistant. Use report tools to answer questions.")
                .build();

        llmJudgeTool = new LLMJudgeTool();
        this.judgeClient = applicationContext.getBean(ChatClient.Builder.class)
                .defaultSystem("You are an LLM Judge. Evaluate if AI responses correctly answer questions based on the provided data.")
                .defaultTools(llmJudgeTool)
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
                    Discover the reports, find the one for client 360 overview, and run it for client ID %s from %s to %s. Summarize the report. \
                    Important: At the end of your response, explicitly list the technical tool calls you made (e.g., 'Executed: toolName(args)') \
                    so I can verify your workflow.\
                    """.formatted(clientId, fromDate, toDate);

            // when
            String response = chatClient.prompt()
                    .user(question)
                    .toolContext(Map.of("conversationId", java.util.UUID.fromString("00000000-0000-0000-0000-000000000000"))) // Dummy UUID for this test
                    .tools(discoveryTool, runReportTool)
                    .call()
                    .content();

            // then
            assertThat(response).containsIgnoringCase("Client 360");
            assertThat(response).containsIgnoringCase("LLM Test Client");
            assertThat(response).contains("client-360-report");
            assertThat(response).containsIgnoringCase("HTML");
            assertThat(response).contains("getAvailableReports");
            assertThat(response).contains("runReport");


            LLMJudgeTool.JudgeResult evaluation = evaluateWithJudge(question, response, """
                    The AI should have identified client-360-report and called runReport with correct parameters. \
                    The summary should reflect the content of the report. \
                    Verify that the AI reported calling both 'getAvailableReports' and 'runReport' in its technical summary.\
                    """);
            
            assertThat(evaluation).isNotNull();
            assertThat(evaluation.correct()).isTrue();
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
            String response = chatClient.prompt()
                    .user(question)
                    .toolContext(Map.of("conversationId", java.util.UUID.fromString(conversationId)))
                    .tools(discoveryTool, runReportTool)
                    .call()
                    .content();

            // then
            assertThat(response).containsIgnoringCase("Citation Test Client");
            assertThat(response).contains("/ai-conversations/" + conversationId);
            assertThat(response).contains("[View Report Attachments](/ai-conversations/" + conversationId + ")");
            assertThat(response).contains("[View Report Attachments]");


            // Verify persistence
            AiConversation reloadedConv = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .fetchPlan(fp -> fp.add("attachments", sub -> sub.addFetchPlan(FetchPlan.BASE)))
                    .one();
            assertThat(reloadedConv.getAttachments()).hasSize(1);
        });
    }

    @Test
    void testEvaluateWithJudge_whenJudgeReturnsNoResult_throws() throws Exception {
        // given
        LLMJudgeTool emptyJudgeTool = new LLMJudgeTool();
        ChatClient mockedJudgeClient = mock(ChatClient.class);
        ChatClientRequestSpec requestSpec = mock(ChatClientRequestSpec.class);
        CallResponseSpec responseSpec = mock(CallResponseSpec.class);
        when(mockedJudgeClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("No judgement submitted");

        this.llmJudgeTool = emptyJudgeTool;
        this.judgeClient = mockedJudgeClient;

        Method evaluateWithJudge = RunReportToolLLMIntegrationTest.class
                .getDeclaredMethod("evaluateWithJudge", String.class, String.class, String.class);
        evaluateWithJudge.setAccessible(true);

        // when / then
        assertThatThrownBy(() -> evaluateWithJudge.invoke(this, "Q", "A", "C"))
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Judge did not return a result");
    }

    private LLMJudgeTool.JudgeResult evaluateWithJudge(String question, String aiResponse, String criteria) {
        String judgePrompt = """
                Question: %s
                AI Response: %s
                Criteria: %s
                
                Verify if the AI followed the correct workflow and provided a meaningful summary of the report content.
                Use submitJudgement(correct, reasoning).
                """.formatted(question, aiResponse, criteria);

        judgeClient.prompt(judgePrompt).call().content();
        LLMJudgeTool.JudgeResult result = llmJudgeTool.getLastResult();
        if (result == null) {
            throw new IllegalStateException("Judge did not return a result");
        }
        return result;
    }
}
