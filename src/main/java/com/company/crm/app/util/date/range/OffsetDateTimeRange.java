package com.company.crm.app.util.date.range;

import java.time.OffsetDateTime;

public record OffsetDateTimeRange(OffsetDateTime startDate, OffsetDateTime endDate) implements DateRange {

    public LocalDateRange asLocalDateRange() {
        return new LocalDateRange(startDate.toLocalDate(), endDate.toLocalDate());
    }
}
