package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Original test entity that will be replaced by ReplacedTestEntity
 */
@Entity
@Table(name = "TEST_ORIGINAL")
@JmixEntity
@Comment("Original entity that should be replaced")
public class OriginalTestEntity extends BaseTestEntity {

    @Column(name = "ORIGINAL_NAME")
    @NotNull
    @Comment("Original name field")
    private String originalName;

    @Column(name = "ORIGINAL_VALUE")
    @Comment("Original value field")
    private String originalValue;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(String originalValue) {
        this.originalValue = originalValue;
    }
}