package com.company.crm.ai.model;

import com.company.crm.app.util.enums.EnumUtils;
import com.company.crm.model.base.DefaultStringEnumClass;
import org.springframework.lang.Nullable;

// TODO: rename zu AiAttachmentOrigin
public enum AiAttachmentType implements DefaultStringEnumClass<AiAttachmentType> {

    AI_GENERATED,
    USER_UPLOADED;

    @Nullable
    public static AiAttachmentType fromId(String id) {
        return EnumUtils.fromId(AiAttachmentType.class, id);
    }
}
