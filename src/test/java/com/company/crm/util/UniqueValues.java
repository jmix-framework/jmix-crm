package com.company.crm.util;

import io.jmix.core.UuidProvider;

import java.util.UUID;

public final class UniqueValues {

    public static UUID uuid() {
        return UuidProvider.createUuidV7();
    }

    public static long millis() {
        return System.currentTimeMillis();
    }

    public static String string() {
        return string(StringVariant.UUID);
    }

    public static String string(StringVariant variant) {
        return switch (variant) {
            case UUID -> uuid().toString();
            case MILLIS -> String.valueOf(millis());
        };
    }

    public enum StringVariant {
        UUID, MILLIS
    }

    private UniqueValues() {
    }
}
