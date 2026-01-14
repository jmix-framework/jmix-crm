package com.company.crm.app.util.date;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.base.DefaultStringEnumClass;

public enum Period implements DefaultStringEnumClass<Period> {
    WEEK,
    MONTH,
    YEAR;

    public LocalDateRange getDateRange(DateTimeService dateTimeService) {
        return switch (this) {
            case WEEK -> dateTimeService.getCurrentWeekRange();
            case MONTH -> dateTimeService.getCurrentMonthRange();
            case YEAR -> dateTimeService.getCurrentYearRange();
        };
    }

    public LocalDateRange getPreviousDateRangeFor(LocalDateRange range) {
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

        return new LocalDateRange(previousPeriodStart, previousPeriodEnd);
    }
}
