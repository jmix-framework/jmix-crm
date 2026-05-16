package com.company.crm.ai.model;

import com.company.crm.app.util.enums.EnumUtils;
import com.company.crm.model.base.DefaultStringEnumClass;
import org.springframework.lang.Nullable;

public enum ChatMessageType implements DefaultStringEnumClass<ChatMessageType> {

    USER,
    ASSISTANT,
    SYSTEM,
    TOOL;

    @Nullable
    public static ChatMessageType fromId(String id) {
        return EnumUtils.fromId(ChatMessageType.class, id);
    }
}
