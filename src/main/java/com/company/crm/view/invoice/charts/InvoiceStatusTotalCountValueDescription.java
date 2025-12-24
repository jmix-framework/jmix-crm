package com.company.crm.view.invoice.charts;

public class InvoiceStatusTotalCountValueDescription {

    private final String status;
    private final Long count;

    public InvoiceStatusTotalCountValueDescription(String status, Long count) {
        this.status = status;
        this.count = count;
    }

    public String getStatus() {
        return status;
    }

    public Long getCount() {
        return count;
    }
}
