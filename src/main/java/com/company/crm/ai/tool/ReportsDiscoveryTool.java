package com.company.crm.ai.tool;

import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring AI Tool for discovering available Jmix reports and their parameters.
 * Provides a comprehensive YAML representation of reports, templates, and input parameters.
 */
public class ReportsDiscoveryTool implements CrmAiTool {

    private static final Logger log = LoggerFactory.getLogger(ReportsDiscoveryTool.class);

    private final AiReportModelDescriptorYamlExporter yamlExporter;
    private final AiToolStatusPublisher toolStatusPublisher;
    private final Collection<String> allowedReportCodes;

    public static ReportsDiscoveryTool create(ApplicationContext applicationContext, Collection<String> whitelist) {
        return new ReportsDiscoveryTool(
                applicationContext.getBean(AiReportModelDescriptorYamlExporter.class),
                applicationContext.getBean(AiToolStatusPublisher.class),
                whitelist);
    }

    ReportsDiscoveryTool(AiReportModelDescriptorYamlExporter yamlExporter,
                         AiToolStatusPublisher toolStatusPublisher,
                         Collection<String> allowedReportCodes) {
        this.yamlExporter = yamlExporter;
        this.toolStatusPublisher = toolStatusPublisher;
        this.allowedReportCodes = allowedReportCodes != null ? List.copyOf(allowedReportCodes) : Collections.emptyList();
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
    public String getAvailableReports(ToolContext toolContext) {
        String statusStart = "Looking up available business reports...";
        log.info("LLM Tool Call: getAvailableReports()");
        toolStatusPublisher.update(toolContext, statusStart);
        try {
            String yaml = allowedReportCodes.isEmpty() ? yamlExporter.export() : yamlExporter.export(allowedReportCodes);
            String text = allowedReportCodes.isEmpty()
                    ? "Discovered available business reports"
                    : String.format("Found %d authorized business report(s)", allowedReportCodes.size());
            toolStatusPublisher.complete(toolContext, statusStart, text);
            return yaml;
        } catch (Exception e) {
            log.error("Failed to discover reports", e);
            toolStatusPublisher.complete(toolContext, statusStart, "Failed to lookup available business reports");
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
            List<String> reportCodes,
            ToolContext toolContext) {
        String statusStart = "Reading business report details...";
        log.info("LLM Tool Call: getReportsByCodes({})", reportCodes);
        toolStatusPublisher.update(toolContext, statusStart);
        try {
            List<String> authorizedCodes = allowedReportCodes.isEmpty() ? reportCodes :
                    reportCodes.stream().filter(allowedReportCodes::contains).collect(Collectors.toList());

            if (authorizedCodes.isEmpty() && !reportCodes.isEmpty()) {
                toolStatusPublisher.complete(toolContext, statusStart, "No authorized reports requested");
                return "Error: None of the requested report codes are authorized.";
            }

            String yaml = yamlExporter.export(authorizedCodes);
            toolStatusPublisher.complete(toolContext, statusStart,
                    String.format("Loaded details for %d authorized business report(s)", authorizedCodes.size()));
            return yaml;
        } catch (Exception e) {
            log.error("Failed to discover reports", e);
            toolStatusPublisher.complete(toolContext, statusStart, "Failed to read report details");
            return "Error: Failed to discover requested reports: " + e.getMessage();
        }
    }
}
