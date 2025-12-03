package com.company.crm.view.client.charts.buyers;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.math.BigDecimal;
import java.util.Objects;

public class ClientPurchasesSumValueDescription {
    private final String clientName;
    private final BigDecimal purchasesSum;

    public ClientPurchasesSumValueDescription(String clientName, BigDecimal ordersAmount) {
        this.clientName = clientName;
        this.purchasesSum = ordersAmount;
    }

    public String getClientName() {
        return clientName;
    }

    public BigDecimal getPurchasesSum() {
        return purchasesSum;
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientName, purchasesSum);
    }
}
