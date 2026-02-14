package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/**
 * Base entity for test model - should NOT be exported (MappedSuperclass)
 */
@MappedSuperclass
@JmixEntity
public abstract class BaseTestEntity {

    @Id
    @JmixGeneratedValue
    private UUID id;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}