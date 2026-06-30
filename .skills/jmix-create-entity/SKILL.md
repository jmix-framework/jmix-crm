---
name: jmix-create-entity
description: Create or change a persistent Jmix JPA entity and its required surrounding artifacts.
---

# Create Persistent Entity

Use this skill when adding or changing a database-backed Jmix entity.

## Steps

1. Create or update the Java entity in `src/main/java/<base-package>/entity`.
2. Add Jmix and JPA metadata: `@JmixEntity`, `@Entity`, `@Table`.
3. Add UUID identity with `@Id` and `@JmixGeneratedValue`.
4. Add `@Version`.
5. Add `@InstanceName` on a stable human-readable field or method.
6. Define columns with exact `nullable`, `length`, `precision`, and `scale` constraints from requirements.
7. Use `FetchType.LAZY` for relationships.
8. Create the Liquibase changelog using `jmix-create-liquibase-changelog`.
9. Add entity and attribute message keys using `jmix-add-i18n-keys`.
10. Add or update views and security roles if the entity is user-facing.
11. Before finishing, compare every required constraint against the source requirements and the Liquibase changelog.

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

## Required-field defaults — at the ENTITY layer, not elsewhere

When a required field "defaults to X" or is "auto-set on create/update"
(e.g. "default now()", "auto-set", "defaults to 0"), the default MUST
apply at the entity layer so a bare `DataManager.create()` +
`DataManager.save()` succeeds with the caller never touching the field.
This is the contract: programmatic paths (services, REST, tests) bypass
the UI, so a default that lives only in the view is no default at all.

Three patterns, in order of preference:

**Constant default — field initializer**

```java
@Column(name = "QUANTITY", nullable = false)
@NotNull
private Integer quantity = 0;
```

**Initial default at creation — `@PostConstruct`**

`@PostConstruct` fires when Jmix instantiates the entity
(`DataManager.create()`, `Metadata.create()`, `DataContext.create()`),
before the caller sets any field. Works for both JPA entities and DTOs.

```java
@Column(name = "CREATED_AT", nullable = false)
@NotNull
private LocalDateTime createdAt;

@PostConstruct
public void postConstruct() {
    createdAt = LocalDateTime.now();
}
```

Imports (Spring Boot 3 uses `jakarta.*`, never `javax.*`): `@PostConstruct` from `jakarta.annotation`; `@NotNull` / `@Email` from `jakarta.validation.constraints`.

**Auto-set on every save — `EntitySavingEvent` listener** (for
`lastUpdated`-style fields that must touch on UPDATE too, or for any
cross-entity defaulting):

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

`EntitySavingEvent` fires before EVERY save (both insert and update),
so the listener covers initial and subsequent timestamps in one place.
`Customer` must declare the `lastUpdated` field; for the full event-listener
pattern see `jmix-add-entity-event-listener`.

### Anti-patterns that look right and break tests

| Wrong placement                                    | Why it fails                                   |
|----------------------------------------------------|------------------------------------------------|
| Default set only in the detail view's `InitEntityEvent` | Non-UI saves bypass the view; `DataManager.save()` hits the `@NotNull` violation. |
| Default set only in a service mutator that runs on UPDATE | The initial INSERT has no default; `@NotNull` fails. And a test calling `DataManager` directly never enters the service. |
| Default set in an `EntityChangedEvent` listener    | The listener fires AFTER persist — too late for `@NotNull`. It reacts to saves; it does not default them. |
| Default set only in the calling UI controller      | Programmatic paths (services, REST, tests) bypass it. |

## Composition Checklist

For parent-child aggregates:

- Parent collection has `@Composition`.
- Parent collection has `@OnDelete(DeletePolicy.CASCADE)` when child lifecycle belongs to parent.
- Child has a non-null back reference to parent.
- Child `@ManyToOne` uses `fetch = FetchType.LAZY` and `optional = false`.
- Child join column is `nullable = false`.
- Parent detail view edits the child collection via a property-bound `<collection property=...>` (no loader/query), and the parent fetchPlan includes the child property.
- The child's own detail view exists, plus its role policy, when the UI opens the child in a dialog.

```java
import io.jmix.core.DeletePolicy;
import io.jmix.core.entity.annotation.OnDelete;
import io.jmix.core.metamodel.annotation.Composition;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Composition
@OnDelete(DeletePolicy.CASCADE)
@OneToMany(mappedBy = "parent")
private List<ChildLine> lines;  // leave uninitialized — Jmix returns a NotInstantiatedList

@JoinColumn(name = "PARENT_ID", nullable = false)
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Parent parent;
```

## Auditing and Soft Delete

Add audit fields with the Spring Data annotations from `org.springframework.data.annotation`: `@CreatedBy`, `@CreatedDate`, `@LastModifiedBy`, `@LastModifiedDate`. For soft delete add `@DeletedBy` and `@DeletedDate` from `io.jmix.core.annotation` — soft-deleted rows are then auto-filtered out of `DataManager`/JPQL queries.

## Calculated and Transient Properties

Non-persistent derived attributes use `@JmixProperty` + `@Transient` + `@DependsOnProperties({"a", "b"})` (`@JmixProperty` from `io.jmix.core.metamodel.annotation`). The same applies to an `@InstanceName` method: it must carry `@DependsOnProperties` listing every attribute it reads so they are fetched.

## Embeddable, Inheritance, and Data Stores

- `@Embeddable` value objects (still annotated `@JmixEntity`) are supported, as are JPA inheritance strategies (`@Inheritance` with `JOINED`, `SINGLE_TABLE`, or `TABLE_PER_CLASS`).
- For a non-default data store, annotate the entity with `@Store(name = "...")` (defined in `application.properties`); add-on entities use an entity-name prefix, e.g. `@Entity(name = "app_Customer")`.

## Constraint Audit

`compileJava` does not build the schema — Liquibase does — so Java/DDL
drift is silent. Check Java annotations and the Liquibase changelog side
by side:

- `nullable` / `@NotNull`
- `length`
- `precision` and `scale`
- enum id values and column type
- foreign key nullability
- indexes and unique constraints
- default values for required fields

## Semantic Constraint Checks

Apply common Java validation and persistence mappings when the field semantics are clear:

- Fields named `email` should usually have `@Email` unless the requirements explicitly say otherwise.
- Unlimited or large text should use the project pattern for long text, usually `@Lob` plus a matching Liquibase type, not an invented arbitrary length.
- `BigDecimal` columns must use the exact required precision and scale in both Java and Liquibase.
- Do not invent a length for a field when the requirements say it is unlimited or when the existing project uses long text for the same concept.

## Forbidden

- Missing `@JmixEntity`.
- Constructor-based entity creation.
- Lombok annotations (`@Data`, `@Getter`, `@Setter`, etc.) on Jmix entities — they interfere with the entity enhancer and break JPA/Jmix metadata.
- `FetchType.EAGER`.
- Missing Liquibase changelog for persistent changes.
- Nullable child back references in composition aggregates.
- Relying only on UI initialization for required persistence fields.
- Instantiating or replacing a collection field that Jmix populated — it may be a `NotInstantiatedList`/`NotInstantiatedSet`. Leave collection fields uninitialized; do not assign `new ArrayList`/`new HashSet`.
- A 0-byte `.java` — it passes compile and the clean test boot, but breaks the registry. Confirm every file you wrote is non-empty.
