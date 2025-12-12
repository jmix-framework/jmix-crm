package com.company.crm.app.util.date.range;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record LocalDateRange(LocalDate startDate, LocalDate endDate) implements DateRange {

    public OffsetDateTimeRange asOffsetDateTimeRange() {
        OffsetDateTime offsetStartDate = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime offsetEndDate = endDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        return new OffsetDateTimeRange(offsetStartDate, offsetEndDate);
    }
}
