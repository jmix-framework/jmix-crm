package com.company.crm.ai.jpql.query;

import io.jmix.core.EntitySerialization;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated converter for JPQL query results.
 *
 * <p>Handles conversion of Jmix KeyValueEntity results to serializable Maps
 * that can be safely returned to AI systems. Uses Jmix EntitySerialization
 * to handle entities safely and converts temporal types to consistent formats.
 */
@Component
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
        if (CollectionUtils.isEmpty(results)) {
            return new ArrayList<>();
        }

        return results.stream()
                .map(keyValueEntity -> convertRow(keyValueEntity, propertyNames))
                .toList();
    }

    private Map<String, Object> convertRow(KeyValueEntity keyValueEntity, String[] propertyNames) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (String propertyName : propertyNames) {
            Object value = keyValueEntity.getValue(propertyName);
            row.put(propertyName, convertToSerializableValue(value));
        }
        return row;
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
        switch (value) {
            case LocalDateTime localDateTime -> {
                return localDateTime.atOffset(ZoneOffset.UTC).toString();
            }
            case OffsetDateTime offsetDateTime -> {
                return offsetDateTime.toString();
            }


            // Handle collections recursively
            case Collection<?> collection -> {
                return collection.stream()
                        .map(this::convertToSerializableValue)
                        .toList();
            }
            default -> {
            }
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
