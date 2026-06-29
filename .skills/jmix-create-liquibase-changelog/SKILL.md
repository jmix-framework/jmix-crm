---
name: jmix-create-liquibase-changelog
description: Create Liquibase changelogs that exactly match Jmix entity model changes.
---

# Create Liquibase Changelog

Use this skill for every persistent entity or schema change.

## Steps

1. Find the root changelog path from `application.properties`.
2. Follow the existing naming style: sequential files or date/time folders.
3. Create a new changelog file for the schema change.
4. Make sure it is reachable from the root `changelog.xml`: usually by placing it under the directory covered by the project's `<includeAll>`, or by adding an explicit `<include>` only when the project uses explicit includes.
5. Use a changeset id and author that match project style; do not reuse an id within the same changelog file.
6. Add `ID` and `VERSION` columns for standard Jmix entities.
7. Add every persistent entity field with exact type, length, precision, scale, and nullability.
8. Add foreign keys for references.
9. Add indexes and unique constraints required by the entity or domain.
10. Verify the table and column names match Java annotations.
11. Use only type macros already present in the project. If no macro exists, use the standard Liquibase type.

## Standard Types

| Java type | Liquibase type |
| --- | --- |
| UUID | `${uuid.type}` |
| String | `varchar(n)` |
| Integer | `int` |
| Long | `bigint` |
| BigDecimal | `decimal(p,s)` |
| Boolean | `boolean` |
| LocalDate | `date` |
| LocalDateTime | `timestamp` |
| Enum id string | `varchar(50)` |

## Entity Table Skeleton

```xml
<changeSet id="create-customer" author="app">
    <createTable tableName="CUSTOMER">
        <column name="ID" type="${uuid.type}">
            <constraints nullable="false" primaryKey="true" primaryKeyName="PK_CUSTOMER"/>
        </column>
        <column name="VERSION" type="int">
            <constraints nullable="false"/>
        </column>
        <column name="NAME" type="varchar(100)">
            <constraints nullable="false"/>
        </column>
    </createTable>
</changeSet>
```

## Audit and soft-delete columns

If the entity carries audit (`@CreatedBy` / `@CreatedDate` / `@LastModifiedBy` /
`@LastModifiedDate`) or soft-delete (`@DeletedBy` / `@DeletedDate`) annotations,
add the matching columns INSIDE its `<createTable>`. They are set by Jmix at
runtime, so keep them NULLABLE (no `nullable="false"`). Add only the columns
whose annotations are actually on the entity — see `jmix-create-entity`
(Auditing and Soft Delete).

```xml
<column name="CREATED_BY" type="varchar(255)"/>
<column name="CREATED_DATE" type="timestamp"/>
<column name="LAST_MODIFIED_BY" type="varchar(255)"/>
<column name="LAST_MODIFIED_DATE" type="timestamp"/>
<column name="DELETED_BY" type="varchar(255)"/>
<column name="DELETED_DATE" type="timestamp"/>
```

## Parent → child ordering (FK references must follow the parent table)

When a child table has a foreign key to a parent, the parent `createTable`
MUST come BEFORE the child's `createTable` / `addForeignKeyConstraint`. A
changeSet that references a table not yet created fails at startup and takes
down the whole context — including tests that only touch the data model.
Order the parent first, the child (with its FK) second:

```xml
<!-- parent FIRST -->
<changeSet id="create-parent" author="app">
    <createTable tableName="PARENT">
        <column name="ID" type="${uuid.type}">
            <constraints nullable="false" primaryKey="true" primaryKeyName="PK_PARENT"/>
        </column>
        <column name="VERSION" type="int"><constraints nullable="false"/></column>
        <column name="NAME" type="varchar(100)"><constraints nullable="false"/></column>
    </createTable>
</changeSet>

<!-- child SECOND: its FK references the already-created parent -->
<changeSet id="create-child" author="app">
    <createTable tableName="CHILD">
        <column name="ID" type="${uuid.type}">
            <constraints nullable="false" primaryKey="true" primaryKeyName="PK_CHILD"/>
        </column>
        <column name="VERSION" type="int"><constraints nullable="false"/></column>
        <column name="NAME" type="varchar(100)"><constraints nullable="false"/></column>
        <column name="PARENT_ID" type="${uuid.type}"><constraints nullable="false"/></column>
    </createTable>
    <addForeignKeyConstraint baseTableName="CHILD" baseColumnNames="PARENT_ID"
                             referencedTableName="PARENT" referencedColumnNames="ID"
                             constraintName="FK_CHILD_ON_PARENT"/>
</changeSet>
```

For a **composition** child, the delete cascade is enforced by Jmix at the
application layer (`@Composition` + `@OnDelete(DeletePolicy.CASCADE)` on the
entity), NOT by the database — Jmix uses soft delete by default, so a DB-level
`onDelete="CASCADE"` would never fire. Leave the FK without `onDelete` unless
you specifically need hard-delete DB-level enforcement.

## Root Changelog Reachability

```xml
<includeAll path="/com/company/app/liquibase/changelog"/>
```

If the project uses explicit includes instead of `includeAll`, follow that existing style:

```xml
<include file="/com/company/app/liquibase/changelog/030-customer.xml"/>
```

## Forbidden

- New changelog file that is not reachable from the root changelog.
- Reusing a changeset id in the same changelog file.
- Modifying a changeset already applied to a DB: it changes the checksum and Liquibase hard-fails at startup. Add a NEW changeset instead.
- Raw `UUID` type instead of `${uuid.type}`.
- Invented type macros such as `${datetime.type}` when the project does not define them.
- Missing `VERSION`.
- Nullable database column for a required Java field.
- Java precision/length different from Liquibase precision/length.
- Missing FK for persistent references.
- A child table / FK changeSet ordered BEFORE the parent table it references.
