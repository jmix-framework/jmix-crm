package com.company.crm.app.util.enums;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public final class EnumUtils {

    @Nullable
    public static <T, E extends EnumClass<T>> E fromId(Class<E> enumClass, @Nullable T id) {
        if (id == null) {
            return null;
        }

        for (E enumValue : enumClass.getEnumConstants()) {
            if (id.equals(enumValue.getId())) {
                return enumValue;
            }
        }

        return null;
    }

    private EnumUtils() {
    }
}
