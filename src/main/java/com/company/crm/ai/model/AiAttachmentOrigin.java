package com.company.crm.ai.model;

import com.company.crm.app.util.enums.EnumUtils;
import com.company.crm.model.base.DefaultStringEnumClass;
import org.springframework.lang.Nullable;

public enum AiAttachmentOrigin implements DefaultStringEnumClass<AiAttachmentOrigin> {

    AI_GENERATED,
    USER_UPLOADED;

    @Nullable
    public static AiAttachmentOrigin fromId(String id) {
        return EnumUtils.fromId(AiAttachmentOrigin.class, id);
    }
}
