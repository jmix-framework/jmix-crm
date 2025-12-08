package com.company.crm.view.catalog.charts;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItemOrdersAmountValueDescription {
    private final String itemName;
    private final BigDecimal amount;

    public OrderItemOrdersAmountValueDescription(String itemName, BigDecimal ordersAmount) {
        this.itemName = itemName;
        this.amount = ordersAmount;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OrderItemOrdersAmountValueDescription) {
            return EqualsBuilder.reflectionEquals(this, obj);
        } else  {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemName, amount);
    }
}
