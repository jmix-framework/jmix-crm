package com.company.crm.ai.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum AiAttachmentType implements EnumClass<String> {

    AI_GENERATED("AI_GENERATED"),
    USER_UPLOADED("USER_UPLOADED");

    private final String id;

    AiAttachmentType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static AiAttachmentType fromId(String id) {
        for (AiAttachmentType at : AiAttachmentType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}
