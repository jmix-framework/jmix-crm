package com.company.crm.ai.jmix.report.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Collection;
import java.util.Map;

/**
 * Spring AI Tool for executing Jmix reports and retrieving their text output.
 */
public class RunReportTool {

    private static final Logger log = LoggerFactory.getLogger(RunReportTool.class);

    private final AiReportExecutionService executionService;
    private final Collection<String> allowedReportCodes;

    public RunReportTool(AiReportExecutionService executionService, Collection<String> allowedReportCodes) {
        this.executionService = executionService;
        this.allowedReportCodes = allowedReportCodes;
    }

    @Tool(description = """
        Execute a report by report code and return the full raw text output for LLM analysis.

        MANDATORY WORKFLOW:
        1. Call the report discovery tool (methods `getAvailableReports` or `getReportsByCodes`) first to inspect available reports, templates, and input parameters.
        2. Identify the `alias` for each required parameter in the discovery output.
        3. Provide correctly typed parameter values in a JSON map where keys are the exact parameter aliases.
        4. Run this tool only after report code and parameter requirements (aliases and types) are fully known.

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
        - Never use parameter captions/names as keys. Use aliases only.
        - If discovery and execution constraints conflict, return the tool error.
        """)
    public ReportExecutionResult runReport(
            @ToolParam(description = "Exact report code from discovery, e.g. 'client-360-report'") String reportCode,
            @ToolParam(description = "Input parameters map keyed by exact parameter aliases (not captions/names)") Map<String, Object> parameters,
            @ToolParam(description = "Optional template code. If null, default report template is used") String templateCode,
            @ToolParam(description = "Optional output type override, e.g. 'HTML', 'CSV', 'PDF'") String outputType
    ) {
        log.info("LLM Tool Call: runReport(reportCode='{}', templateCode='{}', outputType='{}')", reportCode, templateCode, outputType);
        try {
            return executionService.executeReport(reportCode, parameters, templateCode, outputType, allowedReportCodes);
        } catch (Exception e) {
            log.error("Report Tool Error: {} - {}", reportCode, e.getMessage());
            return ReportExecutionResult.failed(reportCode, "EXECUTION_ERROR", "Error executing report tool: " + e.getMessage());
        }
    }
}
