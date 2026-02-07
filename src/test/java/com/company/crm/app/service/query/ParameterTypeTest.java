package com.company.crm.app.service.query;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Map;

/**
 * Test to understand what parameter types actually arrive from Spring AI tool calls
 */
class ParameterTypeTest {

    private static final Logger log = LoggerFactory.getLogger(ParameterTypeTest.class);

    @Test
    void testWhatParameterTypesLookLike() {
        // Simulate different scenarios of what might come from Spring AI/Jackson

        // Scenario 1: LLM sends structured data, Jackson deserializes correctly
        Map<String, Object> wellTypedParameters = Map.of(
                "date", LocalDate.of(2024, 1, 15),
                "number", 42,
                "decimal", 1500.50,
                "flag", true,
                "text", "Hello World"
        );


        // Scenario 2: LLM sends everything as strings (worst case)
        Map<String, Object> stringParameters = Map.of(
                "date", "2024-01-15",
                "number", "42",
                "decimal", "1500.50",
                "flag", "true",
                "text", "Hello World"
        );


        // Scenario 3: Mixed (realistic)
        Map<String, Object> mixedParameters = Map.of(
                "date", "2024-01-15",           // String
                "typedDate", LocalDate.now(),   // Already typed
                "number", "42",                 // String
                "typedNumber", 42,              // Already typed
                "flag", true,                   // Already typed
                "text", "Hello World"           // String (correct)
        );

        log.info("=== Mixed parameters (realistic): ===");
        mixedParameters.forEach((key, value) -> {
            log.info("  {}: {} ({})", key, value, value.getClass().getSimpleName());
        });
    }
}