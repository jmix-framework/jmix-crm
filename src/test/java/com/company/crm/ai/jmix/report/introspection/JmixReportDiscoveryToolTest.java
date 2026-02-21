package com.company.crm.ai.jmix.report.introspection;

import com.company.crm.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JmixReportDiscoveryToolTest extends AbstractTest {

    private JmixReportDiscoveryTool discoveryTool;

    @Autowired
    private AiReportModelDescriptorYamlExporter yamlExporter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        discoveryTool = new JmixReportDiscoveryTool(yamlExporter, List.of());
    }

    @Test
    void shouldReturnAvailableReportsYaml() {
        // Act
        String yaml = discoveryTool.getAvailableReports();

        // Assert
        assertThat(yaml).isNotEmpty();
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).contains("name: Client 360 Report");
        assertThat(yaml).contains("alias: client");
    }

    @Test
    void shouldReturnRequestedReportsYaml() {
        // Act
        String yaml = discoveryTool.getReportsByCodes(List.of("client-360-report"));

        // Assert
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).doesNotContain("invoice-report:");
    }
}
