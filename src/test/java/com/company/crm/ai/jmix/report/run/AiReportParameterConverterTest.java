package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportInputParameter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportParameterConverterTest extends AbstractTest {

    @Autowired
    private AiReportParameterConverter converter;

    @Test
    void testConvertText() {
        // given
        ReportInputParameter paramDef = createParam("textParam", ParameterType.TEXT, false, null, null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("textParam", "value"));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("textParam")).isEqualTo("value");
    }

    @Test
    void testConvertNumeric() {
        // given
        ReportInputParameter paramDef = createParam("numParam", ParameterType.NUMERIC, false, null, null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("numParam", 100.5));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("numParam")).isInstanceOf(BigDecimal.class);
        assertThat(result.convertedParameters().get("numParam")).isEqualTo(new BigDecimal("100.5"));
    }

    @Test
    void testConvertDate() {
        // given
        ReportInputParameter paramDef = createParam("dateParam", ParameterType.DATE, false, null, null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("dateParam", "2023-01-01"));

        // then
        assertThat(result.success()).isTrue();
        Object converted = result.convertedParameters().get("dateParam");
        assertThat(converted).isInstanceOf(Date.class);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) converted);
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY);
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1);
    }

    @Test
    void testConvertTime() {
        // given
        ReportInputParameter paramDef = createParam("timeParam", ParameterType.TIME, false, null, null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("timeParam", "12:30:45"));

        // then
        assertThat(result.success()).isTrue();
        Object converted = result.convertedParameters().get("timeParam");
        assertThat(converted).isInstanceOf(Date.class);
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) converted);
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(12);
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(30);
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(45);
    }

    @Test
    void testConvertDateTime() {
        // given
        ReportInputParameter paramDef = createParam("dateTimeParam", ParameterType.DATETIME, false, null, null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("dateTimeParam", "2023-01-01T12:30:45"));

        // then
        assertThat(result.success()).isTrue();
        Object converted = result.convertedParameters().get("dateTimeParam");
        assertThat(converted).isInstanceOf(Date.class);
        Calendar cal = Calendar.getInstance();
        cal.setTime((Date) converted);
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(2023);
        assertThat(cal.get(Calendar.MONTH)).isEqualTo(Calendar.JANUARY);
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1);
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(12);
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(30);
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(45);
    }

    @Test
    void testConvertEnum() {
        // given
        ReportInputParameter paramDef = createParam(
                "enumParam",
                ParameterType.ENUMERATION,
                false,
                null,
                ParameterType.class.getName()
        );

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("enumParam", "TEXT"));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("enumParam")).isEqualTo(ParameterType.TEXT);
    }

    @Test
    void testConvertEntity() {
        // given
        Client client = entities.client("Converter Test Client");
        ReportInputParameter paramDef = createParam("clientParam", ParameterType.ENTITY, false, "Client", null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("clientParam", client.getId().toString()));

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("clientParam")).isEqualTo(client);
    }

    @Test
    void testConvertEntityList() {
        // given
        Client client1 = entities.client("List Client 1");
        Client client2 = entities.client("List Client 2");
        ReportInputParameter paramDef = createParam("clientListParam", ParameterType.ENTITY_LIST, false, "Client", null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), 
                Map.of("clientListParam", List.of(client1.getId().toString(), client2.getId().toString())));

        // then
        assertThat(result.success()).isTrue();
        List<Object> converted = (List<Object>) result.convertedParameters().get("clientListParam");
        assertThat(converted).containsExactlyInAnyOrder(client1, client2);
    }

    @Test
    void testConvertEntityListInvalidFormat() {
        // given
        ReportInputParameter paramDef = createParam("clientListParam", ParameterType.ENTITY_LIST, false, "Client", null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), 
                Map.of("clientListParam", "not-a-collection"));

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage()).contains("must be a collection");
    }

    @Test
    void testUnknownAlias() {
        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(), Map.of("unknownParam", "someValue"));

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).parameterAlias()).isEqualTo("unknownParam");
        assertThat(result.errors().get(0).errorMessage()).contains("Unknown report parameter alias");
    }

    @Test
    void testNullParameters() {
        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(), null);

        // then
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters()).isEmpty();
    }

    @Test
    void testRequiredParameterMissing() {
        // given
        ReportInputParameter paramDef = createParam("reqParam", ParameterType.TEXT, true, null, null);

        // when
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of());

        // then
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).parameterAlias()).isEqualTo("reqParam");
    }

    private ReportInputParameter createParam(String alias,
                                             ParameterType type,
                                             boolean required,
                                             String entityMetaClass,
                                             String enumClass) {
        ReportInputParameter param = dataManager.create(ReportInputParameter.class);
        param.setAlias(alias);
        param.setType(type);
        param.setRequired(required);
        param.setEntityMetaClass(entityMetaClass);
        param.setEnumerationClass(enumClass);
        return param;
    }
}
