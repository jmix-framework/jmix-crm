package com.company.crm.app.util.date.range;

import org.jspecify.annotations.NonNull;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record LocalDateRange(LocalDate startDate, LocalDate endDate) implements DateRange {

    public LocalDateRange(LocalDate dayRange) {
        this(dayRange, dayRange);
    }

    public OffsetDateTimeRange asOffsetDateTimeRange() {
        var offsetStartDate = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        var offsetEndDate = endDate.atStartOfDay().atOffset(ZoneOffset.UTC);

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
