package com.company.crm.test.ai.report.introspection;

import com.company.crm.AbstractTest;
import com.company.crm.ai.tool.ReportsDiscoveryTool;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportsDiscoveryToolTest extends AbstractTest {

    @Test
    void shouldReturnAvailableReportsYaml() {
        // given
        // Empty whitelist means "all reports" in this test profile
        ReportsDiscoveryTool discoveryTool = ReportsDiscoveryTool.create(applicationContext, List.of());

        // when
        String yaml = discoveryTool.getAvailableReports(null);

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
        ReportsDiscoveryTool discoveryTool = ReportsDiscoveryTool.create(applicationContext, List.of());
        List<String> requestedCodes = List.of("client-360-report");

        // when
        String yaml = discoveryTool.getReportsByCodes(requestedCodes, null);

        // then
        assertThat(yaml).contains("client-360-report:");
        assertThat(yaml).doesNotContain("\n  invoice-");
    }
}
