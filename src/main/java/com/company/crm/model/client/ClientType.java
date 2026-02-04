package com.company.crm.model.client;

import com.company.crm.app.util.enums.EnumUtils;
import com.company.crm.model.base.DefaultStringEnumClass;

public enum ClientType implements DefaultStringEnumClass<ClientType> {
    BUSINESS,
    INDIVIDUAL;

    static ClientType fromId(String id) {
        return EnumUtils.fromId(ClientType.class, id);
    }
}