package com.company.crm.view.client.charts.type;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

public class ClientTypeAmountValueDescription {
    private final String type;
    private final Integer amount;

    public ClientTypeAmountValueDescription(String type, Integer amount) {
        this.type = type;
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, amount);
    }
}
