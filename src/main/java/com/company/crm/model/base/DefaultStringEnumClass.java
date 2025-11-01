package com.company.crm.model.base;

import io.jmix.core.metamodel.datatype.EnumClass;

public interface DefaultStringEnumClass<E extends Enum<?>> extends EnumClass<String> {
    @Override
    default String getId() {
        //noinspection unchecked
        return ((E) this).name();
    }
}