package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.entity.annotation.ReplaceEntity;
import io.jmix.core.metamodel.annotation.Comment;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Replacement test entity that replaces OriginalTestEntity using @ReplaceEntity
 * This entity extends the original and adds additional fields.
 */
@Entity
@JmixEntity
@ReplaceEntity(OriginalTestEntity.class)
@Comment("Replacement entity that extends the original with additional fields")
public class ReplacedTestEntity extends OriginalTestEntity {

    @Column(name = "ADDITIONAL_FIELD")
    @Comment("Additional field only available in replacement")
    private String additionalField;

    public String getAdditionalField() {
        return additionalField;
    }

    public void setAdditionalField(String additionalField) {
        this.additionalField = additionalField;
    }
}