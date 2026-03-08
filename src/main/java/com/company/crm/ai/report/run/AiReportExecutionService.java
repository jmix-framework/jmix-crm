package com.company.crm.ai.report.run;

import com.company.crm.ai.model.AiAttachmentType;
import com.company.crm.ai.model.AiConversation;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import com.company.crm.ai.model.ChatMessageType;
import io.jmix.core.DataManager;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.Messages;
import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.runner.ReportRunner;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Service for executing Jmix reports on behalf of AI tools.
 */
@Service
public class AiReportExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiReportExecutionService.class);
    private static final String DEFAULT_ASSISTANT_NAME = "CRM AI";

    private final ReportRepository reportRepository;
    private final ReportRunner reportRunner;
    private final AiReportParameterConverter parameterConverter;
    private final ReportContentConverter contentConverter;
    private final FileStorage fileStorage;
    private final DataManager dataManager;
    private final Messages messages;

    public AiReportExecutionService(ReportRepository reportRepository,
                                   ReportRunner reportRunner,
                                   AiReportParameterConverter parameterConverter,
                                   ReportContentConverter contentConverter,
                                   FileStorage fileStorage,
                                   DataManager dataManager,
                                   Messages messages) {
        this.reportRepository = reportRepository;
        this.reportRunner = reportRunner;
        this.parameterConverter = parameterConverter;
        this.contentConverter = contentConverter;
        this.fileStorage = fileStorage;
        this.dataManager = dataManager;
        this.messages = messages;
    }

    /**
     * Executes a report by its code with provided parameters.
     *
     * @param reportCode          Unique code of the report to run
     * @param parameters          Input parameters provided by LLM
     * @param templateCode        Optional template code. If null, default template is used.
     * @param outputType          Optional output type override.
     * @param allowedReportCodes Mandatory whitelist of allowed report codes.
     * @return Execution result with content or error details
     */
    public ReportExecutionResult executeReport(String reportCode, Map<String, Object> parameters, String templateCode, String outputType, Collection<String> allowedReportCodes) {
        return executeReport(reportCode, parameters, templateCode, outputType, allowedReportCodes, null);
    }

    /**
     * Executes a report by its code with provided parameters and persists result if conversationId is provided.
     *
     * @param reportCode          Unique code of the report to run
     * @param parameters          Input parameters provided by LLM
     * @param templateCode        Optional template code. If null, default template is used.
     * @param outputType          Optional output type override.
     * @param allowedReportCodes Mandatory whitelist of allowed report codes.
     * @param conversationId      Optional AI conversation ID to link the report to.
     * @return Execution result with content or error details
     */
    public ReportExecutionResult executeReport(String reportCode, Map<String, Object> parameters, String templateCode, String outputType, Collection<String> allowedReportCodes, UUID conversationId) {
        try {
            // 0. Mandatory Whitelist Guard
            if (allowedReportCodes == null || !allowedReportCodes.contains(reportCode)) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.ACCESS_DENIED, "Report execution is not allowed for this report code. Ensure it is whitelisted.");
            }

            // 1. Load report
            Report report = reportRepository.getAllReports().stream()
                    .filter(r -> reportCode.equals(r.getCode()))
                    .findFirst()
                    .orElse(null);

            if (report == null) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.REPORT_NOT_FOUND, "Report with code '" + reportCode + "' not found.");
            }

            // Reload to get all details (parameters, templates)
            report = reportRepository.reloadForRunning(report);

            // 3. Resolve Template
            ReportTemplate template = resolveTemplate(report, templateCode);
            if (templateCode != null && template == null) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.TEMPLATE_NOT_FOUND, "Template with code '" + templateCode + "' not found for this report.");
            }
            if (templateCode == null && outputType != null) {
                ReportTemplate matchingOutputTemplate = resolveTemplateByOutputType(report, outputType);
                if (matchingOutputTemplate != null) {
                    template = matchingOutputTemplate;
                }
            }

            String effectiveTemplateCode = template != null ? template.getCode() : null;
            String effectiveOutputType = outputType != null ? outputType : (template != null && template.getReportOutputType() != null ? template.getReportOutputType().toString() : null);

            // 4. Convert and Validate Parameters
            ReportParameterConversionResult conversionResult = parameterConverter.convertParameters(report.getInputParameters(), parameters);
            if (!conversionResult.success()) {
                if (conversionResult.hasConversionErrors()) {
                    return ReportExecutionResult.parameterConversionError(reportCode, conversionResult.errors());
                }
                return ReportExecutionResult.validationError(reportCode, conversionResult.errors());
            }

            // 5. Run Report
            var runner = reportRunner.byReportEntity(report)
                    .withParams(conversionResult.convertedParameters());

            if (effectiveTemplateCode != null) {
                runner.withTemplateCode(effectiveTemplateCode);
            }
            if (outputType != null) {
                // Note: ReportRunner.withOutputType expects io.jmix.reports.entity.ReportOutputType enum
                try {
                    runner.withOutputType(io.jmix.reports.entity.ReportOutputType.valueOf(outputType.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.INVALID_OUTPUT_TYPE, "Output type '" + outputType + "' is not supported.");
                }
            }

            ReportOutputDocument document = runner.run();

            // 6. Convert Output to Text
            ReportContentResult convertedContent = contentConverter.convert(document, effectiveOutputType);
            if (convertedContent instanceof ReportContentResult.BinaryUnsupported) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.BINARY_OUTPUT_NOT_SUPPORTED_YET, "Binary output formats (like PDF, XLSX) are not yet supported for LLM analysis.");
            }
            String content = ((ReportContentResult.TextContent) convertedContent).content();

            ReportExecutionResult result = ReportExecutionResult.success(reportCode, effectiveTemplateCode, effectiveOutputType, content);

            // 7. Optional Persistence
            if (conversationId != null) {
                return persistReportResult(result, conversationId, report.getName());
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to execute report {}", reportCode, e);
            return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.EXECUTION_ERROR, "An unexpected error occurred during report execution: " + e.getMessage());
        }
    }

    private ReportExecutionResult persistReportResult(ReportExecutionResult result, UUID conversationId, String reportName) {
        FileRef fileRef = null;
        try {
            AiConversation conversation = dataManager.load(AiConversation.class).id(conversationId).optional().orElse(null);
            if (conversation == null) {
                log.warn("Cannot persist report result: AiConversation with ID {} not found", conversationId);
                return result;
            }

            String extension = "HTML".equalsIgnoreCase(result.outputType()) ? "html" : ("CSV".equalsIgnoreCase(result.outputType()) ? "csv" : "txt");
            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("report_%s_%s.%s", result.reportCode(), timestamp, extension);

            String content = result.content() != null ? result.content() : "";
            fileRef = fileStorage.saveStream(fileName, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

            AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
            attachment.setConversation(conversation);
            attachment.setFile(fileRef);
            attachment.setFileName(fileName);
            String attachmentTitle = reportName != null && !reportName.isBlank() ? reportName : result.reportCode();
            attachment.setTitle(attachmentTitle);
            attachment.setType(AiAttachmentType.AI_GENERATED);
            dataManager.save(attachment);
            saveAttachmentEventMessage(conversation, attachmentTitle);

            String citation = String.format("\n\n[View Report Attachments](/ai-conversations/%s)", conversation.getId());
            return new ReportExecutionResult(
                    result.success(),
                    result.reportCode(),
                    result.templateCodeUsed(),
                    result.outputType(),
                    result.content() + citation,
                    result.errorCode(),
                    result.errorMessage(),
                    result.validationErrors()
            );
        } catch (Exception e) {
            if (fileRef != null) {
                try {
                    fileStorage.removeFile(fileRef);
                } catch (Exception cleanupError) {
                    log.warn("Failed to cleanup report file {} after persistence error", fileRef, cleanupError);
                }
            }
            log.error("Failed to persist report result for conversation {}", conversationId, e);
            return result;
        }
    }

    private void saveAttachmentEventMessage(AiConversation conversation, String attachmentTitle) {
        try {
            ChatMessage attachmentMessage = dataManager.create(ChatMessage.class);
            attachmentMessage.setConversation(conversation);
            attachmentMessage.setType(ChatMessageType.ATTACHMENT);
            attachmentMessage.setContent(String.format("%s added attachment \"%s\"",
                    assistantName(), attachmentTitle));
            dataManager.save(attachmentMessage);
        } catch (Exception e) {
            log.warn("Failed to persist attachment event message for conversation {}", conversation.getId(), e);
        }
    }

    private String assistantName() {
        try {
            String value = messages.getMessage("com.company.crm.ai.view.aiconversation/assistantName");
            return value != null && !value.isBlank() ? value : DEFAULT_ASSISTANT_NAME;
        } catch (Exception e) {
            return DEFAULT_ASSISTANT_NAME;
        }
    }

    private ReportTemplate resolveTemplate(Report report, String templateCode) {
        if (templateCode == null) {
            return report.getDefaultTemplate();
        }
        return report.getTemplates().stream()
                .filter(t -> templateCode.equals(t.getCode()))
                .findFirst()
                .orElse(null);
    }

    private ReportTemplate resolveTemplateByOutputType(Report report, String outputType) {
        if (outputType == null) {
            return null;
        }
        return report.getTemplates().stream()
                .filter(template -> template.getReportOutputType() != null
                        && template.getReportOutputType().name().equalsIgnoreCase(outputType))
                .findFirst()
                .orElse(null);
    }
}
