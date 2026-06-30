---
name: jmix-create-enum
description: Create a Jmix enum for entity attributes with stable database ids and localization.
---

# Create Jmix Enum

Use this skill when an entity attribute has a fixed set of values.

## Steps

1. Create the enum in the `entity` package.
2. Implement `io.jmix.core.metamodel.datatype.EnumClass<T>`.
3. Use stable database ids, not display labels.
4. Add a typed `fromId()` method annotated with `@Nullable`.
5. Store the enum id type in the entity field.
6. Add getter/setter conversion in the entity.
7. Add Liquibase column matching the id type.
8. Add enum message keys in all locale files — see `jmix-add-i18n-keys` for the `<package>/<EnumClass>.<CONSTANT>` key shape (e.g. `com.company.app.entity/TransactionType.INCOME`), NOT an all-dots `FQCN.CONSTANT` form.

## Enum Template

```java
import io.jmix.core.metamodel.datatype.EnumClass;
import org.springframework.lang.Nullable;

public enum TransactionType implements EnumClass<String> {
    INCOME("INCOME"),
    OUTCOME("OUTCOME");

    private final String id;

    TransactionType(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Nullable
    public static TransactionType fromId(String id) {
        for (TransactionType value : TransactionType.values()) {
            if (value.getId().equals(id)) {
                return value;
            }
        }
        return null;
    }
}
```

## Entity Mapping

```java
@Column(name = "TYPE", nullable = false, length = 50)
private String type;

public TransactionType getType() {
    return type == null ? null : TransactionType.fromId(type);
}

public void setType(TransactionType type) {
    this.type = type == null ? null : type.getId();
}
```

## Stable-id type-chain

Keep one consistent id type across `EnumClass<T>`, `getId()`/`fromId()`, the entity field, and the Liquibase column (e.g. all `String`/`VARCHAR`). A mismatch compiles cleanly but silently corrupts load/save. Each literal id must be a hardcoded stable value, never a display label or `ordinal()`/`name()`.

When binding an enum attribute in a view, use `<comboBox>` (its `Range` is an enumeration) — not `entityComboBox`, which is for entity associations.

## Forbidden

- `io.jmix.core.EnumClass`.
- Raw `EnumClass` without a type parameter.
- Storing display labels as ids.
- `ordinal()` or enum `.name()` persistence.
- Missing enum message keys.
