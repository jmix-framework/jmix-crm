package com.company.crm.app.util.date.range;

import java.time.OffsetDateTime;

public record OffsetDateTimeRange(OffsetDateTime startDate, OffsetDateTime endDate) implements DateRange {

    public static OffsetDateTimeRange from(OffsetDateTime startDate, OffsetDateTime endDate) {
        return new OffsetDateTimeRange(startDate, endDate);
    }
}
