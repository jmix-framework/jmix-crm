package com.company.crm.ai.jmix.report.run;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ReportExecutionResultTest {

    @Test
    void testSuccess() {
        ReportExecutionResult result = ReportExecutionResult.success("report-code", "template-code", "HTML", "content");
        assertThat(result.success()).isTrue();
        assertThat(result.reportCode()).isEqualTo("report-code");
        assertThat(result.templateCodeUsed()).isEqualTo("template-code");
        assertThat(result.outputType()).isEqualTo("HTML");
        assertThat(result.content()).isEqualTo("content");
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void testFailed() {
        ReportExecutionResult result = ReportExecutionResult.failed("report-code", "ERROR_CODE", "error-message");
        assertThat(result.success()).isFalse();
        assertThat(result.reportCode()).isEqualTo("report-code");
        assertThat(result.errorCode()).isEqualTo("ERROR_CODE");
        assertThat(result.errorMessage()).isEqualTo("error-message");
    }

    @Test
    void testValidationError() {
        List<ReportValidationError> errors = List.of(new ReportValidationError("param1", "error1"));
        ReportExecutionResult result = ReportExecutionResult.validationError("report-code", errors);
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo("PARAMETER_VALIDATION_ERROR");
        assertThat(result.validationErrors()).hasSize(1);
    }
}
