package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunReportToolIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    private RunReportTool reportTool;

    @BeforeEach
    @Override
    protected void beforeEach() {
        reportTool = new RunReportTool(executionService, List.of("client-360-report", "unknown-report"));
    }

    @Test
    void testRunReport() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Tool Test Client");
            
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = reportTool.runReport("client-360-report", parameters, null, "HTML");

            assertThat(result.success()).isTrue();
            assertThat(result.reportCode()).isEqualTo("client-360-report");
            assertThat(result.content()).contains("Client 360° Report");
            assertThat(result.content()).contains("Tool Test Client");
        });
    }

    @Test
    void testRunReportNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            ReportExecutionResult result = reportTool.runReport("unknown-report", Map.of(), null, null);
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("REPORT_NOT_FOUND");
        });
    }
}
