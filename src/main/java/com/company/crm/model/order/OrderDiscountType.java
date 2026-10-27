package com.company.crm.model.order;

import com.company.crm.app.util.enums.EnumUtils;
import io.jmix.core.metamodel.datatype.EnumClass;
import org.jspecify.annotations.Nullable;

public enum OrderDiscountType implements EnumClass<String> {
    PERCENT("PERCENT"),
    AMOUNT("AMOUNT");

    private final String id;

    OrderDiscountType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static OrderDiscountType fromId(@Nullable String id) {
        return EnumUtils.fromId(OrderDiscountType.class, id);
    }
}
