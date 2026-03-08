package com.company.crm.ai.jmix.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiJpqlParameterConverterDebugUuidDisabledTest {

    private AiJpqlParameterConverterNoUuid converter;

    @BeforeEach
    void setUp() {
        converter = new AiJpqlParameterConverterNoUuid(new DefaultConversionService());
    }

    @Test
    void shouldNotConvertUuidStringToUuidObject() {
        // given
        UUID originalUuid = UUID.randomUUID();
        String uuidString = originalUuid.toString();
        Map<String, Object> parameters = Map.of("clientId", uuidString);

        // when
        Map<String, Object> convertedParameters = converter.convertParameters(JpqlParameters.fromMap(parameters).parameters());

        // then
        assertThat(convertedParameters).hasSize(1);
        assertThat(convertedParameters.get("clientId")).isInstanceOf(String.class);
        assertThat(convertedParameters.get("clientId")).isEqualTo(uuidString);
        System.out.println("Original: " + parameters);
        System.out.println("Converted: " + convertedParameters);
    }

    /**
     * Test version of AiJpqlParameterConverter that skips UUID conversion
     */
    static class AiJpqlParameterConverterNoUuid extends AiJpqlParameterConverter {

        public AiJpqlParameterConverterNoUuid(org.springframework.core.convert.ConversionService conversionService) {
            super(conversionService);
        }

        @Override
        public Object convertParameterValue(Object value) {
            if (value == null) {
                return null;
            }

            // If already correctly typed, return as-is
            if (!(value instanceof String stringValue)) {
                return value;
            }

            if (stringValue.isBlank()) {
                return stringValue;
            }

            // 1. Check for Boolean (very specific)
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                return Boolean.valueOf(stringValue);
            }

            // SKIP UUID CONVERSION - this is the key change!

            // 3. Check for Date/Time patterns (e.g., 2024-01-15 or 2024-01-15T10:00:00)
            if (stringValue.length() >= 10 && Character.isDigit(stringValue.charAt(0)) && stringValue.contains("-")) {
                Object date = tryConvertDateTypes(stringValue);
                if (date != null) return date;
            }

            // 4. Check for Numeric patterns (only if it looks like a number and isn't too long to be an ID)
            if (isLikelyNumeric(stringValue)) {
                Object numeric = tryConvertNumericTypes(stringValue);
                if (numeric != null) return numeric;
            }

            // No conversion possible or likely: let JPQL engine handle it
            return stringValue;
        }

        // Need to expose these protected methods for testing
        private Object tryConvertDateTypes(String stringValue) {
            try {
                var method = AiJpqlParameterConverter.class.getDeclaredMethod("tryConvertDateTypes", String.class);
                method.setAccessible(true);
                return method.invoke(this, stringValue);
            } catch (Exception e) {
                return null;
            }
        }

        private Object tryConvertNumericTypes(String stringValue) {
            try {
                var method = AiJpqlParameterConverter.class.getDeclaredMethod("tryConvertNumericTypes", String.class);
                method.setAccessible(true);
                return method.invoke(this, stringValue);
            } catch (Exception e) {
                return null;
            }
        }

        private boolean isLikelyNumeric(String s) {
            try {
                var method = AiJpqlParameterConverter.class.getDeclaredMethod("isLikelyNumeric", String.class);
                method.setAccessible(true);
                return (Boolean) method.invoke(this, s);
            } catch (Exception e) {
                return false;
            }
        }
    }
}