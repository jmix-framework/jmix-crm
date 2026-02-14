package com.company.crm.ai.jmix.testmodel;

import io.jmix.core.metamodel.datatype.EnumClass;

/**
 * Test enum for order status with ID values
 */
public enum TestOrderStatus implements EnumClass<Integer> {

    DRAFT(10),
    SUBMITTED(20),
    APPROVED(30),
    SHIPPED(40),
    DELIVERED(50),
    CANCELLED(99);

    private final Integer id;

    TestOrderStatus(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    public static TestOrderStatus fromId(Integer id) {
        for (TestOrderStatus status : TestOrderStatus.values()) {
            if (status.getId().equals(id)) {
                return status;
            }
        }
        return null;
    }
}