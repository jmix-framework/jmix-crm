package com.company.crm.util;

import io.jmix.core.UuidProvider;

public final class UniqueValues {

    public static long millis() {
        return System.currentTimeMillis();
    }

    public static String string() {
        return UuidProvider.createUuidV7().toString();
    }

    private UniqueValues() {
    }
}
