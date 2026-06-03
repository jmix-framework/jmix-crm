package com.company.crm.ai.tool;

import com.company.crm.ai.context.AiContextEntityDefinition;
import com.company.crm.ai.jpql.introspection.exporter.AiDomainModelDescriptorYamlExporter;
import com.company.crm.ai.jpql.query.AiJpqlQueryService;
import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.report.run.AiReportExecutionService;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.view.ViewRegistry;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;

@Component
public class CrmAiToolFactory {

    private final MetadataTools metadataTools;
    private final AiDomainModelDescriptorYamlExporter yamlExporter;
    private final AiJpqlQueryService aiJpqlQueryService;
    private final ServerProperties serverProperties;
    private final ViewRegistry viewRegistry;
    private final AiReportModelDescriptorYamlExporter reportYamlExporter;
    private final AiReportExecutionService executionService;
    private final AiToolStatusPublisher toolStatusPublisher;

    public CrmAiToolFactory(MetadataTools metadataTools,
                            AiDomainModelDescriptorYamlExporter yamlExporter,
                            AiJpqlQueryService aiJpqlQueryService,
                            ServerProperties serverProperties,
                            ViewRegistry viewRegistry,
                            AiReportModelDescriptorYamlExporter reportYamlExporter,
                            AiReportExecutionService executionService,
                            AiToolStatusPublisher toolStatusPublisher) {
        this.metadataTools = metadataTools;
        this.yamlExporter = yamlExporter;
        this.aiJpqlQueryService = aiJpqlQueryService;
        this.serverProperties = serverProperties;
        this.viewRegistry = viewRegistry;
        this.reportYamlExporter = reportYamlExporter;
        this.executionService = executionService;
        this.toolStatusPublisher = toolStatusPublisher;
    }

    public Builder builder() {
        return new Builder();
    }

    public class Builder {
        private final Collection<CrmAiTool> tools = new HashSet<>();

        public Builder viewsDiscoveryTool() {
            return tool(new ViewsDiscoveryTool(serverProperties, viewRegistry, metadataTools));
        }

        public Builder jpqlQueryExecutorTool() {
            return tool(new JpqlExecutorTool(aiJpqlQueryService, toolStatusPublisher));
        }

        public Builder entitiesDiscoveryTool(Collection<AiContextEntityDefinition> toolDefinitions) {
            return tool(new EntitiesDiscoveryTool(metadataTools, yamlExporter, toolStatusPublisher, toolDefinitions));
        }

        public Builder reportsDiscoveryTool(Collection<String> whitelist) {
            return tool(new ReportsDiscoveryTool(reportYamlExporter, toolStatusPublisher, whitelist));
        }

        public Builder runReportTool(Collection<String> allowedReportCodes) {
            return tool(new RunReportTool(executionService, toolStatusPublisher, allowedReportCodes));
        }

        public Builder tool(CrmAiTool tool) {
            tools.add(tool);
            return this;
        }

        public Builder tools(Collection<CrmAiTool> tools) {
            this.tools.addAll(tools);
            return this;
        }

        public CrmAiTools build() {
            return new CrmAiTools(tools.toArray(new CrmAiTool[0]));
        }

        public Object[] buildToolsArray() {
            return build().tools();
        }
    }
}
