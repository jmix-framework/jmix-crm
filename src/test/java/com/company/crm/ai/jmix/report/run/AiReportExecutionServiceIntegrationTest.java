package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.ai.entity.AiAttachmentType;
import com.company.crm.ai.entity.AiConversation;
import com.company.crm.ai.entity.AiConversationAttachment;
import com.company.crm.ai.service.AiConversationService;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportExecutionServiceIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private AiConversationService aiConversationService;

    @Test
    void testExecuteClient360Report() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Integration Test Client");
            
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = executionService.executeReport("client-360-report", parameters, null, "HTML", List.of("client-360-report"));

            if (!result.success()) {
                System.out.println("DEBUG: Report failed. Code: " + result.errorCode() + ", Message: " + result.errorMessage());
                if (result.validationErrors() != null) {
                    result.validationErrors().forEach(e -> System.out.println("DEBUG: Validation error: " + e.parameterAlias() + " - " + e.errorMessage()));
                }
            }

            assertThat(result.success())
                    .withFailMessage("Report execution failed: " + result.errorMessage() + " (Error code: " + result.errorCode() + ")")
                    .isTrue();
            assertThat(result.reportCode()).isEqualTo("client-360-report");
            assertThat(result.outputType()).isEqualTo("HTML");
            assertThat(result.content()).contains("Client 360° Report");
            assertThat(result.content()).contains("Integration Test Client");
        });
    }

    @Test
    void testExecuteReport_withConversationId_persistsAttachment() {
        systemAuthenticator.runWithSystem(() -> {
            // 1. Setup conversation
            AiConversation conversation = aiConversationService.createNewConversation("Test Persistence");
            
            // 2. Setup client
            Client client = entities.client("Persistence Test Client");
            Map<String, Object> parameters = validClientReportParameters(client);

            // 3. Execute report with conversationId
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report", 
                    parameters, 
                    null, 
                    "HTML", 
                    List.of("client-360-report"),
                    conversation.getId()
            );

            // 4. Verify result
            assertThat(result.success()).isTrue();
            assertThat(result.content()).contains(
                    String.format("[View Report Attachments](/ai-conversations/%s)", conversation.getId())
            );

            // 5. Verify persistence in DB
            AiConversation reloadedConv = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .fetchPlan(fp -> fp.add("attachments", sub -> sub.addFetchPlan(FetchPlan.BASE)))
                    .one();
            
            assertThat(reloadedConv.getAttachments()).hasSize(1);
            AiConversationAttachment attachment = reloadedConv.getAttachments().get(0);
            assertThat(attachment.getFileName()).startsWith("report_client-360-report_");
            assertThat(attachment.getFileName()).endsWith(".html");
            assertThat(attachment.getTitle()).isEqualTo("Client 360 Report");
            assertThat(attachment.getType()).isEqualTo(AiAttachmentType.AI_GENERATED);
            
            // 6. Verify file existence in storage
            assertThat(attachment.getFile()).isNotNull();
            assertThat(fileStorage.fileExists(attachment.getFile())).isTrue();
        });
    }

    @Test
    void testExecuteReport_withUnknownConversationId_doesNotPersistAttachment() {
        systemAuthenticator.runWithSystem(() -> {
            long attachmentsBefore = dataManager
                    .loadValue("select count(a) from AiConversationAttachment a", Long.class)
                    .one();

            Client client = entities.client("Unknown Conversation Client");
            Map<String, Object> parameters = validClientReportParameters(client);

            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report"),
                    UUID.randomUUID()
            );

            assertThat(result.success()).isTrue();
            assertThat(result.content()).doesNotContain("[View Report Attachments](/ai-conversations/");

            long attachmentsAfter = dataManager
                    .loadValue("select count(a) from AiConversationAttachment a", Long.class)
                    .one();
            assertThat(attachmentsAfter).isEqualTo(attachmentsBefore);
        });
    }

    @Test
    void testReportNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            ReportExecutionResult result = executionService.executeReport("non-existent-report", Map.of(), null, null, List.of("non-existent-report"));
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.REPORT_NOT_FOUND);
        });
    }

    @Test
    void testAccessDenied() {
        systemAuthenticator.runWithSystem(() -> {
            // Report is NOT in the whitelist
            ReportExecutionResult result = executionService.executeReport("client-360-report", Map.of(), null, null, List.of("some-other-report"));
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.ACCESS_DENIED);
            assertThat(result.errorMessage()).contains("Ensure it is whitelisted");
        });
    }

    @Test
    void testTemplateNotFound_returnsTemplateNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Template Not Found Client");

            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    validClientReportParameters(client),
                    "does-not-exist",
                    "HTML",
                    List.of("client-360-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.TEMPLATE_NOT_FOUND);
            assertThat(result.errorMessage()).contains("does-not-exist");
            assertThat(result.content()).isNull();
        });
    }

    @Test
    void testInvalidOutputType_returnsInvalidOutputType() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Invalid Output Type Client");

            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    validClientReportParameters(client),
                    null,
                    "FOO",
                    List.of("client-360-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(result.errorMessage()).contains("FOO");
        });
    }

    @Test
    void testMissingRequiredParameter_returnsValidationError() {
        systemAuthenticator.runWithSystem(() -> {
            Map<String, Object> parameters = Map.of(
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.PARAMETER_VALIDATION_ERROR);
            assertThat(result.validationErrors())
                    .anyMatch(error -> "client".equals(error.parameterAlias())
                            && error.errorMessage().contains("Required parameter is missing"));
        });
    }

    @Test
    void testInvalidEntityId_returnsParameterConversionError() {
        systemAuthenticator.runWithSystem(() -> {
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    validClientReportParametersWithClientId("not-a-uuid"),
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.PARAMETER_CONVERSION_ERROR);
            assertThat(result.validationErrors())
                    .anyMatch(error -> "client".equals(error.parameterAlias())
                            && error.errorMessage().contains("Conversion failed"));
        });
    }

    @Test
    void testInvalidDateFormat_returnsParameterConversionError() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Invalid Date Format Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", "01.01.2026",
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.PARAMETER_CONVERSION_ERROR);
            assertThat(result.validationErrors())
                    .anyMatch(error -> "fromDate".equals(error.parameterAlias())
                            && error.errorMessage().contains("Conversion failed"));
        });
    }

    @Test
    void testUnknownAliasInService_returnsValidationError() {
        systemAuthenticator.runWithSystem(() -> {
            Map<String, Object> parameters = Map.of(
                    "clientX", UUID.randomUUID().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.PARAMETER_VALIDATION_ERROR);
            assertThat(result.validationErrors())
                    .anyMatch(error -> "clientX".equals(error.parameterAlias())
                            && error.errorMessage().contains("Unknown report parameter alias"));
        });
    }

    @Test
    void testBinaryOutput_returnsBinaryNotSupported() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Binary Output Client");
            Order order = entities.order(client, LocalDate.now(), OrderStatus.NEW);
            Invoice invoice = entities.invoice(client, order);

            ReportExecutionResult result = executionService.executeReport(
                    "invoice-report",
                    Map.of("invoice", invoice.getId().toString()),
                    null,
                    "PDF",
                    List.of("invoice-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.BINARY_OUTPUT_NOT_SUPPORTED_YET);
            assertThat(result.content()).isNull();
        });
    }

    @Test
    void testNullWhitelist_returnsAccessDenied() {
        systemAuthenticator.runWithSystem(() -> {
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    Map.of(),
                    null,
                    null,
                    null
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.ACCESS_DENIED);
        });
    }

    @Test
    void testExecuteCategoryCashflowRiskReport_withCsvOutput() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Risk CSV Client");
            Category category = entities.category("Risk CSV Category", "RCSV");
            CategoryItem item = entities.categoryItem("Risk CSV Item", "RCSV-I", category, java.math.BigDecimal.valueOf(1000), UomType.PIECES);
            Order order = entities.order(client, LocalDate.now().minusDays(15), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, java.math.BigDecimal.ONE);
            order.setOrderItems(List.of(orderItem));
            order.setTotal(java.math.BigDecimal.valueOf(1000));
            order = dataManager.save(order);
            entities.invoice(client, order, java.math.BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now().minusDays(10));

            ReportExecutionResult result = executionService.executeReport(
                    "category-cashflow-risk-report",
                    validCategoryCashflowReportParameters(client),
                    null,
                    "CSV",
                    List.of("category-cashflow-risk-report")
            );

            assertThat(result.success()).isTrue();
            assertThat(result.outputType()).isEqualTo("CSV");
            assertThat(result.content()).contains("Category");
            assertThat(result.content()).contains("Risk CSV Category");
        });
    }

    @Test
    void testExecuteCategoryCashflowRiskReport_withXlsxOutput_returnsBinaryNotSupported() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Risk XLSX Client");

            ReportExecutionResult result = executionService.executeReport(
                    "category-cashflow-risk-report",
                    validCategoryCashflowReportParameters(client),
                    null,
                    "XLSX",
                    List.of("category-cashflow-risk-report")
            );

            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.BINARY_OUTPUT_NOT_SUPPORTED_YET);
            assertThat(result.content()).isNull();
        });
    }

    private Map<String, Object> validClientReportParameters(Client client) {
        return validClientReportParametersWithClientId(client.getId().toString());
    }

    private Map<String, Object> validClientReportParametersWithClientId(String clientId) {
        return Map.of(
                "client", clientId,
                "fromDate", LocalDate.now().minusDays(30).toString(),
                "toDate", LocalDate.now().toString()
        );
    }

    private Map<String, Object> validCategoryCashflowReportParameters(Client client) {
        return Map.of(
                "client", client.getId().toString(),
                "fromDate", LocalDate.now().minusDays(30).toString(),
                "toDate", LocalDate.now().toString(),
                "asOfDate", LocalDate.now().toString(),
                "includePaid", true
        );
    }
}
