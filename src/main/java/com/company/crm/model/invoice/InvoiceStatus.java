package com.company.crm.model.invoice;

import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;


public enum InvoiceStatus implements EnumClass<Integer> {

    NEW(10),
    PENDING(20),
    OVERDUE(30),
    PAID(40);

    private final Integer id;

    InvoiceStatus(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    @Nullable
    public static InvoiceStatus fromId(Integer id) {
        for (InvoiceStatus at : InvoiceStatus.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}