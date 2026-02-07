package com.company.crm.model.catalog.item;

import com.company.crm.app.util.enums.EnumUtils;
import com.company.crm.model.base.DefaultStringEnumClass;

public enum UomType implements DefaultStringEnumClass<UomType> {
    PIECES,
    KILOGRAM,
    LITER,
    METER;

    static UomType fromId(String id) {
        return EnumUtils.fromId(UomType.class, id);
    }
}