package com.company.crm.test.ai.jpql.query;

import com.company.crm.ai.jpql.query.AiJpqlParameterConverter;
import com.company.crm.ai.jpql.query.JpqlParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AiJpqlParameterConverter} that verify parameter conversion functionality.
 * Tests focus on the converter's ability to handle basic parameter types and transformations
 * required for AI-assisted JPQL query processing.
 */
class AiJpqlParameterConverterTest {

    private ConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new ApplicationConversionService();
    }

    @Test
    void testBooleanStringConversion() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("true");

        // then
        assertThat(result).isEqualTo(true);
    }

    @Test
    void testLocalDateConversion() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("2024-01-15");

        // then
        assertThat(result).isInstanceOf(LocalDate.class);
        assertThat(result).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    void testLocalDateTimeConversion() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("2024-01-15T10:30:00");

        // then
        assertThat(result).isInstanceOf(LocalDateTime.class);
        assertThat(result).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
    }

    @Test
    void testNumericStringsConvertedToBigDecimal() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result42 = converter.convertParameterValue("42");
        Object resultDecimal = converter.convertParameterValue("1500.50");

        // then
        assertThat(result42).isInstanceOf(java.math.BigDecimal.class);
        assertThat(result42).isEqualTo(new java.math.BigDecimal("42"));
        assertThat(resultDecimal).isInstanceOf(java.math.BigDecimal.class);
        assertThat(resultDecimal).isEqualTo(new java.math.BigDecimal("1500.50"));
    }

    @Test
    void testStringRemainsString() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("%Test%");

        // then
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("%Test%");
    }

    @Test
    void testNullValue() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void testAlreadyCorrectlyTypedParameters() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // Test various already-typed parameters that Spring AI/Jackson might provide
        Integer existingInt = 42;
        Boolean existingBool = true;
        LocalDate existingDate = LocalDate.of(2024, 1, 15);

        // when / then
        assertThat(converter.convertParameterValue(existingInt)).isSameAs(existingInt);
        assertThat(converter.convertParameterValue(existingBool)).isSameAs(existingBool);
        assertThat(converter.convertParameterValue(existingDate)).isSameAs(existingDate);
    }

    @Test
    void testMixedParameterMapWithCorrectTypes() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringDate", "2024-01-15"); // String -> will be converted to LocalDate
        parameters.put("alreadyTypedDate", LocalDate.of(2024, 1, 15)); // Already typed -> unchanged
        parameters.put("stringNumber", "100"); // String -> will be converted to BigDecimal
        parameters.put("alreadyTypedNumber", 100); // Already typed -> unchanged
        parameters.put("alreadyTypedBool", true); // Already typed -> unchanged
        parameters.put("pattern", "%Test%"); // String pattern -> unchanged

        // when
        Map<String, Object> result = converter.convertParameters(JpqlParameters.fromMap(parameters).parameters());

        // then
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
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        Map<String, Object> empty = new HashMap<>();

        // when
        Map<String, Object> result = converter.convertParameters(JpqlParameters.fromMap(empty).parameters());

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testNullParameterMap() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Map<String, Object> result = converter.convertParameters(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void testInvalidDateString() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("not-a-date");

        // then
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("not-a-date");
    }

    @Test
    void testLocaleSensitiveNumericString_staysString() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("1,50");

        // then
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo("1,50");
    }

    @Test
    void testVeryLongNumericId_staysString() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);
        String longNumericId = "1234567890123456789012345";

        // when
        Object result = converter.convertParameterValue(longNumericId);

        // then
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo(longNumericId);
    }

    @Test
    void testSignedNumericString_convertedToBigDecimal() {
        // given
        var converter = new AiJpqlParameterConverter(conversionService);

        // when
        Object result = converter.convertParameterValue("-42.25");

        // then
        assertThat(result).isInstanceOf(java.math.BigDecimal.class);
        assertThat(result).isEqualTo(new java.math.BigDecimal("-42.25"));
    }
}
