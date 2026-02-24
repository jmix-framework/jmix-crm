package com.company.crm.ai.jmix.report.run;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ReportExecutionResultTest {

    @Test
    void testSuccess() {
        // when
        ReportExecutionResult result = ReportExecutionResult.success("report-code", "template-code", "HTML", "content");

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.reportCode()).isEqualTo("report-code");
        assertThat(result.templateCodeUsed()).isEqualTo("template-code");
        assertThat(result.outputType()).isEqualTo("HTML");
        assertThat(result.content()).isEqualTo("content");
        assertThat(result.errorCode()).isNull();
        assertThat(result.errorMessage()).isNull();
        assertThat(result.validationErrors()).isNull();
    }

    @Test
    void testFailed() {
        // when
        ReportExecutionResult result = ReportExecutionResult.failed("report-code", ReportExecutionErrorCode.EXECUTION_ERROR, "error-message");

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.reportCode()).isEqualTo("report-code");
        assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.EXECUTION_ERROR);
        assertThat(result.errorMessage()).isEqualTo("error-message");
    }

    @Test
    void testValidationError() {
        // given
        List<ReportValidationError> errors = List.of(new ReportValidationError("param1", "error1"));

        // when
        ReportExecutionResult result = ReportExecutionResult.validationError("report-code", errors);

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.errorCode()).isEqualTo(ReportExecutionErrorCode.PARAMETER_VALIDATION_ERROR);
        assertThat(result.validationErrors()).hasSize(1);
        assertThat(result.validationErrors().get(0).parameterAlias()).isEqualTo("param1");
        assertThat(result.validationErrors().get(0).errorMessage()).isEqualTo("error1");
    }
}
