package com.company.crm.ai.jmix.query;

import io.jmix.core.EntitySerialization;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Dedicated converter for JPQL query results.
 *
 * <p>Handles conversion of Jmix KeyValueEntity results to serializable Maps
 * that can be safely returned to AI systems. Uses Jmix EntitySerialization
 * to handle entities safely and converts temporal types to consistent formats.
 */
@Component("ai_ResultConverter")
public class ResultConverter {

    private final EntitySerialization entitySerialization;

    public ResultConverter(EntitySerialization entitySerialization) {
        this.entitySerialization = entitySerialization;
    }

    /**
     * Convert KeyValueEntity results to List of Maps
     * Uses Jmix EntitySerialization to handle entities safely
     */
    public List<Map<String, Object>> convertToMapList(List<KeyValueEntity> results, String[] propertyNames) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> mapList = new ArrayList<>();
        for (KeyValueEntity keyValueEntity : results) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String propertyName : propertyNames) {
                Object value = keyValueEntity.getValue(propertyName);
                row.put(propertyName, convertToSerializableValue(value));
            }
            mapList.add(row);
        }
        return mapList;
    }

    /**
     * Convert potentially complex objects to serializable values for AI consumption
     */
    public Object convertToSerializableValue(Object value) {
        if (value == null) {
            return null;
        }

        // Handle primitives and standard types first (before entity serialization)
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        // Handle temporal types - convert to consistent string representation
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC).toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }

        // Handle collections recursively
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::convertToSerializableValue)
                    .toList();
        }

        // Handle Jmix entities using EntitySerialization (only for complex objects)
        try {
            return entitySerialization.objectToJson(value);
        } catch (Exception e) {
            // Not an entity or serialization failed, return as-is
            return value;
        }
    }
}