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
     *
     * @param params Report parameters containing required "fromDate" and "toDate"
     * @return LocalDateRange with the provided date range
     */
    public static LocalDateRange getDateRangeFromParams(Map<String, Object> params) {
        LocalDate fromDate = convertToLocalDate(params.get("fromDate"));
        LocalDate toDate = convertToLocalDate(params.get("toDate"));
        return new LocalDateRange(fromDate, toDate);
    }

    /**
     * Converts a report date parameter to LocalDate using the system default time zone for legacy dates.
     *
     * @param date LocalDate or Date object to convert
     * @return LocalDate or null if input is null
     */
    public static LocalDate convertToLocalDate(Object date) {
        if (date == null) {
            return null;
        }
        if (date instanceof LocalDate localDate) {
            return localDate;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (date instanceof Date legacyDate) {
            return legacyDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        throw new IllegalArgumentException("Unsupported report date type: " + date.getClass().getName());
    }
}
