# One-time data fix

Part of `jmix-create-liquibase-changelog`. Read this when the change needs a one-time data fix.

A data fix for a schema change (backfilling a new column, clearing a timestamp
so a job processes every row again) is an ordinary `<update>` changeset.

```xml
<!-- literal value -->
<changeSet id="3" author="app">
    <update tableName="CUSTOMER">
        <column name="STATUS" value="PENDING"/>
    </update>
</changeSet>

<!-- set to NULL: a <column> with NO value attribute at all -->
<changeSet id="4" author="app">
    <update tableName="CUSTOMER">
        <column name="LAST_CONTACTED_AT"/>
        <column name="LAST_INVOICED_AT"/>
    </update>
</changeSet>
```

- `value=""` is not NULL: on most databases it writes an empty string, and on a
  non-text column it fails. To write NULL, omit every `value` / `valueXxx`
  attribute.
- Add a `<where>` clause when the fix targets a subset of rows. Without one it
  updates every row, which is usually what a one-time reset wants.
