package com.company.crm.view.invoice.charts;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

public class InvoiceStatusAmountValueDescription {
    private final String status;
    private final Long amount;

    public InvoiceStatusAmountValueDescription(String status, Long amount) {
        this.status = status;
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public Long getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InvoiceStatusAmountValueDescription) {
            return EqualsBuilder.reflectionEquals(this, obj);
        } else  {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, amount);
    }
}
