package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.model.client.Client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunReportToolIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    @Autowired
    private AiConversationService aiConversationService;

    @Autowired
    private ObjectMapper objectMapper;

    private RunReportTool reportTool;

    @BeforeEach
    @Override
    protected void beforeEach() {
        reportTool = new RunReportTool(executionService, List.of("client-360-report", "unknown-report"));
    }

    @Test
    void testRunReport() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Tool Test Client");
            
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "HTML", null);

            assertThat(result.success()).isTrue();
            assertThat(result.reportCode()).isEqualTo("client-360-report");
            assertThat(result.content()).contains("Client 360° Report");
            assertThat(result.content()).contains("Tool Test Client");
            assertThat(result.content()).doesNotContain("[View Report Attachments](/ai-conversations/");
        });
    }

    @Test
    void testRunReport_withConversationId() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation("Tool Test Persistence");
            Client client = entities.client("Tool Persistence Client");
            
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ToolContext toolContext = new ToolContext(Map.of("conversationId", conversation.getId()));

            ReportExecutionResult result = reportTool.runReport(
                    "client-360-report", 
                    parameters, 
                    null, 
                    "HTML", 
                    toolContext
            );

            assertThat(result.success()).isTrue();
            assertThat(result.content()).contains(
                    String.format("[View Report Attachments](/ai-conversations/%s)", conversation.getId())
            );
        });
    }

    @Test
    void testRunReport_withConversationIdAsString() {
        systemAuthenticator.runWithSystem(() -> {
            AiConversation conversation = aiConversationService.createNewConversation("Tool String Conversation");
            Client client = entities.client("Tool String Context Client");

            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ToolContext toolContext = new ToolContext(Map.of("conversationId", conversation.getId().toString()));

            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "HTML", toolContext);

            assertThat(result.success()).isTrue();
            assertThat(result.content()).contains(
                    String.format("[View Report Attachments](/ai-conversations/%s)", conversation.getId())
            );
        });
    }

    @Test
    void testRunReport_withInvalidConversationIdString() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Tool Invalid Conversation Client");

            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ToolContext toolContext = new ToolContext(Map.of("conversationId", "not-a-uuid"));

            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "HTML", toolContext);

            assertThat(result.success()).isTrue();
            assertThat(result.content()).doesNotContain("[View Report Attachments](/ai-conversations/");
        });
    }

    @Test
    void testRunReportNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            ReportExecutionResult result = reportTool.runReport("unknown-report", Map.of(), null, null, null);
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.REPORT_NOT_FOUND);
        });
    }

    @Test
    void testRunReportErrorCodeContract_serializedAsString() {
        systemAuthenticator.runWithSystem(() -> {
            try {
                Client client = entities.client("Tool Contract Client");
                Map<String, Object> parameters = Map.of(
                        "client", client.getId().toString(),
                        "fromDate", LocalDate.now().minusDays(30).toString(),
                        "toDate", LocalDate.now().toString()
                );

                ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "FOO", null);
                String serialized = objectMapper.writeValueAsString(result);
                JsonNode json = objectMapper.readTree(serialized);

                assertThat(result.success()).isFalse();
                assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
                assertThat(json.path("errorCode").isTextual()).isTrue();
                assertThat(json.path("errorCode").asText()).isEqualTo("INVALID_OUTPUT_TYPE");
                assertThat(serialized).contains("\"errorCode\":\"INVALID_OUTPUT_TYPE\"");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testRunReport_rejectsBinaryOutputType() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Tool Binary Output Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "XLSX", null);

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(result.errorMessage()).contains("CSV");
            assertThat(result.errorMessage()).contains("HTML");
        });
    }

    @Test
    void testRunReport_rejectsBinaryTemplateCode() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Tool Binary Template Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, "XLSX", null, null);

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(result.errorMessage()).contains("Binary templates");
            assertThat(result.errorMessage()).contains("CSV");
        });
    }
}
