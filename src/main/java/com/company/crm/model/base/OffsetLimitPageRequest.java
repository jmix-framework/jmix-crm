package com.company.crm.model.base;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import java.io.Serializable;

public record OffsetLimitPageRequest(long offset, long limit, @Nullable Sort sort) implements Pageable, Serializable {

    public static OffsetLimitPageRequest of(long offset, long limit) {
        return new OffsetLimitPageRequest(offset, limit);
    }

    /**
     * Creates a new {@link OffsetLimitPageRequest} with sort parameters applied.
     *
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     * @param sort   can be {@code null}.
     */
    public OffsetLimitPageRequest {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset index must not be less than zero!");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one!");
        }
    }

    /**
     * Creates a new {@link OffsetLimitPageRequest} with sort parameters applied.
     *
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     */
    public OffsetLimitPageRequest(long offset, long limit) {
        this(offset, limit, null);
    }

    /**
     * Creates a new {@link OffsetLimitPageRequest} with sort parameters applied.
     *
     * @param offset     zero-based offset.
     * @param limit      the size of the elements to be returned.
     * @param direction  the direction of the {@link Sort} to be specified, can be {@literal null}.
     * @param properties the properties to sort by, must not be {@literal null} or empty.
     */
    public OffsetLimitPageRequest(int offset, int limit, Sort.Direction direction, String... properties) {
        this(offset, limit, Sort.by(direction, properties));
    }

    /**
     * Creates a new {@link OffsetLimitPageRequest} with sort parameters applied.
     *
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     */
    public OffsetLimitPageRequest(int offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    @Override
    public int getPageNumber() {
        return Math.toIntExact(offset / limit);
    }

    @Override
    public int getPageSize() {
        return Math.toIntExact(limit);
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort == null ? Sort.unsorted() : sort;
    }

    @Override
    public Pageable next() {
        return new OffsetLimitPageRequest(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    public OffsetLimitPageRequest previous() {
        return hasPrevious() ? new OffsetLimitPageRequest(getOffset() - getPageSize(), getPageSize(), getSort()) : this;
    }


    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new OffsetLimitPageRequest(0, getPageSize(), getSort());
    }

    @Override
    public Pageable withPage(int pageNumber) {
        throw new UnsupportedOperationException("Page number is not supported in %s".formatted(getClass().getSimpleName()));
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof OffsetLimitPageRequest(long offset1, long limit1, Sort sort1))) return false;

        return new EqualsBuilder()
                .append(limit, limit1)
                .append(offset, offset1)
                .append(sort, sort1)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(limit)
                .append(offset)
                .append(sort)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("limit", limit)
                .append("offset", offset)
                .append("sort", sort)
                .toString();
    }
}