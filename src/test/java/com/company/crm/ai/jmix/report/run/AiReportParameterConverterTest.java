package com.company.crm.ai.jmix.report.run;

import com.company.crm.AbstractTest;
import com.company.crm.model.client.Client;
import io.jmix.reports.entity.ParameterType;
import io.jmix.reports.entity.ReportInputParameter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportParameterConverterTest extends AbstractTest {

    @Autowired
    private AiReportParameterConverter converter;

    @Test
    void testConvertText() {
        ReportInputParameter paramDef = createParam("textParam", ParameterType.TEXT);
        
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("textParam", "value"));
        
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("textParam")).isEqualTo("value");
    }

    @Test
    void testConvertNumeric() {
        ReportInputParameter paramDef = createParam("numParam", ParameterType.NUMERIC);
        
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("numParam", 100.5));
        
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("numParam")).isInstanceOf(BigDecimal.class);
        assertThat(result.convertedParameters().get("numParam")).isEqualTo(new BigDecimal("100.5"));
    }

    @Test
    void testConvertDate() {
        ReportInputParameter paramDef = createParam("dateParam", ParameterType.DATE);
        
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("dateParam", "2023-01-01"));
        
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
        ReportInputParameter paramDef = createParam("timeParam", ParameterType.TIME);
        
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("timeParam", "12:30:45"));
        
        assertThat(result.success()).isTrue();
        Object converted = result.convertedParameters().get("timeParam");
        assertThat(converted).isInstanceOf(Date.class);
    }

    @Test
    void testConvertDateTime() {
        ReportInputParameter paramDef = createParam("dateTimeParam", ParameterType.DATETIME);
        
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("dateTimeParam", "2023-01-01T12:30:45"));
        
        assertThat(result.success()).isTrue();
        Object converted = result.convertedParameters().get("dateTimeParam");
        assertThat(converted).isInstanceOf(Date.class);
    }

    @Test
    void testConvertEnum() {
        ReportInputParameter paramDef = createParam("enumParam", ParameterType.ENUMERATION);
        paramDef.setEnumerationClass(ParameterType.class.getName());

        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("enumParam", "TEXT"));
        
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("enumParam")).isEqualTo(ParameterType.TEXT);
    }

    @Test
    void testConvertEntity() {
        Client client = entities.client("Converter Test Client");
        ReportInputParameter paramDef = createParam("clientParam", ParameterType.ENTITY);
        paramDef.setEntityMetaClass("Client");

        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of("clientParam", client.getId().toString()));
        
        if (!result.success()) {
            result.errors().forEach(e -> System.out.println("DEBUG: Conversion error: " + e.parameterAlias() + " - " + e.errorMessage()));
        }
        
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters().get("clientParam")).isEqualTo(client);
    }

    @Test
    void testConvertEntityList() {
        Client client1 = entities.client("List Client 1");
        Client client2 = entities.client("List Client 2");
        ReportInputParameter paramDef = createParam("clientListParam", ParameterType.ENTITY_LIST);
        paramDef.setEntityMetaClass("Client");

        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), 
                Map.of("clientListParam", List.of(client1.getId().toString(), client2.getId().toString())));
        
        assertThat(result.success()).isTrue();
        List<Object> converted = (List<Object>) result.convertedParameters().get("clientListParam");
        assertThat(converted).containsExactlyInAnyOrder(client1, client2);
    }

    @Test
    void testConvertEntityListInvalidFormat() {
        ReportInputParameter paramDef = createParam("clientListParam", ParameterType.ENTITY_LIST);
        paramDef.setEntityMetaClass("Client");

        // Passing a string instead of a collection
        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), 
                Map.of("clientListParam", "not-a-collection"));
        
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).errorMessage()).contains("must be a collection");
    }

    @Test
    void testUnknownAlias() {
        ReportParameterConversionResult result = converter.convertParameters(List.of(), Map.of("unknownParam", "someValue"));
        
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).parameterAlias()).isEqualTo("unknownParam");
        assertThat(result.errors().get(0).errorMessage()).contains("Unknown report parameter alias");
    }

    @Test
    void testNullParameters() {
        ReportParameterConversionResult result = converter.convertParameters(List.of(), null);
        assertThat(result.success()).isTrue();
        assertThat(result.convertedParameters()).isEmpty();
    }

    @Test
    void testRequiredParameterMissing() {
        ReportInputParameter paramDef = createParam("reqParam", ParameterType.TEXT);
        paramDef.setRequired(true);

        ReportParameterConversionResult result = converter.convertParameters(List.of(paramDef), Map.of());
        
        assertThat(result.success()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).parameterAlias()).isEqualTo("reqParam");
    }

    private ReportInputParameter createParam(String alias, ParameterType type) {
        ReportInputParameter param = dataManager.create(ReportInputParameter.class);
        param.setAlias(alias);
        param.setType(type);
        return param;
    }
}
