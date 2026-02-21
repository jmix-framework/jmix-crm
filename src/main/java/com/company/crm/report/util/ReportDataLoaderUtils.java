package com.company.crm.report.util;

import com.company.crm.app.util.date.range.LocalDateRange;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

/**
 * Utility class for common functionality shared across DataLoader beans.
 * Provides consistent date conversion and parameter extraction methods.
 */
public class ReportDataLoaderUtils {

    /**
     * Extracts and converts date parameters from report parameters to LocalDateRange.
     * @param params Report parameters containing required "fromDate" and "toDate"
     * @return LocalDateRange with the provided date range
     */
    public static LocalDateRange getDateRangeFromParams(Map<String, Object> params) {
        LocalDate fromDate = convertToLocalDate((Date) params.get("fromDate"));
        LocalDate toDate = convertToLocalDate((Date) params.get("toDate"));
        return new LocalDateRange(fromDate, toDate);
    }

    /**
     * Converts a Date object to LocalDate using the system default time zone.
     * @param date Date object to convert
     * @return LocalDate or null if input is null
     */
    public static LocalDate convertToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}