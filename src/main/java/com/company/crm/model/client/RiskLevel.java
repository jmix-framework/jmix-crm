package com.company.crm.model.client;

import com.company.crm.app.util.enums.EnumUtils;
import com.company.crm.model.base.DefaultStringEnumClass;
import org.jspecify.annotations.Nullable;

public enum RiskLevel implements DefaultStringEnumClass<RiskLevel> {

    HIGH,
    MEDIUM,
    LOW;

    @Nullable
    public static RiskLevel fromId(String id) {
        return EnumUtils.fromId(RiskLevel.class, id);
    }
}