package com.company.crm.app.util.demo;

import com.company.crm.app.util.common.ThreadUtils;

public final class DemoUtils {

    public static void defaultSleepForSearchClient() {
        sleep(1_000);
    }

    public static void defaultSleepForStatisticLoading() {
        sleep(2_000);
    }

    public static void sleep(long millis) {
        ThreadUtils.trySleep(millis);
    }

    private DemoUtils() {
    }
}
