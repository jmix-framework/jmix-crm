# Unique columns

Part of `jmix-create-entity`. Read this when a column or a set of columns must be unique.

For a single-column natural key, add `unique = true` to the `@Index` and mirror it with
`unique="true"` on the `<createIndex>`:

```java
@Table(name = "DEPARTMENT", indexes = @Index(
        name = "IDX_DEPARTMENT_ON_NAME", columnList = "NAME", unique = true))
```

```xml
<createIndex indexName="IDX_DEPARTMENT_ON_NAME" tableName="DEPARTMENT" unique="true">
    <column name="NAME"/>
</createIndex>
```

For a rule over several columns, use a unique constraint rather than a unique index —
`@Table(uniqueConstraints = @UniqueConstraint(name = "UK_ORDER_LINE",
columnNames = {"ORDER_ID", "PRODUCT_ID"}))` paired with `<addUniqueConstraint
tableName="ORDER_LINE" columnNames="ORDER_ID, PRODUCT_ID"
constraintName="UK_ORDER_LINE"/>`. Same name on both sides.

Case-insensitive uniqueness cannot be declared with `@Index`/`@UniqueConstraint` at all: it
needs a functional index on `lower(column)` and dialect-specific SQL, so decide it at design
time, not when writing the changelog.
