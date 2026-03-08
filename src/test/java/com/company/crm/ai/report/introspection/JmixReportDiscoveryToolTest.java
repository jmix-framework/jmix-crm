package com.company.crm.ai.report.introspection;

import com.company.crm.AbstractTest;
import com.company.crm.ai.report.introspection.AiReportModelDescriptorYamlExporter;
import com.company.crm.ai.report.introspection.JmixReportDiscoveryTool;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JmixReportDiscoveryToolTest extends AbstractTest {

    @Autowired
    private AiReportModelDescriptorYamlExporter yamlExporter;

    @Test
    void shouldReturnAvailableReportsYaml() {
        // given
        // Empty whitelist means "all reports" in this test profile
        JmixReportDiscoveryTool discoveryTool = new JmixReportDiscoveryTool(yamlExporter, List.of());

        // when
        String yaml = discoveryTool.getAvailableReports();

        // then
        assertThat(yaml).isNotEmpty();
        assertThat(yaml).contains("reports:");
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).contains("name: Client 360 Report");
        assertThat(yaml).contains("templates:");
        assertThat(yaml).contains("outputType: HTML");
        assertThat(yaml).contains("parameters:");
        assertThat(yaml).contains("alias: client");
        assertThat(yaml).contains("type: ENTITY");
    }

    @Test
    void shouldReturnRequestedReportsYaml() {
        // given
        // Empty whitelist means request filter is solely driven by requestedCodes
        JmixReportDiscoveryTool discoveryTool = new JmixReportDiscoveryTool(yamlExporter, List.of());
        List<String> requestedCodes = List.of("client-360-report");

        // when
        String yaml = discoveryTool.getReportsByCodes(requestedCodes);

        // then
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).doesNotContain("\n  invoice-");
    }
}
