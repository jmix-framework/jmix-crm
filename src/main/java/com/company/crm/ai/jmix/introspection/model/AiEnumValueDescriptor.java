package com.company.crm.ai.jmix.introspection.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Structured enum value descriptor for AI model introspection output.
 *
 * @param id stored value used by the entity (EnumClass#getId or ordinal fallback)
 * @param description optional localized description for the enum value
 */
public record AiEnumValueDescriptor(
        Object id,
        @JsonInclude(JsonInclude.Include.NON_NULL) String description
) {
}
