package com.company.crm.app.util.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThreadUtils {

    private static final Logger log = LoggerFactory.getLogger(ThreadUtils.class);

    public static void trySleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.warn("Sleep has been interrupted", e);
        }
    }

    private ThreadUtils() {
    }
}
