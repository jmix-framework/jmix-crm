package com.company.crm.app.util.date.range;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record LocalDateRange(LocalDate startDate, LocalDate endDate) implements DateRange {

    public static LocalDateRange from(LocalDate startDate, LocalDate endDate) {
        return new LocalDateRange(startDate, endDate);
    }

    public LocalDateRange(LocalDate dayRange) {
        this(dayRange, dayRange);
    }

    public OffsetDateTimeRange asOffsetDateTimeRange() {
        var zoneId = ZoneId.systemDefault();
        var offsetStartDate = startDate.atStartOfDay(zoneId).toOffsetDateTime();
        var offsetEndDate = endDate.atStartOfDay(zoneId).toOffsetDateTime();

        if (startDate.equals(endDate)) {
            offsetEndDate = endOfDay(offsetEndDate);
        }

        return new OffsetDateTimeRange(offsetStartDate, offsetEndDate);
    }

    private static OffsetDateTime endOfDay(OffsetDateTime offsetEndDate) {
        offsetEndDate = offsetEndDate.withHour(23).withMinute(59).withSecond(59);
        return offsetEndDate;
    }
}
