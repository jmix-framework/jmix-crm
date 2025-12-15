package com.company.crm.model.order;

import com.company.crm.app.util.enums.EnumUtils;
import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum OrderStatus implements EnumClass<Integer> {

    NEW(10),
    ACCEPTED(20),
    IN_PROGRESS(30),
    DONE(40);

    private final Integer id;

    OrderStatus(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static OrderStatus fromId(Integer id) {
        return EnumUtils.fromId(OrderStatus.class, id);
    }

    public static OrderStatus fromStringId(String id) {
        try {
            return fromId(Integer.valueOf(id));
        } catch (Exception ignored) {
        }
        return null;
    }
}