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

    /**
     * Target types we attempt to convert string parameters to, in order of preference
     */
    private static final List<Class<?>> TARGET_TYPES = List.of(
            // Modern Java Time API
            LocalDate.class,
            LocalDateTime.class,
            LocalTime.class,
            ZonedDateTime.class,
            OffsetDateTime.class,
            OffsetTime.class,

            // Legacy Java Date API
            java.util.Date.class,
            Date.class,
            Timestamp.class,
            Time.class,

            // Numeric types for calculations
            BigDecimal.class,
            Integer.class,
            Long.class,
            Double.class,
            Float.class,

            // UUID for entity IDs
            UUID.class,

            // Boolean
            Boolean.class,

            // Instant last - can convert any number to timestamp
            Instant.class
    );

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
     *
     * We systematically try each target type using Spring's ConversionService.
     * The first successful conversion wins.
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

        // Try each target type using Spring ConversionService
        Object converted = TARGET_TYPES.stream()
                .filter(targetType -> conversionService.canConvert(String.class, targetType))
                .map(targetType -> tryConvertToType(stringValue, targetType))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (converted != null) {
            log.debug("Spring converted parameter '{}' from String to {}: {}",
                    stringValue, converted.getClass().getSimpleName(), converted);
            return converted;
        }

        // No conversion possible: let JPQL engine handle it
        log.debug("No conversion possible for '{}', letting JPQL engine handle it", stringValue);
        return stringValue;
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