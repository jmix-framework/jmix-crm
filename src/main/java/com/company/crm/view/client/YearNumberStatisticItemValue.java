package com.company.crm.view.client;

public class YearNumberStatisticItemValue {

    private final int year;
    private final Number statValue;

    public YearNumberStatisticItemValue(int year, Number statValue) {
        this.year = year;
        this.statValue = statValue;
    }

    public int getYear() {
        return year;
    }

    public Number getStatValue() {
        return statValue;
    }
}
