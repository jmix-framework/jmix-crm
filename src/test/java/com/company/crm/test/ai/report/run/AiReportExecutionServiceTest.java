package com.company.crm.test.ai.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import com.company.crm.ai.report.run.AiReportExecutionService;
import com.company.crm.ai.report.run.ReportExecutionErrorCode;
import com.company.crm.ai.report.run.ReportExecutionResult;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportExecutionServiceTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    @Autowired
    private FileStorage fileStorage;

    @Autowired
    private AiConversationService aiConversationService;

    @Test
    void testExecuteClient360Report() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Integration Test Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport("client-360-report", parameters, null, "HTML", List.of("client-360-report"));

            // then
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
    void testExecuteReport_withAssistantMessageId_persistsAttachment() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            AiConversation conversation = aiConversationService.createNewConversation();
            ChatMessage message = dataManager.create(ChatMessage.class);
            message.setConversation(conversation);
            message.setType(ChatMessageType.ASSISTANT);
            message.setCreatedDate(OffsetDateTime.now());
            dataManager.save(message);

            Client client = entities.client("Persistence Test Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report"),
                    message.getId()
            );

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.content()).contains(
                    String.format("[View Report Attachments](/ai-conversations/%s)", conversation.getId())
            );

            AiConversation reloadedConv = dataManager.load(AiConversation.class)
                    .id(conversation.getId())
                    .fetchPlan(fp -> fp.add("messages", sub -> sub.add("attachments", attSub -> attSub.addFetchPlan(FetchPlan.BASE))))
                    .one();

            List<AiConversationAttachment> attachments = reloadedConv.getMessages().stream()
                    .flatMap(m -> m.getAttachments().stream())
                    .toList();

            assertThat(attachments).hasSize(1);
            AiConversationAttachment attachment = attachments.getFirst();
            assertThat(attachment.getFileName()).startsWith("report_client-360-report_");
            assertThat(attachment.getFileName()).endsWith(".html");
            assertThat(attachment.getTitle()).isEqualTo("Client 360 Report - Persistence Test Client");
            assertThat(attachment.getOrigin()).isEqualTo(AiAttachmentOrigin.AI_GENERATED);

            assertThat(attachment.getFile()).isNotNull();
            assertThat(fileStorage.fileExists(attachment.getFile())).isTrue();
        });
    }

    @Test
    void testExecuteReport_withUnknownAssistantMessageId_doesNotPersistAttachment() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            long attachmentsBefore = dataManager
                    .loadValue("select count(a) from AiConversationAttachment a", Long.class)
                    .one();

            Client client = entities.client("Unknown Message Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report"),
                    UUID.randomUUID()
            );

            // then
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
            // given

            // when
            ReportExecutionResult result = executionService.executeReport("non-existent-report", Map.of(), null, null, List.of("non-existent-report"));

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.REPORT_NOT_FOUND);
        });
    }

    @Test
    void testAccessDenied() {
        systemAuthenticator.runWithSystem(() -> {
            // given

            // when
            ReportExecutionResult result = executionService.executeReport("client-360-report", Map.of(), null, null, List.of("some-other-report"));

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.ACCESS_DENIED);
            assertThat(result.errorMessage()).contains("Ensure it is whitelisted");
        });
    }

    @Test
    void testTemplateNotFound_returnsTemplateNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Template Not Found Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    "does-not-exist",
                    "HTML",
                    List.of("client-360-report")
            );

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.TEMPLATE_NOT_FOUND);
            assertThat(result.errorMessage()).contains("does-not-exist");
            assertThat(result.content()).isNull();
        });
    }

    @Test
    void testInvalidOutputType_returnsInvalidOutputType() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Invalid Output Type Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "FOO",
                    List.of("client-360-report")
            );

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.INVALID_OUTPUT_TYPE);
            assertThat(result.errorMessage()).contains("FOO");
        });
    }

    @Test
    void testMissingRequiredParameter_returnsValidationError() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Map<String, Object> parameters = Map.of(
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            // then
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
            // given
            Map<String, Object> parameters = Map.of(
                    "client", "not-a-uuid",
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            // then
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
            // given
            Client client = entities.client("Invalid Date Format Client");
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", "not-a-date",
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            // then
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
            // given
            Map<String, Object> parameters = Map.of(
                    "clientX", UUID.randomUUID().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    parameters,
                    null,
                    "HTML",
                    List.of("client-360-report")
            );

            // then
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
            // given
            Client client = entities.client("Binary Output Client");
            Order order = entities.order(client, LocalDate.now(), OrderStatus.NEW);
            Invoice invoice = entities.invoice(client, order);

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "invoice-report",
                    Map.of("invoice", invoice.getId().toString()),
                    null,
                    "PDF",
                    List.of("invoice-report")
            );

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.BINARY_OUTPUT_NOT_SUPPORTED_YET);
            assertThat(result.content()).isNull();
        });
    }

    @Test
    void testNullWhitelist_returnsAccessDenied() {
        systemAuthenticator.runWithSystem(() -> {
            // given

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "client-360-report",
                    Map.of(),
                    null,
                    null,
                    null
            );

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.ACCESS_DENIED);
        });
    }

    @Test
    void testExecuteCategoryCashflowRiskReport_withCsvOutput() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Risk CSV Client");
            Category category = entities.category("Risk CSV Category", "RCSV");
            CategoryItem item = entities.categoryItem("Risk CSV Item", "RCSV-I", category, java.math.BigDecimal.valueOf(1000), UomType.PIECES);
            Order order = entities.order(client, LocalDate.now().minusDays(15), OrderStatus.DONE);
            var orderItem = entities.orderItem(order, item, java.math.BigDecimal.ONE);
            order.setOrderItems(List.of(orderItem));
            order.setTotal(java.math.BigDecimal.valueOf(1000));
            order = dataManager.save(order);
            entities.invoice(client, order, java.math.BigDecimal.valueOf(1000), InvoiceStatus.PENDING, LocalDate.now().minusDays(10));

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "category-cashflow-risk-report",
                    validCategoryCashflowReportParameters(client),
                    null,
                    "CSV",
                    List.of("category-cashflow-risk-report")
            );

            // then
            assertThat(result.success()).isTrue();
            assertThat(result.outputType()).isEqualTo("CSV");
            assertThat(result.content()).contains("Category");
            assertThat(result.content()).contains("Risk CSV Category");
        });
    }

    @Test
    void testExecuteCategoryCashflowRiskReport_withXlsxOutput_returnsBinaryNotSupported() {
        systemAuthenticator.runWithSystem(() -> {
            // given
            Client client = entities.client("Risk XLSX Client");

            // when
            ReportExecutionResult result = executionService.executeReport(
                    "category-cashflow-risk-report",
                    validCategoryCashflowReportParameters(client),
                    null,
                    "XLSX",
                    List.of("category-cashflow-risk-report")
            );

            // then
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.BINARY_OUTPUT_NOT_SUPPORTED_YET);
            assertThat(result.content()).isNull();
        });
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
