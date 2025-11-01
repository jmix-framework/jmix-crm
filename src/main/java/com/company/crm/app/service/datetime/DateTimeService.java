package com.company.crm.app.service.datetime;

import io.jmix.core.security.CurrentAuthentication;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import static java.time.temporal.TemporalAdjusters.nextOrSame;
import static java.time.temporal.TemporalAdjusters.previousOrSame;

@Service
public class DateTimeService {

    public static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone(ZoneOffset.UTC);

    private final CurrentAuthentication currentAuthentication;

    public DateTimeService(CurrentAuthentication currentAuthentication) {
        this.currentAuthentication = currentAuthentication;
    }

    public OffsetDateTime toOffsetDateTime(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    /**
     * Получить дату с использованием формата "dd.MM.yy HH:mm".
     */
    public String defaultFormat(OffsetDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"));
    }

    /**
     * Получить время с использованием формата "HH:mm".
     */
    public String defaultTimeFormat(OffsetDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public OffsetDateTime getTimeForCurrentUser() {
        return transformForCurrentUser(now());
    }

    public TimeZone getTimeZoneForCurrentUser() {
        return currentAuthentication.getTimeZone();
    }

    public OffsetDateTime transformForCurrentUser(OffsetDateTime dateTime) {
        ZoneOffset offset = getTimeZoneForCurrentUser().toZoneId().getRules().getOffset(Instant.now());
        return dateTime != null ? dateTime.withOffsetSameInstant(offset) : now().withOffsetSameInstant(offset);
    }

    public OffsetDateTime fromUnixSeconds(Long unixSeconds) {
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(unixSeconds), UTC_TIME_ZONE.toZoneId());
    }

    public long toUnixSeconds(OffsetDateTime offsetDateTime) {
        return offsetDateTime.toEpochSecond();
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
