package com.company.crm.test.ai.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.report.run.ReportExecutionErrorCode;
import com.company.crm.ai.report.run.ReportExecutionResult;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.ai.tool.RunReportTool;
import com.company.crm.model.client.Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.FetchPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunReportToolIntegrationTest extends AbstractTest {

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private ObjectMapper objectMapper;

    private RunReportTool reportTool;

    @BeforeEach
    @Override
    protected void beforeEach() {
        reportTool = RunReportTool.create(applicationContext, List.of("client-360-report", "unknown-report"));
    }

    @Test
    void testRunReport() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Tool Test Client");

            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "HTML", null);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.reportCode()).isEqualTo("client-360-report");
            assertThat(result.content()).contains("Client 360° Report");
            assertThat(result.content()).contains("Tool Test Client");
            assertThat(result.content()).doesNotContain("[View Report Attachments](/ai-conversations/");
        });
    }

    @ParameterizedTest(name = "assistantMessageId as String: {0}")
    @ValueSource(booleans = {false, true})
    void testRunReport_withAssistantMessageId(boolean asString) {
        systemAuthenticator.runWithSystem(() -> {
            // given
            AiConversation conversation = aiConversationService.createNewConversation();
            ChatMessage message = dataManager.create(ChatMessage.class);
            message.setConversation(conversation);
            message.setType(ChatMessageType.ASSISTANT);
            message.setCreatedDate(LocalDate.now().atStartOfDay().atOffset(java.time.ZoneOffset.UTC));
            dataManager.save(message);

            Client client = entities.client(
                    asString ? "Tool String Context Client" : "Tool Persistence Client");

            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            Object assistantMessageIdParam = asString ? message.getId().toString() : message.getId();
            ToolContext toolContext = new ToolContext(Map.of("assistantMessageId", assistantMessageIdParam));

            // when
            ReportExecutionResult result = reportTool.runReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    toolContext
            );

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.content()).contains(
                    String.format("[View Report Attachments](/ai-conversations/%s)", conversation.getId())
            );

            AiConversation reloadedConversation = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .fetchPlan(fp -> fp.add("messages", sub -> sub.add("attachments", attSub -> attSub.addFetchPlan(FetchPlan.BASE))))
                    .one();
            List<AiConversationAttachment> attachments = reloadedConversation.getMessages().stream()
                    .flatMap(m -> m.getAttachments().stream())
                    .toList();
            assertThat(attachments).hasSize(1);
            AiConversationAttachment attachment = attachments.getFirst();
            assertThat(attachment.getFileName()).startsWith("report_client-360-report_");
            assertThat(attachment.getFileName()).endsWith(".html");
            assertThat(attachment.getFile()).isNotNull();
        });
    }

    @Test
    void testRunReport_withInvalidConversationIdString() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Tool Invalid Conversation Client");

            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ToolContext toolContext = new ToolContext(Map.of("conversationId", "not-a-uuid"));

            // when
            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "HTML", toolContext);

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.content()).doesNotContain("[View Report Attachments](/ai-conversations/");
        });
    }

    @Test
    void testRunReportNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            // when
            ReportExecutionResult result = reportTool.runReport("unknown-report", Map.of(), null, null, null);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.REPORT_NOT_FOUND);
        });
    }

    @Test
    void testRunReportErrorCodeContract_serializedAsString() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Tool Contract Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "FOO", null);
            JsonNode json = objectMapper.valueToTree(result);
            String serialized = json.toString();

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(json.path("errorCode").isTextual()).isTrue();
            assertThat(json.path("errorCode").asText()).isEqualTo("INVALID_OUTPUT_TYPE");
            assertThat(serialized).contains("\"errorCode\":\"INVALID_OUTPUT_TYPE\"");
        });
    }

    @Test
    void testRunReport_rejectsBinaryOutputType() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Tool Binary Output Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "XLSX", null);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(result.errorMessage()).contains("CSV");
            assertThat(result.errorMessage()).contains("HTML");
        });
    }

    @Test
    void testRunReport_rejectsBinaryTemplateCode() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Tool Binary Template Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, "XLSX", null, null);

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(result.errorMessage()).contains("Binary templates");
            assertThat(result.errorMessage()).contains("CSV");
        });
    }
}
