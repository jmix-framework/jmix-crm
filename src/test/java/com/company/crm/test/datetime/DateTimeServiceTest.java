package com.company.crm.test.datetime;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.datetime.DateTimeService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeServiceTest extends AbstractServiceTest<DateTimeService> {

    @Test
    void currentDayRange_hasValidBounds() {
        var range = service.getCurrentDayRange();

        assertThat(range.startDate()).isEqualTo(range.endDate());
        assertThat(range.startDate()).isEqualTo(LocalDate.now(service.getTimeZoneForCurrentUser().toZoneId()));
    }

    @Test
    void toOffsetDateTime_preservesDate() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        var offsetDateTime = service.toOffsetDateTime(date);

        assertThat(offsetDateTime.toLocalDate()).isEqualTo(date);
    }

    @Test
    void getStartOfDay_returnsTruncatedTime() {
        var now = service.now();
        var startOfDay = service.getStartOfDay(now);

        assertThat(startOfDay.getHour()).isZero();
        assertThat(startOfDay.getMinute()).isZero();
        assertThat(startOfDay.getSecond()).isZero();
    }

    @Test
    void getEndOfDay_returnsEndOfDayTime() {
        var now = service.now();
        var startOfDay = service.getStartOfDay(now);
        var endOfDay = service.getEndOfDay(startOfDay);

        assertThat(endOfDay.getHour()).isEqualTo(23);
        assertThat(endOfDay.getMinute()).isEqualTo(59);
        assertThat(endOfDay.getSecond()).isEqualTo(59);
    }

    @Test
    void getCurrentMonthRange_hasValidBounds() {
        var range = service.getCurrentMonthRange();
        LocalDate now = LocalDate.now(service.getTimeZoneForCurrentUser().toZoneId());

        assertThat(range.startDate()).isEqualTo(now.withDayOfMonth(1));
        assertThat(range.endDate()).isEqualTo(now.withDayOfMonth(now.getMonth().length(now.isLeapYear())));
    }
}
