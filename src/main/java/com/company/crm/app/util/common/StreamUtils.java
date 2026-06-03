package com.company.crm.app.util.common;

import java.util.Collection;
import java.util.stream.Stream;

public final class StreamUtils {

    private StreamUtils() {
    }

    /**
     * Returns a sequential {@link Stream} with the specified collection as its source.
     * If the collection is {@code null}, an empty stream is returned.
     *
     * @param <T>        the type of collection elements
     * @param collection the collection to stream, may be null
     * @return a sequential {@code Stream}, or an empty stream if the collection is null
     */
    public static <T> Stream<T> safeStream(Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }
}
