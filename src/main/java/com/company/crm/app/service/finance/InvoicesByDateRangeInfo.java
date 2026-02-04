package com.company.crm.app.service.finance;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.invoice.InvoiceStatus;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class InvoicesByDateRangeInfo {

    private final LocalDate date;
    private final LocalDateRange dateRange;
    private final InvoiceStatus status;
    private final Long amount;

    public InvoicesByDateRangeInfo(LocalDate date, LocalDateRange dateRange, InvoiceStatus status, Long amount) {
        this.date = date;
        this.dateRange = dateRange;
        this.status = status;
        this.amount = amount;
    }

    public String getMonth() {
        return date.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
    }

    public int getYear() {
        return date.getYear();
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalDateRange getDateRange() {
        return dateRange;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Long getAmount() {
        return amount;
    }
}
