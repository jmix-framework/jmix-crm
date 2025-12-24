package com.company.crm.view.home;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

public class OrderStatusAmountValueDescription {
    private final String status;
    private final Integer amount;

    public OrderStatusAmountValueDescription(String status, Integer amount) {
        this.status = status;
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public Integer getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OrderStatusAmountValueDescription) {
            return EqualsBuilder.reflectionEquals(this, obj);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, amount);
    }
}
