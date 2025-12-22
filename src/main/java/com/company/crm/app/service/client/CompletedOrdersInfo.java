package com.company.crm.app.service.client;

import com.company.crm.app.util.date.range.LocalDateRange;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;

public final class CompletedOrdersInfo {

    // fields
    private final LocalDate date;
    private final LocalDateRange range;
    private final Long dateOrders;
    private final Long rangeOrders;
    private final BigDecimal dateTotal;
    private final BigDecimal rangeTotal;
    private final Integer rangeSalesLifeCycleLength;

    public CompletedOrdersInfo(LocalDate date, LocalDateRange range,
                               Long dateOrders, BigDecimal dateTotal,
                               Long rangeOrders, BigDecimal rangeTotal,
                               Integer rangeSalesLifeCycleLength) {
        this.date = date;
        this.range = range;

        this.dateTotal = dateTotal;
        this.dateOrders = dateOrders;

        this.rangeOrders = rangeOrders;
        this.rangeTotal = rangeTotal;

        this.rangeSalesLifeCycleLength = rangeSalesLifeCycleLength;
    }

    public LocalDate getDate() {
        return date;
    }

    public Year getYear() {
        return Year.of(date.getYear());
    }

    public Month getMonth() {
        return date.getMonth();
    }

    public Long getRangeOrders() {
        return rangeOrders;
    }

    public BigDecimal getRangeTotal() {
        return rangeTotal;
    }

    public LocalDateRange getRange() {
        return range;
    }

    public int getYearInt() {
        return date.getYear();
    }

    public int getMonthInt() {
        return date.getMonthValue();
    }

    public BigDecimal getDateAverageBill() {
        return getAverageBill(dateTotal, dateOrders);
    }

    public BigDecimal getRangeAverageBill() {
        return getAverageBill(rangeTotal, rangeOrders);
    }

    private BigDecimal getAverageBill(BigDecimal totalSum, Long ordersAmount) {
        if (totalSum == null || ordersAmount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal avgBill = totalSum.divide(BigDecimal.valueOf(ordersAmount), RoundingMode.HALF_UP);
        if (avgBill.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return avgBill;
    }

    public Long getDateOrders() {
        return dateOrders;
    }

    public BigDecimal getDateTotal() {
        return dateTotal;
    }

    public Integer getRangeSalesLifeCycleLength() {
        return rangeSalesLifeCycleLength;
    }

}
