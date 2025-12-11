package com.company.crm.app.util.ui.component.card;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.util.date.DateRange;
import com.company.crm.model.base.DefaultStringEnumClass;
import io.jmix.core.common.datastruct.Pair;

import java.time.LocalDate;

public enum CardPeriod implements DefaultStringEnumClass<CardPeriod> {
    WEEK,
    MONTH,
    YEAR;

    public DateRange getDateRange(DateTimeService dateTimeService) {
        switch (this) {
            case WEEK -> {
                return new DateRange(
                        dateTimeService.getWeekStart().toLocalDate(),
                        dateTimeService.getWeekEnd().toLocalDate()
                );
            }
            case MONTH -> {
                return new DateRange(
                        dateTimeService.getMonthStart().toLocalDate(),
                        dateTimeService.getMonthEnd().toLocalDate()
                );
            }
            case YEAR -> {
                return new DateRange(
                        dateTimeService.getYearStart().toLocalDate(),
                        dateTimeService.getYearEnd().toLocalDate()
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + this);
        }
    }

    public DateRange getPreviousDateRangeFor(DateRange range) {
        var startDate = range.startDate();
        var endDate = range.endDate();

        var previousPeriodStart = switch (this) {
            case WEEK -> startDate.minusWeeks(1);
            case MONTH -> startDate.minusMonths(1);
            case YEAR -> startDate.minusYears(1);
        };

        var previousPeriodEnd = switch (this) {
            case WEEK -> endDate.minusWeeks(1);
            case MONTH -> endDate.minusMonths(1);
            case YEAR -> endDate.minusYears(1);
        };

        return new DateRange(previousPeriodStart, previousPeriodEnd);
    }
}
