package com.company.crm.ai.jmix.report.run;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ReportParameterConversionResultTest {

    @Test
    void testSuccess() {
        // given
        Map<String, Object> params = Map.of("param1", "value1");

        // when
        ReportParameterConversionResult result = ReportParameterConversionResult.success(params);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters()).isEqualTo(params);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void testSuccess_reflectsMutableInputMapContract() {
        // given
        HashMap<String, Object> params = new HashMap<>();
        params.put("param1", "value1");

        // when
        ReportParameterConversionResult result = ReportParameterConversionResult.success(params);
        params.put("param2", "value2");

        // then
        assertThat(result.convertedParameters()).containsEntry("param2", "value2");
    }

    @Test
    void testFailed() {
        // given
        List<ReportValidationError> errors = List.of(new ReportValidationError("param1", "error1"));

        // when
        ReportParameterConversionResult result = ReportParameterConversionResult.failed(errors, true);

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.convertedParameters()).isEmpty();
        assertThat(result.errors()).isEqualTo(errors);
        assertThat(result.hasConversionErrors()).isTrue();
        assertThat(result.errors().get(0).parameterAlias()).isEqualTo("param1");
        assertThat(result.errors().get(0).errorMessage()).isEqualTo("error1");
    }
}
