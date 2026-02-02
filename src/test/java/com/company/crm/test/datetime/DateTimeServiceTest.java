package com.company.crm.test.datetime;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.datetime.DateTimeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeServiceTest extends AbstractTest {

    @Autowired
    private DateTimeService dateTimeService;

    @Test
    void currentDayRange_hasValidBounds() {
        var range = dateTimeService.getCurrentDayRange();

        assertThat(range.startDate()).isEqualTo(range.endDate());
        assertThat(range.startDate()).isEqualTo(LocalDate.now(dateTimeService.getTimeZoneForCurrentUser().toZoneId()));
    }

    @Test
    void toOffsetDateTime_preservesDate() {
        LocalDate date = LocalDate.of(2026, 1, 15);
        var offsetDateTime = dateTimeService.toOffsetDateTime(date);

        assertThat(offsetDateTime.toLocalDate()).isEqualTo(date);
    }

    @Test
    void getStartOfDay_returnsTruncatedTime() {
        var now = dateTimeService.now();
        var startOfDay = dateTimeService.getStartOfDay(now);

        assertThat(startOfDay.getHour()).isZero();
        assertThat(startOfDay.getMinute()).isZero();
        assertThat(startOfDay.getSecond()).isZero();
    }

    @Test
    void getEndOfDay_returnsEndOfDayTime() {
        var now = dateTimeService.now();
        var startOfDay = dateTimeService.getStartOfDay(now);
        var endOfDay = dateTimeService.getEndOfDay(startOfDay);

        assertThat(endOfDay.getHour()).isEqualTo(23);
        assertThat(endOfDay.getMinute()).isEqualTo(59);
        assertThat(endOfDay.getSecond()).isEqualTo(59);
    }

    @Test
    void getCurrentMonthRange_hasValidBounds() {
        var range = dateTimeService.getCurrentMonthRange();
        LocalDate now = LocalDate.now(dateTimeService.getTimeZoneForCurrentUser().toZoneId());

        assertThat(range.startDate()).isEqualTo(now.withDayOfMonth(1));
        assertThat(range.endDate()).isEqualTo(now.withDayOfMonth(now.getMonth().length(now.isLeapYear())));
    }
}
