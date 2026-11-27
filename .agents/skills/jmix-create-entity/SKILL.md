---
name: jmix-create-entity
description: Create or change a persistent Jmix JPA entity and its required surrounding artifacts.
---

# Create Persistent Entity

Use this skill when adding or changing a database-backed Jmix entity.

## Steps

1. Create or update the Java entity in `src/main/java/<base-package>/entity`.
   If you change an existing entity, first read `references/widening.md` in this skill's folder.
2. Add Jmix and JPA metadata: `@JmixEntity`, `@Entity`, `@Table`.
   - If the entity is `@Embeddable` or extends or is extended by another entity, first read `references/embeddable-inheritance.md` in this skill's folder.
   - If the entity belongs to a non-default data store or to an add-on, read **When the entity is in a non-default data store or in an add-on**.
3. Choose the id strategy explicitly; see "Id strategy" below. If the entity extends a
   framework base class, first read `references/framework-superclass.md` in this skill's folder.
4. Add `@Version`.
5. Add `@InstanceName` on a stable human-readable field or method.
6. Define columns with exact `nullable`, `length`, `precision`, and `scale` constraints from
   requirements. Check every column name against the reserved words of every targeted
   dialect ("Column names" below). Omit `length = 255`: it is `@Column`'s own default, and
   the changelog still spells `varchar(255)` ("Constraint audit" below).
   If the entity stores an uploaded file, read **When the entity has a file attribute**.
7. Use `FetchType.LAZY` for relationships. A required reference needs three annotations
   ("Required references" below).
   - If the entity is a parent or child in an aggregate, first read `references/composition.md` in this skill's folder.
   - If a reference carries `@OnDeleteInverse` and its target has no soft delete, first read `references/on-delete-inverse.md` in this skill's folder.
8. Create the Liquibase changelog using `jmix-create-liquibase-changelog`.
9. Add entity and attribute message keys using `jmix-add-i18n-keys`.
10. Add or update views and security roles if the entity is user-facing.
11. Before finishing, compare every required constraint against the source requirements and
    the Liquibase changelog ("Constraint audit" below).

## Entity Template

```java
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@JmixEntity
@Table(name = "CUSTOMER")
@Entity
public class Customer {
    @JmixGeneratedValue
    @Column(name = "ID", nullable = false)
    @Id
    private UUID id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @InstanceName
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    // getters and setters
}
```

Imports: Spring Boot 3 and later use `jakarta.*`, never `javax.*`. `@PostConstruct` is from
`jakarta.annotation`; `@NotNull` / `@Email` are from `jakarta.validation.constraints`.

## Id strategy

Decide the strategy before writing the `@Id` field; it is expensive to change once rows and
changelogs exist. A new entity uses the same id strategy as the project's existing entities,
unless its id must come from outside the application (assigned natural key below). In a
project with no entities yet, use a generated UUID.

**Generated surrogate** — for entities this application owns. Jmix assigns the id on
`DataManager.create()`. The template uses a `UUID`:

```java
@JmixGeneratedValue
@Column(name = "ID", nullable = false)
@Id
private UUID id;
```

With a `Long` (or `Integer`) id field, the same `@JmixGeneratedValue` makes Jmix take the
id from a database sequence named `seq_id_<entity name>`. Jmix creates that sequence on
first use if it does not exist; `@JmixGeneratedValue(sequenceName = "...")` takes the id
from the named sequence instead (Jmix also creates it on first use if it is missing). The id column is `bigint` for a `Long` id and `int`/`integer` for an
`Integer` id, not `${uuid.type}`.

**Assigned natural key** — when the id comes from outside the application and must be
preserved: a numeric id from an external system being mirrored, an externally issued code.
No `@JmixGeneratedValue`: it makes Jmix assign the id at creation, which must not happen
here. The writer sets the id:

```java
@Column(name = "ID", nullable = false)
@Id
private Long id;              // assigned from the source system, never generated
```

```java
ExternalAccount account = dataManager.create(ExternalAccount.class);
account.setId(dto.id());       // caller MUST set it — nothing generates it
dataManager.save(account);
```

Pick the assigned key when the source system's id is the identity users and integrations
refer to, or a sync must upsert by that id (load by id, insert if absent). Otherwise pick the
generated surrogate, including when the external key only needs to be unique: that is a
unique index on an ordinary column, not an id. Match the Liquibase column type to the Java
type (`bigint` for `Long`, not `${uuid.type}`).

## Column names

A column name derived from the field name may be an SQL reserved word, and the targeted
dialects do not agree which words those are. Check per dialect, not per SQL standard: a
green suite against the test store proves nothing about the production store.

- **PostgreSQL:** `select catcode from pg_get_keywords() where word = '<lower-case name>'`
  — `reserved` cannot be used as an identifier at all. `END` is reserved;
  `START`, `LANGUAGE`, `TEXT`, `TYPE` and `NAME` are unreserved and fine.
- **HSQLDB** (the usual test store): default settings accept SQL-standard keywords as
  identifiers, so it will not warn you — `START`, `END` and `LANGUAGE` all pass there in
  DDL, INSERT, SELECT and UPDATE.

Rename the field or give it an explicit prefixed `@Column(name = "...")`. Common offenders
an entity field produces: `end`, `order`, `user`, `group`, `desc`, `references`.

## Required references

A required reference carries all three annotations (this is what Jmix Studio generates):

```java
@NotNull
@JoinColumn(name = "CUSTOMER_ID", nullable = false)
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Customer customer;
```

`@NotNull` and `optional = false` make the attribute mandatory in the Jmix metamodel.
`@JoinColumn(nullable = false)` declares the foreign key column NOT NULL; it must match the
changelog. Write all three. This holds for every required reference, not only composition
children.

## Required-field defaults

This rule applies to a required field whose default value the requirements define: a
status, a flag, a date ("defaults to X", "auto-set on create/update", "default now()",
"defaults to 0"). The default must apply at the entity layer, so that a bare
`DataManager.create()` + `DataManager.save()` succeeds without the caller touching the
field. Programmatic paths (services, REST, tests) bypass the UI, so a default that lives
only in a view is no default.

A required field the user enters (name, email) gets no default: saving is rejected until it
is filled, and tests must set it.

Three patterns, in order of preference:

**Constant default — field initializer**

```java
@Column(name = "QUANTITY", nullable = false)
@NotNull
private Integer quantity = 0;
```

**Initial default at creation — `@PostConstruct`.** It fires when Jmix instantiates the
entity (`DataManager.create()`, `Metadata.create()`, `DataContext.create()`), before the
caller sets any field. Works for both JPA entities and DTOs.

```java
@Column(name = "CREATED_AT", nullable = false)
@NotNull
private LocalDateTime createdAt;

@PostConstruct
public void postConstruct() {
    createdAt = LocalDateTime.now();
}
```

**Auto-set on every save — `EntitySavingEvent` listener**, for `lastUpdated`-style fields
that must change on UPDATE too, or for defaults that depend on other entities:

```java
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerSavingListener {

    @EventListener
    public void onSaving(EntitySavingEvent<Customer> event) {
        event.getEntity().setLastUpdated(LocalDateTime.now());
    }
}
```

`EntitySavingEvent` fires before every save (insert and update), so one listener covers the
initial and later values. `Customer` must declare the `lastUpdated` field; for the full
listener pattern see `jmix-add-entity-event-listener`.

Placements that look right and break tests:

| Wrong placement                                    | Why it fails                                   |
|----------------------------------------------------|------------------------------------------------|
| Default set only in the detail view's `InitEntityEvent` | Non-UI saves bypass the view; `DataManager.save()` hits the `@NotNull` violation. |
| Default set only in a service mutator that runs on UPDATE | The initial INSERT has no default; `@NotNull` fails. And a test calling `DataManager` directly never enters the service. |
| Default set in an `EntityChangedEvent` listener    | The listener fires AFTER persist — too late for `@NotNull`. It reacts to saves; it does not default them. |
| Default set only in the calling UI controller      | Programmatic paths (services, REST, tests) bypass it. |

## Index parity

When you add an index (typically on a reference column), declare it in the entity
annotation and in the changelog, with the same name on both sides:

```java
@Table(name = "CUSTOMER", indexes = {
        @Index(name = "IDX_CUSTOMER_MANAGER", columnList = "MANAGER_ID"),
        @Index(name = "IDX_CUSTOMER_REGION", columnList = "REGION_ID")
})
```

```xml
<createIndex indexName="IDX_CUSTOMER_MANAGER" tableName="CUSTOMER">
    <column name="MANAGER_ID"/>
</createIndex>
```

No check catches the difference: a missing `@Index` changes neither the metamodel nor the
schema, so `compileJava`, the Jmix inspection, and a green `test` all pass with the
index in the changelog only. It breaks Studio round-tripping and the rule that the code is
the source of truth. Do it as you write the column, not later.

If a column or a set of columns must be unique, first read `references/unique-columns.md` in this skill's folder.

## Auditing and Soft Delete

Add audit fields with the Spring Data annotations from `org.springframework.data.annotation`:
`@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`. For soft delete add
`@DeletedBy` and `@DeletedDate` from `io.jmix.core.annotation`; soft-deleted rows are then
filtered out of `DataManager`/JPQL queries automatically.

If a saved entity may reference a soft-deleted row, read **When a saved reference may point to a soft-deleted row**.

## Calculated and Transient Properties

Non-persistent derived attributes use `@JmixProperty` + `@Transient` +
`@DependsOnProperties({"a", "b"})` (`@JmixProperty` from `io.jmix.core.metamodel.annotation`).
An `@InstanceName` method also needs `@DependsOnProperties` listing every attribute it
reads, so they are fetched.

`@DependsOnProperties` lists only the entity's own attributes. A referenced entity is fetched
with its instance name only, so do not read its other attributes in a calculated property.

## Accessors

Match what the project's entities already do. If every entity carries `@Getter @Setter`,
that works (the enhancer runs over those classes and the suite passes), so write the new
entity the same way. In a project with hand-written accessors, write them by hand.

## Constraint audit

`compileJava` does not build the schema — Liquibase does — so a difference between Java and
DDL is silent. Check the Java annotations and the changelog side by side:

- `nullable` / `@NotNull`
- `length` — compare the effective length, not whether the attribute is written. `@Column`'s
  default is 255, so a 255-character column omits `length` in the annotation, while the
  changelog must still spell `varchar(255)` because SQL has no such default. An attribute
  present on one side and absent on the other is not a difference here.
- `precision` and `scale` — they default to 0, which is never the requirement, so always
  write them on both sides.
- enum id values and column type
- foreign key nullability
- indexes and unique constraints
- default values for required fields

Field semantics to apply when they are clear:

- A field named `email` usually gets `@Email`, unless the requirements say otherwise.
- Unlimited or large text uses the project's long-text pattern, usually `@Lob` plus a
  matching Liquibase type. Do not invent a length when the requirements say unlimited or the
  project uses long text for the same concept.
- `BigDecimal` columns use the exact required precision and scale in Java and Liquibase.

If a field uses `@PropertyDatatype`, read **When a field uses `@PropertyDatatype`**.

## Forbidden

- Missing `@JmixEntity`.
- Constructor-based entity creation.
- `@Data`, `@EqualsAndHashCode` or `@ToString` on a Jmix entity: they generate `equals`/`hashCode`/`toString` over mutable persistent fields, replacing the identity semantics the framework relies on and touching lazy references while doing it.
- `FetchType.EAGER`.
- `javax.*` imports instead of `jakarta.*`.
- Missing Liquibase changelog for persistent changes.
- A new entity with an id strategy different from the project's existing entities, unless its id is an assigned natural key or it extends a framework `@MappedSuperclass`.
- `@JmixGeneratedValue` on an assigned natural key — Jmix then assigns the id itself, and the source system's id is lost.
- `${uuid.type}` for the id column of a `Long` or `Integer` id.
- A column name that is a reserved word in any targeted dialect; HSQLDB will not warn.
- A required `@ManyToOne` without all three of `@NotNull`, `@ManyToOne(..., optional = false)` and `@JoinColumn(..., nullable = false)`.
- A required field with a default from the requirements that is set only in the UI (`InitEntityEvent`, a UI controller), only in a service mutator, or in an `EntityChangedEvent` listener.
- An index declared in the changelog but missing from `@Table(indexes = …)`, or the reverse.
- An `@InstanceName` method or calculated property without `@DependsOnProperties` for every attribute it reads, or one that reads non-instance-name attributes of a referenced entity.
- An invented `length` for unlimited text.
- `length = 255` written explicitly on `@Column`.
- `precision` or `scale` missing on either side (Java or changelog).
- An accessor style different from the project's other entities.
- Changing an existing entity's id strategy, `@Table` name, `@Version`, or existing columns' types and constraints while adding fields to it; a second `createTable` for an existing table.
- Nullable child back references in composition aggregates.
- Instantiating or replacing a collection field that Jmix populated — it may be a `NotInstantiatedList`/`NotInstantiatedSet`. Leave collection fields uninitialized; do not assign `new ArrayList`/`new HashSet`.

## Special cases

### When a saved reference may point to a soft-deleted row

The soft-delete filter applies to writes as well as to the application's queries. Inserting
or updating a row whose reference points at a soft-deleted entity fails, with a message that
names the wrong cause:

```
IllegalStateException: During synchronization a new object was found through
a relationship that was not marked cascade PERSIST
```

If such a reference is a legal state in the model (a child that may stay under a deleted
parent), the save must carry the hint:

```java
dataManager.save(new SaveContext()
        .setHint(PersistenceHints.SOFT_DELETION, false)
        .saving(entity));
```

### When the entity has a file attribute

Use `FileRef` for an uploaded file reference. The JPA converter is applied automatically:

```java
import io.jmix.core.FileRef;

@Column(name = "SCAN_FILE", length = 1024)
private FileRef scanFile;
```

Map the matching Liquibase column as `varchar(1024)`.

### When the entity is in a non-default data store or in an add-on

For a non-default data store, annotate the entity with `@Store(name = "...")` (the store is
defined in `application.properties`). Add-on entities use an entity-name prefix, e.g.
`@Entity(name = "app_Customer")`.

### When a field uses `@PropertyDatatype`

Inspect the datatype class's `@Ddl` too. Compare its SQL type with the intended column and
verify the effective generated DDL: matching `@Column` and Liquibase alone can hide a
conflicting datatype declaration. Do not assume the field annotation wins. Use a compatible
datatype or separate display formatting from schema typing, and review generated migrations
for unintended precision or scale changes before applying them.
