package com.company.crm.app.service.datetime;

import com.company.crm.app.util.date.range.LocalDateRange;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import org.jspecify.annotations.Nullable;
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

    // common
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

    // current time ranges
    public LocalDateRange getCurrentDayRange() {
        return new LocalDateRange(getCurrentDayStart().toLocalDate(), getCurrentDayEnd().toLocalDate());
    }

    public LocalDateRange getCurrentWeekRange() {
        return new LocalDateRange(getCurrentWeekStart().toLocalDate(), getCurrentWeekEnd().toLocalDate());
    }

    public LocalDateRange getCurrentMonthRange() {
        return new LocalDateRange(getCurrentMonthStart().toLocalDate(), getCurrentMonthEnd().toLocalDate());
    }

    public LocalDateRange getCurrentYearRange() {
        return new LocalDateRange(getCurrentYearStart().toLocalDate(), getCurrentYearEnd().toLocalDate());
    }

    // start of time
    public OffsetDateTime getCurrentDayStart() {
        return getStartOfDay(getTimeForCurrentUser());
    }

    public OffsetDateTime getCurrentWeekStart() {
        return getStartOfWeek(getTimeForCurrentUser());
    }

    public OffsetDateTime getCurrentMonthStart() {
        return getStartOfMonth(getTimeForCurrentUser());
    }

    public OffsetDateTime getCurrentYearStart() {
        return getStartOfYear(getTimeForCurrentUser());
    }

    public OffsetDateTime getStartOfDay(OffsetDateTime timeForCurrentUser) {
        return timeForCurrentUser.truncatedTo(ChronoUnit.DAYS);
    }

    public OffsetDateTime getStartOfWeek(OffsetDateTime currentTIme) {
        return getStartOfDay(currentTIme.with(previousOrSame(DayOfWeek.MONDAY)));
    }

    public OffsetDateTime getStartOfMonth(OffsetDateTime currentTime) {
        return getStartOfDay(currentTime.withDayOfMonth(1));
    }

    public OffsetDateTime getStartOfYear(OffsetDateTime currentTime) {
        return getStartOfDay(currentTime.withDayOfYear(1));
    }

    // end of time
    public OffsetDateTime getCurrentDayEnd() {
        return getEndOfDay(getCurrentDayStart());
    }

    public OffsetDateTime getCurrentWeekEnd() {
        return getEndOfWeek(getTimeForCurrentUser());
    }

    public OffsetDateTime getCurrentMonthEnd() {
        return getEndOfMonth(getTimeForCurrentUser());
    }

    public OffsetDateTime getCurrentYearEnd() {
        return getEndOfYear(getTimeForCurrentUser());
    }

    public OffsetDateTime getEndOfDay(OffsetDateTime currentDayStart) {
        return currentDayStart.withHour(23).withMinute(59).withSecond(59);
    }

    public OffsetDateTime getEndOfWeek(OffsetDateTime currentTime) {
        return getEndOfDay(currentTime.with(nextOrSame(DayOfWeek.SUNDAY)));
    }

    public OffsetDateTime getEndOfMonth(OffsetDateTime currentDate) {
        var daysInMonth = currentDate.getMonth().length(Year.of(currentDate.getYear()).isLeap());
        return getEndOfDay(currentDate.withDayOfMonth(daysInMonth));
    }

    public OffsetDateTime getEndOfYear(OffsetDateTime currentDate) {
        var daysInYear = Year.of(currentDate.getYear()).isLeap() ? 366 : 365;
        return getEndOfDay(currentDate.withDayOfYear(daysInYear));
    }
}
