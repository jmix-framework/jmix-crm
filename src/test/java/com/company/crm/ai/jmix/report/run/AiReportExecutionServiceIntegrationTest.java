package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import io.jmix.reports.ReportRepository;
import io.jmix.reports.entity.Report;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportExecutionServiceIntegrationTest extends AbstractTest {

    @Autowired
    private AiReportExecutionService executionService;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void testExecuteClient360Report() {
        systemAuthenticator.runWithSystem(() -> {
            Client client = entities.client("Integration Test Client");
            
            Map<String, Object> parameters = Map.of(
                    "client", client.getId().toString(),
                    "fromDate", LocalDate.now().minusDays(30).toString(),
                    "toDate", LocalDate.now().toString()
            );

            ReportExecutionResult result = executionService.executeReport("client-360-report", parameters, null, "HTML", List.of("client-360-report"));

            if (!result.success()) {
                System.out.println("DEBUG: Report failed. Code: " + result.errorCode() + ", Message: " + result.errorMessage());
                if (result.validationErrors() != null) {
                    result.validationErrors().forEach(e -> System.out.println("DEBUG: Validation error: " + e.parameterAlias() + " - " + e.errorMessage()));
                }
            }

            assertThat(result.success())
                    .withFailMessage("Report execution failed: " + result.errorMessage() + " (Error code: " + result.errorCode() + ")")
                    .isTrue();
            assertThat(result.reportCode()).isEqualTo("client-360-report");
            assertThat(result.outputType()).isEqualTo("HTML");
            assertThat(result.content()).contains("Client 360° Report");
            assertThat(result.content()).contains("Integration Test Client");
        });
    }

    @Test
    void testReportNotFound() {
        systemAuthenticator.runWithSystem(() -> {
            ReportExecutionResult result = executionService.executeReport("non-existent-report", Map.of(), null, null, List.of("non-existent-report"));
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("REPORT_NOT_FOUND");
        });
    }

    @Test
    void testAccessDenied() {
        systemAuthenticator.runWithSystem(() -> {
            // Report is NOT in the whitelist
            ReportExecutionResult result = executionService.executeReport("client-360-report", Map.of(), null, null, List.of("some-other-report"));
            assertThat(result.success()).isFalse();
            assertThat(result.errorCode()).isEqualTo("ACCESS_DENIED");
            assertThat(result.errorMessage()).contains("Ensure it is whitelisted");
        });
    }
}
