package com.company.crm.ai.jmix.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Smart converter for JPQL query parameters using Spring's ConversionService.
 *
 * <p>This component automatically converts string parameters from AI queries to appropriate Java types
 * for JPQL execution. It uses Spring's ConversionService to handle type conversion systematically.
 *
 * <h3>Conversion Strategy</h3>
 * <p>The converter maintains a prioritized list of target types and attempts conversion in order:
 * <ol>
 *   <li>Java Time API types (LocalDate, LocalDateTime, etc.)</li>
 *   <li>Legacy SQL types (Date, Time, Timestamp)</li>
 *   <li>Numeric types (BigDecimal, Long, Integer, Double)</li>
 *   <li>UUID for entity references</li>
 *   <li>Boolean for true/false values</li>
 * </ol>
 *
 * <p>For each string parameter, we ask Spring's ConversionService: "Can you convert this string
 * to each target type?" and use the first successful conversion. This approach leverages Spring's
 * robust type conversion capabilities while maintaining predictable behavior.
 *
 * @see ConversionService
 * @see org.springframework.core.convert.converter.Converter
 */
@Component("ai_JpqlParameterConverter")
public class AiJpqlParameterConverter {

    private static final Logger log = LoggerFactory.getLogger(AiJpqlParameterConverter.class);

    private final ConversionService conversionService;

    public AiJpqlParameterConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Convert all parameters - only converts obvious cases
     */
    public Map<String, Object> convertParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return null;
        }

        if (parameters.isEmpty()) {
            return parameters; // Return empty map, not null
        }

        return parameters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertParameterValue(entry.getValue())
                ));
    }

    /**
     * Convert a single parameter value by trying each target type in order.
     * Only attempts conversion if the string matches likely patterns for the target types
     * to avoid aggressive mis-conversion of plain strings.
     */
    public Object convertParameterValue(Object value) {
        if (value == null) {
            return null;
        }

        // If already correctly typed, return as-is
        if (!(value instanceof String stringValue)) {
            log.debug("Parameter '{}' already correctly typed as {}, no conversion needed",
                    value, value.getClass().getSimpleName());
            return value;
        }

        if (stringValue.isBlank()) {
            return stringValue;
        }

        // 1. Check for Boolean (very specific)
        if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
            return Boolean.valueOf(stringValue);
        }

        // 2. Check for UUID pattern
        if (stringValue.length() == 36 && stringValue.contains("-")) {
            Object uuid = tryConvertToType(stringValue, UUID.class);
            if (uuid != null) return uuid;
        }

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
        log.debug("No conversion likely for '{}', letting JPQL engine handle it as String", stringValue);
        return stringValue;
    }

    private Object tryConvertDateTypes(String stringValue) {
        List<Class<?>> dateTypes = List.of(
                LocalDate.class, LocalDateTime.class, OffsetDateTime.class,
                ZonedDateTime.class, java.util.Date.class, Timestamp.class
        );
        return dateTypes.stream()
                .filter(type -> conversionService.canConvert(String.class, type))
                .map(type -> tryConvertToType(stringValue, type))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Object tryConvertNumericTypes(String stringValue) {
        // Don't convert very long digit strings (likely IDs or codes)
        if (stringValue.length() > 15 && !stringValue.contains(".")) {
            return null;
        }
        
        List<Class<?>> numericTypes = List.of(BigDecimal.class, Long.class, Integer.class);
        return numericTypes.stream()
                .filter(type -> conversionService.canConvert(String.class, type))
                .map(type -> tryConvertToType(stringValue, type))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private boolean isLikelyNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        int dotCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c)) continue;
            if (c == '.' && dotCount == 0) {
                dotCount++;
                continue;
            }
            if (i == 0 && (c == '-' || c == '+')) continue;
            return false;
        }
        return Character.isDigit(s.charAt(s.length() - 1)); // Must end with digit
    }

    /**
     * Try to convert a string value to a specific target type using Spring ConversionService.
     * Returns the converted value or null if conversion fails.
     */
    private Object tryConvertToType(String stringValue, Class<?> targetType) {
        try {
            return conversionService.convert(stringValue, targetType);
        } catch (Exception e) {
            // Conversion failed - this is expected for some combinations
            log.trace("Failed to convert '{}' to {}: {}", stringValue, targetType.getSimpleName(), e.getMessage());
            return null;
        }
    }
}