package com.company.crm.app.util.demo;

import com.company.crm.app.util.common.ThreadUtils;

public final class DemoUtils {

    public static void defaultSleepForClientsSearching() {
        sleep(1_000);
    }

    public static void defaultSleepForStatisticsLoading() {
        sleep(1_000);
    }

    public static void sleep(long millis) {
        ThreadUtils.trySleep(millis);
    }

    private DemoUtils() {
    }
}
