package com.company.crm.ai.report.run;

import com.company.crm.ai.model.AiAttachmentOrigin;
import com.company.crm.ai.model.AiConversationAttachment;
import com.company.crm.ai.model.ChatMessage;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FileRef;
import io.jmix.core.FileStorage;
import io.jmix.core.Metadata;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import io.jmix.reports.entity.ReportTemplate;
import io.jmix.reports.runner.ReportRunner;
import io.jmix.reports.yarg.reporting.ReportOutputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static io.jmix.reports.entity.ReportOutputType.valueOf;

/**
 * Service for executing Jmix reports on behalf of AI tools.
 */
@Service
public class AiReportExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AiReportExecutionService.class);

    private final DataManager dataManager;
    private final FileStorage fileStorage;
    private final ReportRunner reportRunner;
    private final ReportRepository reportRepository;
    private final ReportContentConverter contentConverter;
    private final AiReportParameterConverter parameterConverter;
    private final Metadata metadata;
    private final MetadataTools metadataTools;

    public AiReportExecutionService(ReportRepository reportRepository,
                                    ReportRunner reportRunner,
                                    AiReportParameterConverter parameterConverter,
                                    ReportContentConverter contentConverter,
                                    FileStorage fileStorage,
                                    DataManager dataManager,
                                    Metadata metadata,
                                    MetadataTools metadataTools) {
        this.reportRepository = reportRepository;
        this.reportRunner = reportRunner;
        this.parameterConverter = parameterConverter;
        this.contentConverter = contentConverter;
        this.fileStorage = fileStorage;
        this.dataManager = dataManager;
        this.metadata = metadata;
        this.metadataTools = metadataTools;
    }

    /**
     * Executes a report by its code with provided parameters.
     *
     * @param reportCode         Unique code of the report to run
     * @param parameters         Input parameters provided by LLM
     * @param templateCode       Optional template code. If null, default template is used.
     * @param outputType         Optional output type override.
     * @param allowedReportCodes Mandatory whitelist of allowed report codes.
     * @return Execution result with content or error details
     */
    public ReportExecutionResult executeReport(String reportCode, Map<String, Object> parameters, String templateCode, String outputType, Collection<String> allowedReportCodes) {
        return executeReport(reportCode, parameters, templateCode, outputType, allowedReportCodes, null);
    }

    /**
     * Executes a report by its code with provided parameters and persists result if conversationId is provided.
     *
     * @param reportCode         Unique code of the report to run
     * @param parameters         Input parameters provided by LLM
     * @param templateCode       Optional template code. If null, default template is used.
     * @param outputType         Optional output type override.
     * @param allowedReportCodes Mandatory whitelist of allowed report codes.
     * @return Execution result with content or error details
     */
    public ReportExecutionResult executeReport(String reportCode, Map<String, Object> parameters, String templateCode, String outputType, Collection<String> allowedReportCodes, UUID assistantMessageId) {
        try {
            // 0. Mandatory Whitelist Guard
            if (allowedReportCodes == null || !allowedReportCodes.contains(reportCode)) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.ACCESS_DENIED, "Report execution is not allowed for this report code. Ensure it is whitelisted.");
            }

            // 1. Load report
            Report report = findReport(reportCode);
            if (report == null) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.REPORT_NOT_FOUND, "Report with code '" + reportCode + "' not found.");
            }

            // Reload to get all details (parameters, templates)
            Report runnableReport = reportRepository.reloadForRunning(report);

            // 3. Resolve Template
            ReportTemplate template = resolveTemplate(runnableReport, templateCode);
            if (templateCode != null && template == null) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.TEMPLATE_NOT_FOUND, "Template with code '" + templateCode + "' not found for this report.");
            }
            if (templateCode == null && outputType != null) {
                ReportTemplate matchingOutputTemplate = resolveTemplateByOutputType(runnableReport, outputType);
                if (matchingOutputTemplate != null) {
                    template = matchingOutputTemplate;
                }
            }

            String effectiveTemplateCode = template != null ? template.getCode() : null;
            String effectiveOutputType = outputType != null ? outputType : (template != null && template.getReportOutputType() != null ? template.getReportOutputType().toString() : null);

            // 4. Convert and Validate Parameters
            ReportParameterConversionResult conversionResult = parameterConverter.convertParameters(runnableReport.getInputParameters(), parameters);
            if (!conversionResult.success()) {
                if (conversionResult.hasConversionErrors()) {
                    return ReportExecutionResult.parameterConversionError(reportCode, conversionResult.errors());
                }
                return ReportExecutionResult.validationError(reportCode, conversionResult.errors());
            }

            // 5. Run Report
            if (outputType != null) {
                try {
                    valueOf(outputType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.INVALID_OUTPUT_TYPE, "Output type '" + outputType + "' is not supported.");
                }
            }

            if (outputType != null && !contentConverter.isTextOutput(outputType)) {
                return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.BINARY_OUTPUT_NOT_SUPPORTED_YET, "Binary output formats (like PDF, XLSX) are not yet supported for LLM analysis.");
            }

            var runner = reportRunner.byReportEntity(runnableReport)
                    .withParams(conversionResult.convertedParameters());

            if (effectiveTemplateCode != null) {
                runner.withTemplateCode(effectiveTemplateCode);
            }
            if (outputType != null) {
                runner.withOutputType(valueOf(outputType.toUpperCase()));
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
            if (assistantMessageId != null) {
                String primaryEntityName = resolvePrimaryEntityInstanceName(conversionResult.convertedParameters());
                return persistReportResult(result, assistantMessageId, runnableReport.getName(), primaryEntityName);
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to execute report {}", reportCode, e);
            return ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.EXECUTION_ERROR, "An unexpected error occurred during report execution: " + e.getMessage());
        }
    }

    private Report findReport(String reportCode) {
        return reportRepository.getAllReports().stream()
                .filter(report -> reportCode.equals(report.getCode()))
                .findFirst()
                .orElse(null);
    }

    private ReportExecutionResult persistReportResult(ReportExecutionResult result, UUID assistantMessageId, String reportName, String primaryEntityName) {
        try {
            ChatMessage assistantMessage = dataManager.load(ChatMessage.class)
                    .id(assistantMessageId)
                    .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                            .add("conversation", FetchPlan.BASE))
                    .optional()
                    .orElse(null);
            if (assistantMessage == null) {
                log.warn("Cannot persist report result: ChatMessage with ID {} not found", assistantMessageId);
                return result;
            }

            String extension = "HTML".equalsIgnoreCase(result.outputType()) ? "html" : ("CSV".equalsIgnoreCase(result.outputType()) ? "csv" : "txt");
            LocalDateTime now = LocalDateTime.now();
            String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = String.format("report_%s_%s.%s", result.reportCode(), timestamp, extension);

            String content = result.content() != null ? result.content() : "";
            FileRef fileRef = fileStorage.saveStream(fileName, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

            try {
                AiConversationAttachment attachment = dataManager.create(AiConversationAttachment.class);
                attachment.setMessage(assistantMessage);
                attachment.setFile(fileRef);
                attachment.setFileName(fileName);
                attachment.setTitle(reportAttachmentTitle(result.reportCode(), reportName, primaryEntityName));
                attachment.setOrigin(AiAttachmentOrigin.AI_GENERATED);
                dataManager.save(attachment);
            } catch (Exception e) {
                cleanupReportFile(fileRef);
                throw e;
            }

            String citation = String.format("\n\n[View Report Attachments](/ai-conversations/%s)", assistantMessage.getConversation().getId());
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
            log.error("Failed to persist report result for assistant message {}", assistantMessageId, e);
            return result;
        }
    }

    private String reportAttachmentTitle(String reportCode, String reportName, String primaryEntityName) {
        String attachmentTitle = StringUtils.hasText(reportName) ? reportName : reportCode;
        if (StringUtils.hasText(primaryEntityName)) {
            return attachmentTitle + " - " + primaryEntityName;
        }
        return attachmentTitle;
    }

    private void cleanupReportFile(FileRef fileRef) {
        try {
            fileStorage.removeFile(fileRef);
        } catch (Exception cleanupError) {
            log.warn("Failed to cleanup report file {} after persistence error", fileRef, cleanupError);
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

    private String resolvePrimaryEntityInstanceName(Map<String, Object> convertedParameters) {
        return convertedParameters.values().stream()
                .filter(Objects::nonNull)
                .filter(value -> metadata.findClass(value.getClass()) != null)
                .map(value -> {
                    try {
                        return metadataTools.getInstanceName(value);
                    } catch (Exception e) {
                        log.debug("Could not resolve instance name for parameter value {}", value, e);
                        return null;
                    }
                })
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }
}
