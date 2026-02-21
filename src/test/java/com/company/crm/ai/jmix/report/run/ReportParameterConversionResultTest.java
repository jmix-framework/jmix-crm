package com.company.crm.ai.jmix.report.run;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ReportParameterConversionResultTest {

    @Test
    void testSuccess() {
        Map<String, Object> params = Map.of("param1", "value1");
        ReportParameterConversionResult result = ReportParameterConversionResult.success(params);
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters()).isEqualTo(params);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void testFailed() {
        List<ReportValidationError> errors = List.of(new ReportValidationError("param1", "error1"));
        ReportParameterConversionResult result = ReportParameterConversionResult.failed(errors, true);
        assertThat(result.success()).isFalse();
        assertThat(result.convertedParameters()).isEmpty();
        assertThat(result.errors()).isEqualTo(errors);
        assertThat(result.hasConversionErrors()).isTrue();
    }
}
