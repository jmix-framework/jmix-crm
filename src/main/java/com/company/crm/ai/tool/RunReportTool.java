package com.company.crm.ai.tool;

import com.company.crm.ai.report.run.AiReportExecutionService;
import com.company.crm.ai.report.run.ReportExecutionErrorCode;
import com.company.crm.ai.report.run.ReportExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Spring AI Tool for executing Jmix reports and retrieving their text output.
 */
public class RunReportTool implements CrmAiTool {

    private static final Logger log = LoggerFactory.getLogger(RunReportTool.class);
    private static final Set<String> ALLOWED_OUTPUT_TYPES = Set.of("HTML", "CSV");
    private static final Set<String> BLOCKED_TEMPLATE_CODES = Set.of("DOCX", "XLSX", "PDF");

    private final AiReportExecutionService executionService;
    private final AiToolStatusPublisher toolStatusPublisher;
    private final Collection<String> allowedReportCodes;

    public static RunReportTool create(ApplicationContext applicationContext,
                                       Collection<String> allowedReportCodes) {
        return new RunReportTool(
                applicationContext.getBean(AiReportExecutionService.class),
                applicationContext.getBean(AiToolStatusPublisher.class),
                allowedReportCodes);
    }

    public RunReportTool(AiReportExecutionService executionService,
                         AiToolStatusPublisher toolStatusPublisher,
                         Collection<String> allowedReportCodes) {
        this.executionService = executionService;
        this.toolStatusPublisher = toolStatusPublisher;
        this.allowedReportCodes = allowedReportCodes;
    }

    @Tool(description = """
            Execute a report by report code and return the full raw text output for LLM analysis.
            
            MANDATORY WORKFLOW:
            1. Call the report discovery tool (methods `getAvailableReports` or `getReportsByCodes`) first to inspect available reports, templates, and input parameters.
            2. Identify the `alias` for each required parameter in the discovery output.
            3. Provide correctly typed parameter values in a JSON map where keys are the exact parameter aliases.
            4. Choose a text output when available; prefer CSV if the report supports it.
            5. Run this tool only after report code and parameter requirements (aliases and types) are fully known.
            
            INPUT CONTRACT:
            - reportCode: Required. Exact report code from discovery (e.g., 'client-360-report').
            - parameters: Required map. Keys MUST match report input parameter aliases exactly.
            - parameters MUST be passed by alias, not by parameter display name/caption.
            - templateCode: Optional. If not provided, the report default template is used.
            - outputType: Optional. If not provided, the template's default output type is used.
            
            PARAMETER FORMATS:
            - For ENTITY parameters: Provide the UUID string of the entity.
            - For DATE values: Use ISO format YYYY-MM-DD.
            - For TIME values: Use ISO format HH:mm:ss.
            - For DATETIME values: Use ISO format YYYY-MM-DDTHH:mm:ss.
            - For BOOLEAN: Use true/false.
            - For NUMERIC: Use standard numeric format (e.g., 100.50).
            
            OUTPUT BEHAVIOR:
            - For text-based report outputs (e.g. HTML, CSV, JSON), this tool returns the full raw text in result.content.
            - No snippet truncation is applied; full text is returned for analysis.
            - Binary outputs (e.g. PDF, XLSX) are currently not returned as raw content and will return a BINARY_OUTPUT_NOT_SUPPORTED_YET error.
            - If CSV is available for a report, use CSV for AI analysis instead of XLSX/PDF.
            
            ERROR HANDLING:
            The tool returns structured failure results with machine-readable error codes:
            - ACCESS_DENIED
            - REPORT_NOT_FOUND
            - TEMPLATE_NOT_FOUND
            - INVALID_OUTPUT_TYPE
            - PARAMETER_VALIDATION_ERROR (e.g., missing required alias, unknown alias)
            - PARAMETER_CONVERSION_ERROR (e.g., invalid UUID, date format, or numeric format)
            - BINARY_OUTPUT_NOT_SUPPORTED_YET
            - EXECUTION_ERROR
            
            STRICTNESS:
            - Do not invent report codes, parameter aliases, or template codes.
            - Do not choose binary output types (XLSX/PDF) for AI analysis when CSV is available.
            - Never use parameter captions/names as keys. Use aliases only.
            - If discovery and execution constraints conflict, return the tool error.
            """)
    public ReportExecutionResult runReport(
            @ToolParam(description = "Exact report code from discovery, e.g. 'client-360-report'") String reportCode,
            @ToolParam(description = "Input parameters map keyed by exact parameter aliases (not captions/names)") Map<String, Object> parameters,
            @ToolParam(description = "Optional template code. If null, default report template is used") String templateCode,
            @ToolParam(description = "Optional output type override, prefer text types like 'CSV' or 'HTML'; avoid binary types like 'XLSX'/'PDF' for AI analysis") String outputType,
            ToolContext toolContext
    ) {
        String statusStart = "Running business report...";
        log.info("LLM Tool Call: runReport(reportCode='{}', templateCode='{}', outputType='{}')", reportCode, templateCode, outputType);
        toolStatusPublisher.update(toolContext, statusStart);
        try {
            ReportExecutionResult formatValidationError = validateRequestedFormat(reportCode, templateCode, outputType);
            if (formatValidationError != null) {
                publishCompletion(toolContext, statusStart, reportCode, formatValidationError);
                return formatValidationError;
            }

            UUID assistantMessageId = resolveUuid(toolContext, "assistantMessageId");
            ReportExecutionResult result = executionService.executeReport(reportCode, parameters, templateCode, outputType, allowedReportCodes, assistantMessageId);
            publishCompletion(toolContext, statusStart, reportCode, result);
            return result;
        } catch (Exception e) {
            log.error("Report Tool Error: {} - {}", reportCode, e.getMessage());
            ReportExecutionResult failed = ReportExecutionResult.failed(reportCode, ReportExecutionErrorCode.EXECUTION_ERROR, "Error executing report tool: " + e.getMessage());
            publishCompletion(toolContext, statusStart, reportCode, failed);
            return failed;
        }
    }

    private void publishCompletion(ToolContext toolContext, String statusStart, String reportCode,
                                   ReportExecutionResult result) {
        String humanReportName = reportCode.replace("-report", "").replace("-", " ");
        if (result.success()) {
            String statusText = String.format("Successfully executed report '%s'", humanReportName);
            toolStatusPublisher.complete(toolContext, statusStart, statusText);
        } else {
            String statusText = String.format("Failed to execute report '%s': %s", humanReportName, result.errorMessage());
            toolStatusPublisher.complete(toolContext, statusStart, statusText);
        }
    }

    private ReportExecutionResult validateRequestedFormat(String reportCode, String templateCode, String outputType) {
        if (outputType != null) {
            String normalizedOutputType = outputType.trim().toUpperCase();
            if (!ALLOWED_OUTPUT_TYPES.contains(normalizedOutputType)) {
                return ReportExecutionResult.failed(
                        reportCode,
                        ReportExecutionErrorCode.INVALID_OUTPUT_TYPE,
                        "runReport supports only text output types at the moment. Use outputType='CSV' or 'HTML'."
                );
            }
        }

        if (templateCode != null) {
            String normalizedTemplateCode = templateCode.trim().toUpperCase();
            if (BLOCKED_TEMPLATE_CODES.contains(normalizedTemplateCode)) {
                return ReportExecutionResult.failed(
                        reportCode,
                        ReportExecutionErrorCode.INVALID_OUTPUT_TYPE,
                        "Binary templates are not supported by runReport for AI analysis. Use outputType='CSV' or 'HTML'."
                );
            }
        }

        return null;
    }

    private UUID resolveUuid(ToolContext toolContext, String key) {
        if (toolContext == null || !toolContext.getContext().containsKey(key)) {
            return null;
        }

        Object rawValue = toolContext.getContext().get(key);
        if (rawValue instanceof UUID uuid) {
            return uuid;
        }

        if (rawValue instanceof String uuidString) {
            try {
                return UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring invalid {} in tool context: {}", key, uuidString);
                return null;
            }
        }

        log.warn("Ignoring unsupported {} type in tool context: {}", key, rawValue.getClass().getName());
        return null;
    }
}
