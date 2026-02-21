package com.company.crm.ai.jmix.report.introspection;

import com.company.crm.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportModelDescriptorYamlExporterIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportModelDescriptorYamlExporter exporter;

    @Test
    void shouldExportAllReportsToYaml() {
        // Act
        String yaml = exporter.export();
        System.out.println("DEBUG YAML OUTPUT:\n" + yaml);

        // Assert
        assertThat(yaml).isNotEmpty();
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).contains("code: client-360-report");
        assertThat(yaml).contains("name: Client 360 Report");
        assertThat(yaml).contains("description:");
        assertThat(yaml).contains("Comprehensive client report");
        assertThat(yaml).contains("alias: client");
        assertThat(yaml).contains("type: ENTITY");
        assertThat(yaml).contains("entityMetaClass: Client");
        
        // Check template details
        assertThat(yaml).contains("code: HTML");
        assertThat(yaml).contains("outputType: HTML");
        assertThat(yaml).contains("isDefault: true");
    }

    @Test
    void shouldExportOnlyRequestedReportsToYaml() {
        // Act
        String yaml = exporter.export(List.of("client-360-report"));

        // Assert
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).doesNotContain("invoice-report:");
    }
}
