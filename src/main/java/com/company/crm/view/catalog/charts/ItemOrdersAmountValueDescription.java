package com.company.crm.view.catalog.charts;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.math.BigDecimal;
import java.util.Objects;

public class ItemOrdersAmountValueDescription {
    private final String item;
    private final BigDecimal amount;

    public ItemOrdersAmountValueDescription(String itemName, BigDecimal ordersAmount) {
        this.item = itemName;
        this.amount = ordersAmount;
    }

    public String getItem() {
        return item;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ItemOrdersAmountValueDescription) {
            return EqualsBuilder.reflectionEquals(this, obj);
        } else  {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, amount);
    }
}
