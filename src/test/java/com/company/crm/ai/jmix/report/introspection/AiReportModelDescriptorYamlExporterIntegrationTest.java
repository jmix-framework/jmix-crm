package com.company.crm.ai.jmix.report.introspection;

import com.company.crm.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportModelDescriptorYamlExporterIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportModelDescriptorYamlExporter exporter;

    @Test
    void shouldExportAllReportsToYaml() {
        // given

        // when
        String yaml = exporter.export();

        // then
        assertThat(yaml).isNotEmpty();
        assertThat(yaml).contains("reports:");
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).contains("code: client-360-report");
        assertThat(yaml).contains("name: Client 360 Report");
        assertThat(yaml).contains("description:");
        assertThat(yaml).contains("Comprehensive 360-degree view of a client including financial risk");
        assertThat(yaml).contains("holistic overview");
        assertThat(yaml).contains("alias: client");
        assertThat(yaml).contains("type: ENTITY");
        assertThat(yaml).contains("entityMetaClass: Client");
        
        // Check template details
        assertThat(yaml).contains("code: HTML");
        assertThat(yaml).contains("outputType: HTML");
        assertThat(yaml).contains("isDefault: true");
        assertThat(extractReportCodes(yaml)).contains("client-360-report");
    }

    @Test
    void shouldExportOnlyRequestedReportsToYaml() {
        // given
        List<String> requestedReports = List.of("client-360-report");

        // when
        String yaml = exporter.export(requestedReports);

        // then
        assertThat(yaml).contains("client-360-report:");
        assertThat(extractReportCodes(yaml)).containsExactly("client-360-report");
    }

    private Set<String> extractReportCodes(String yaml) {
        Set<String> codes = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("(?m)^  ([^\\s:][^:]*):$").matcher(yaml);
        while (matcher.find()) {
            codes.add(matcher.group(1));
        }
        return codes;
    }
}
