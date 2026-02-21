package com.company.crm.ai.jmix.report.introspection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI Tool for discovering available Jmix reports and their parameters.
 * Provides a comprehensive YAML representation of reports, templates, and input parameters.
 */
public class JmixReportDiscoveryTool {

    private static final Logger log = LoggerFactory.getLogger(JmixReportDiscoveryTool.class);

    private final AiReportModelDescriptorYamlExporter yamlExporter;
    private final List<String> whitelist;

    /**
     * Creates a report discovery tool with an optional whitelist of authorized report codes.
     */
    public JmixReportDiscoveryTool(AiReportModelDescriptorYamlExporter yamlExporter, List<String> whitelist) {
        this.yamlExporter = yamlExporter;
        this.whitelist = whitelist != null ? List.copyOf(whitelist) : Collections.emptyList();
    }

    /**
     * Get available reports and their structure in YAML format.
     *
     * @return YAML string containing available reports, their templates, and input parameters.
     */
    @Tool(description = """
        Discover all available business reports in the system.

        This tool provides a structured YAML list of all reports you can run.
        For each report, it provides:
        - Report Code (unique ID)
        - Name, Group, Description
        - Available Templates
        - Input Parameters (aliases, types, required flags)

        CRITICAL: Always call this tool first before using any report execution tools.
        """)
    public String getAvailableReports() {
        log.info("LLM Tool Call: getAvailableReports()");
        try {
            return whitelist.isEmpty() ? yamlExporter.export() : yamlExporter.export(whitelist);
        } catch (Exception e) {
            log.error("Failed to discover reports", e);
            return "Error: Failed to discover reports: " + e.getMessage();
        }
    }

    /**
     * Get structure for specific reports by their codes in YAML format.
     *
     * @param reportCodes List of report codes to include.
     * @return YAML string containing requested reports.
     */
    @Tool(description = "Get detailed structure for specific reports by their unique codes.")
    public String getReportsByCodes(
            @ToolParam(description = "List of report codes to include (e.g., [\"client-360-report\"])")
            List<String> reportCodes) {
        log.info("LLM Tool Call: getReportsByCodes({})", reportCodes);
        try {
            List<String> authorizedCodes = whitelist.isEmpty() ? reportCodes : 
                    reportCodes.stream().filter(whitelist::contains).collect(Collectors.toList());

            if (authorizedCodes.isEmpty() && !reportCodes.isEmpty()) {
                return "Error: None of the requested report codes are authorized.";
            }

            return yamlExporter.export(authorizedCodes);
        } catch (Exception e) {
            log.error("Failed to discover reports", e);
            return "Error: Failed to discover requested reports: " + e.getMessage();
        }
    }
}
