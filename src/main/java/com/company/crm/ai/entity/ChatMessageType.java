package com.company.crm.ai.entity;

import io.jmix.core.metamodel.datatype.EnumClass;

import org.springframework.lang.Nullable;


public enum ChatMessageType implements EnumClass<String> {

    USER("USER"),
    ASSISTANT("ASSISTANT"),
    SYSTEM("SYSTEM"),
    TOOL("TOOL");

    private final String id;

    ChatMessageType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static ChatMessageType fromId(String id) {
        for (ChatMessageType at : ChatMessageType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}