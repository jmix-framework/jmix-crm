package com.company.crm.ai.jmix.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiJpqlParameterConverterDebugTest {

    private AiJpqlParameterConverter converter;

    @BeforeEach
    void setUp() {
        converter = new AiJpqlParameterConverter(new DefaultConversionService());
    }

    @Test
    void shouldConvertUuidStringToUuidObject() {
        // given
        UUID originalUuid = UUID.randomUUID();
        String uuidString = originalUuid.toString();
        Map<String, Object> parameters = Map.of("clientId", uuidString);

        // when
        Map<String, Object> convertedParameters = converter.convertParameters(JpqlParameters.fromMap(parameters).parameters());

        // then
        assertThat(convertedParameters).hasSize(1);
        assertThat(convertedParameters.get("clientId")).isInstanceOf(UUID.class);
        assertThat(convertedParameters.get("clientId")).isEqualTo(originalUuid);
        System.out.println("Original: " + parameters);
        System.out.println("Converted: " + convertedParameters);
    }

    @Test
    void shouldKeepNonUuidStringsAsStrings() {
        // given
        Map<String, Object> parameters = Map.of("name", "some-name");

        // when
        Map<String, Object> convertedParameters = converter.convertParameters(JpqlParameters.fromMap(parameters).parameters());

        // then
        assertThat(convertedParameters).hasSize(1);
        assertThat(convertedParameters.get("name")).isInstanceOf(String.class);
        assertThat(convertedParameters.get("name")).isEqualTo("some-name");
        System.out.println("Original: " + parameters);
        System.out.println("Converted: " + convertedParameters);
    }

    @Test
    void shouldHandleMixedParameters() {
        // given
        UUID clientId = UUID.randomUUID();
        Map<String, Object> parameters = Map.of(
                "clientId", clientId.toString(),
                "name", "some-name",
                "amount", "1500.50"
        );

        // when
        Map<String, Object> convertedParameters = converter.convertParameters(JpqlParameters.fromMap(parameters).parameters());

        // then
        assertThat(convertedParameters).hasSize(3);
        assertThat(convertedParameters.get("clientId")).isInstanceOf(UUID.class);
        assertThat(convertedParameters.get("name")).isInstanceOf(String.class);
        assertThat(convertedParameters.get("amount")).isInstanceOf(java.math.BigDecimal.class);
        System.out.println("Original: " + parameters);
        System.out.println("Converted: " + convertedParameters);
    }
}