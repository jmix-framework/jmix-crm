package com.company.crm.report.config;

import java.math.BigDecimal;

/**
 * Configuration class containing all threshold values and constants used in Client360Report generation.
 * Centralizes hardcoded values that were previously scattered throughout the report implementation.
 */
public class ClientReportThresholds {

    /**
     * Outstanding balance threshold for flagging clients with significant debt.
     * Clients with outstanding balance above this amount are flagged for attention.
     */
    public static final BigDecimal OUTSTANDING_BALANCE_FLAG = BigDecimal.valueOf(1000);

    /**
     * Number of days since client creation to consider them a long-term customer.
     * Clients created more than this many days ago are marked as long-term customers.
     */
    public static final int LONG_TERM_CUSTOMER_DAYS = 365;

    /**
     * Number of days since last transaction to consider recent activity.
     * Clients with transactions within this period are marked as having recent activity.
     */
    public static final int RECENT_ACTIVITY_DAYS = 30;

    /**
     * Number of days to look back when loading recent activities for the report.
     * Controls how far back in time to search for activities to display.
     */
    public static final int RECENT_ACTIVITIES_DAYS_RANGE = 7;

    /**
     * Maximum number of activities to load per day when gathering recent activities.
     * Helps limit memory usage and report size for very active clients.
     */
    public static final int ACTIVITIES_PER_DAY = 5;

    /**
     * Absolute maximum number of recent activities to include in the report.
     * Final cap to prevent reports from becoming too large regardless of date range.
     */
    public static final int MAX_RECENT_ACTIVITIES = 20;

    // Private constructor to prevent instantiation
    private ClientReportThresholds() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}