package com.company.crm.ai.tool;

import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.report.run.AiReportExecutionService;
import com.company.crm.report.CategoryCashflowRiskReport;
import com.company.crm.report.Client360Report;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.view.ViewRegistry;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Registers the CRM-specific AI tools as {@link io.jmix.aitools.tool.JmixAiTool} beans
 * so they are picked up by {@code AiToolRegistry}.
 */
@Configuration
public class CrmAiToolsConfig {

    /**
     * Reports the AI assistant is allowed to discover and run. Formerly held in CrmAnalyticsService.
     */
    static final List<String> ALLOWED_REPORT_CODES = List.of(
            Client360Report.CODE,
            CategoryCashflowRiskReport.CODE
    );

    @Bean
    public ViewsDiscoveryTool viewsDiscoveryTool(ServerProperties serverProperties,
                                                 ViewRegistry viewRegistry,
                                                 MetadataTools metadataTools) {
        return new ViewsDiscoveryTool(serverProperties, viewRegistry, metadataTools);
    }

    @Bean
    public ReportsDiscoveryTool reportsDiscoveryTool(AiReportModelDescriptorYamlExporter reportYamlExporter,
                                                     AiToolStatusPublisher toolStatusPublisher) {
        return new ReportsDiscoveryTool(reportYamlExporter, toolStatusPublisher, ALLOWED_REPORT_CODES);
    }

    @Bean
    public RunReportTool runReportTool(AiReportExecutionService executionService,
                                       AiToolStatusPublisher toolStatusPublisher) {
        return new RunReportTool(executionService, toolStatusPublisher, ALLOWED_REPORT_CODES);
    }
}
