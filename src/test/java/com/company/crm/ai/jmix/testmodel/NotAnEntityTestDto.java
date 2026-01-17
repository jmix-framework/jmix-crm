package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.InstanceName;

import java.util.UUID;

/**
 * Jmix DTO entity (non-JPA) - should NOT appear in domain model export
 * because it's not a JPA entity but a pure DTO entity
 */
@JmixEntity
public class NotAnEntityTestDto {

    @JmixId
    @JmixGeneratedValue
    private UUID id;

    @InstanceName
    private String value;

    private String description;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}