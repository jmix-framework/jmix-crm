package com.company.crm.app.service.query;

import com.company.crm.ai.jmix.query.AiJpqlParameterConverter;
import com.company.crm.app.annotation.TestProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.ConversionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link AiJpqlParameterConverter} that verify parameter conversion functionality.
 * Tests focus on the converter's ability to handle basic parameter types and transformations
 * required for AI-assisted JPQL query processing.
 */
@SpringBootTest
@TestProfile
class AiJpqlParameterConverterTest {

    @Autowired
    private ConversionService conversionService;

    @Test
    void testParameterConverterCreation() {
        var converter = new AiJpqlParameterConverter(conversionService);
        assertThat(converter).isNotNull();
    }

    @Test
    void testLocalDateConversion() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Object result = converter.convertParameterValue("2024-01-15");

        assertThat(result).isInstanceOf(LocalDate.class);
        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void testLocalDateTimeConversion() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Object result = converter.convertParameterValue("2024-01-15T10:30:00");

        assertThat(result).isInstanceOf(LocalDateTime.class);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void testNumericStringsConvertedToBigDecimal() {
        var converter = new AiJpqlParameterConverter(conversionService);

        // Numbers should be converted to BigDecimal to help JPQL engine
        Object result42 = converter.convertParameterValue("42");
        Object resultDecimal = converter.convertParameterValue("1500.50");

        // Should be converted to BigDecimal - JPQL engine is strict about types
        assertThat(result42).isInstanceOf(java.math.BigDecimal.class);
        assertThat(result42).isEqualTo(new java.math.BigDecimal("42"));
        assertThat(resultDecimal).isInstanceOf(java.math.BigDecimal.class);
        assertThat(resultDecimal).isEqualTo(new java.math.BigDecimal("1500.50"));
    }

    @Test
    void testStringRemainsString() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Object result = converter.convertParameterValue("%Test%");

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("%Test%");
    }

    @Test
    void testNullValue() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Object result = converter.convertParameterValue(null);

        assertThat(result).isNull();
    }

    @Test
    void testAlreadyCorrectlyTypedParameters() {
        var converter = new AiJpqlParameterConverter(conversionService);

        // Test various already-typed parameters that Spring AI/Jackson might provide
        Integer existingInt = 42;
        Boolean existingBool = true;
        LocalDate existingDate = LocalDate.of(2024, 1, 15);

        assertThat(converter.convertParameterValue(existingInt)).isSameAs(existingInt);
        assertThat(converter.convertParameterValue(existingBool)).isSameAs(existingBool);
        assertThat(converter.convertParameterValue(existingDate)).isSameAs(existingDate);
    }

    @Test
    void testMixedParameterMapWithCorrectTypes() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringDate", "2024-01-15"); // String -> will be converted to LocalDate
        parameters.put("alreadyTypedDate", LocalDate.of(2024, 1, 15)); // Already typed -> unchanged
        parameters.put("stringNumber", "100"); // String -> will be converted to BigDecimal
        parameters.put("alreadyTypedNumber", 100); // Already typed -> unchanged
        parameters.put("alreadyTypedBool", true); // Already typed -> unchanged
        parameters.put("pattern", "%Test%"); // String pattern -> unchanged

        Map<String, Object> result = converter.convertParameters(parameters);

        assertThat(result).hasSize(6);
        assertThat(result.get("stringDate")).isInstanceOf(LocalDate.class); // Converted
        assertThat(result.get("alreadyTypedDate")).isSameAs(parameters.get("alreadyTypedDate")); // Unchanged
        assertThat(result.get("stringNumber")).isInstanceOf(java.math.BigDecimal.class); // Converted to BigDecimal
        assertThat(result.get("stringNumber")).isEqualTo(new java.math.BigDecimal("100")); // Converted to BigDecimal
        assertThat(result.get("alreadyTypedNumber")).isSameAs(parameters.get("alreadyTypedNumber")); // Unchanged
        assertThat(result.get("alreadyTypedBool")).isSameAs(parameters.get("alreadyTypedBool")); // Unchanged
        assertThat(result.get("pattern")).isEqualTo("%Test%"); // Unchanged
    }

    @Test
    void testEmptyParameterMap() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Map<String, Object> empty = new HashMap<>();
        Map<String, Object> result = converter.convertParameters(empty);

        assertThat(result).isEmpty();
    }

    @Test
    void testNullParameterMap() {
        var converter = new AiJpqlParameterConverter(conversionService);

        Map<String, Object> result = converter.convertParameters(null);

        assertThat(result).isNull();
    }

    @Test
    void testInvalidDateString() {
        var converter = new AiJpqlParameterConverter(conversionService);

        // Invalid date strings should remain as strings
        Object result = converter.convertParameterValue("not-a-date");

        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("not-a-date");
    }
}