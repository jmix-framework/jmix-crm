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
}
