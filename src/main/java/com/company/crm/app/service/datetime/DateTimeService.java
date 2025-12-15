package com.company.crm.app.service.datetime;

import com.company.crm.app.util.date.range.LocalDateRange;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.TimeZone;

import static com.company.crm.app.util.ui.CrmUiUtils.getCurrentUI;
import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

@Service
public class DateTimeService {

    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    private final TimeSource timeSource;
    private final CurrentAuthentication currentAuthentication;

    public DateTimeService(CurrentAuthentication currentAuthentication, TimeSource timeSource) {
        this.currentAuthentication = currentAuthentication;
        this.timeSource = timeSource;
    }

    public OffsetDateTime now() {
        return timeSource.now().toOffsetDateTime();
    }

    public long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }

    public OffsetDateTime getTimeForCurrentUser() {
        return transformForCurrentUser(now());
    }

    public TimeZone getTimeZoneForCurrentUser() {
        return getCurrentUI().isPresent()
                ? currentAuthentication.getTimeZone()
                : UTC_TIME_ZONE;
    }

    public OffsetDateTime transformForCurrentUser(@Nullable OffsetDateTime dateTime) {
        ZoneOffset zoneOffset = getTimeZoneForCurrentUser().toZoneId().getRules().getOffset(Instant.now());
        return dateTime != null
                ? dateTime.withOffsetSameInstant(zoneOffset)
                : now().withOffsetSameInstant(zoneOffset);
    }

    public OffsetDateTime toOffsetDateTime(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    public LocalDateRange getCurrentDayRange() {
        return new LocalDateRange(getDayStart().toLocalDate(), getDayEnd().toLocalDate());
    }

    public LocalDateRange getCurrentWeekRange() {
        return new LocalDateRange(getWeekStart().toLocalDate(), getWeekEnd().toLocalDate());
    }

    public LocalDateRange getCurrentMonthRange() {
        return new LocalDateRange(getMonthStart().toLocalDate(), getMonthEnd().toLocalDate());
    }

    public LocalDateRange getCurrentYearRange() {
        return new LocalDateRange(getYearStart().toLocalDate(), getYearEnd().toLocalDate());
    }

    public OffsetDateTime getDayStart() {
        return getTimeForCurrentUser().truncatedTo(ChronoUnit.DAYS);
    }

    public OffsetDateTime getDayEnd() {
        return getDayStart().withHour(23).withMinute(59).withSecond(59);
    }

    public OffsetDateTime getWeekStart() {
        return getTimeForCurrentUser().with(previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
    }

    public OffsetDateTime getWeekEnd() {
        return getTimeForCurrentUser().with(nextOrSame(DayOfWeek.SUNDAY)).withHour(23).withMinute(59).withSecond(59);
    }

    public OffsetDateTime getMonthStart() {
        return getTimeForCurrentUser().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
    }

    public OffsetDateTime getMonthEnd() {
        var currentDate = getTimeForCurrentUser();
        var daysInMonth = currentDate.getMonth().length(Year.of(currentDate.getYear()).isLeap());
        return currentDate.withDayOfMonth(daysInMonth).withHour(23).withMinute(59).withSecond(59);
    }

    public OffsetDateTime getYearStart() {
        return getTimeForCurrentUser().withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
    }

    public OffsetDateTime getYearEnd() {
        var currentDate = getTimeForCurrentUser();
        var daysInYear = Year.of(currentDate.getYear()).isLeap() ? 366 : 365;
        return currentDate.withDayOfYear(daysInYear).withHour(23).withMinute(59).withSecond(59);
    }
}
