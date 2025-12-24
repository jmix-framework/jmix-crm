package com.company.crm.view.payment.charts;

import java.math.BigDecimal;

public class ClientTotalPaymentsValueDescription {

    private final String client;
    private final BigDecimal total;

    public ClientTotalPaymentsValueDescription(String client, BigDecimal total) {
        this.client = client;
        this.total = total;
    }

    public String getClient() {
        return client;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
