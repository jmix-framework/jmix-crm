---
name: jmix-create-enum
description: Create a Jmix enum for entity attributes with stable database ids and localization.
---

# Create Jmix Enum

Use this skill when an entity attribute has a fixed set of values.

## Steps

1. Look for an existing enum convention in the project first, and match it instead of introducing a second scheme:

   ```bash
   grep -rn "implements .*EnumClass" src/main/java --include='*.java' | head
   ```

   If the project's enums implement a shared base interface, read **When the project's enums implement a shared base interface** below.
2. Create the enum in the `entity` package.
3. Implement `io.jmix.core.metamodel.datatype.EnumClass<T>`.
4. Use stable database ids, not display labels.
5. Add a typed `fromId()` method annotated with `@Nullable` from `org.jspecify.annotations`.
6. Store the enum id type in the entity field.
7. Add getter/setter conversion in the entity.
8. Add a Liquibase column that matches the id type.
9. Add enum message keys in all locale files. See `jmix-add-i18n-keys` for the `<package>/<EnumClass>.<CONSTANT>` key shape (e.g. `com.company.app.entity/TransactionType.INCOME`), not an all-dots `FQCN.CONSTANT` form.

## Enum Template

```java
import io.jmix.core.metamodel.datatype.EnumClass;
import org.jspecify.annotations.Nullable;

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

A constant may carry more constructor arguments and accessors beside the stable id. The metamodel does not see them, so they need no message key and appear in no fetch plan. Only the stable id is persisted, never a second field.

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

- Keep one id type across `EnumClass<T>`, `getId()`/`fromId()`, the entity field, and the Liquibase column (e.g. all `String`/`VARCHAR`). A mismatch compiles but silently corrupts load and save.
- Each literal id is a hardcoded stable value, never a display label or `ordinal()`/`name()`. Exception: the project's base interface already returns `name()`; read **When the project's enums implement a shared base interface** below.
- In a view, bind an enum attribute with `<comboBox>` (its `Range` is an enumeration), not `entityComboBox`, which is for entity associations.

## Forbidden

- `io.jmix.core.EnumClass`.
- Raw `EnumClass` without a type parameter.
- Storing display labels as ids.
- `ordinal()` persistence, or introducing `.name()` ids into a project that has no enum id convention yet (an established project-wide convention is matched; see **When the project's enums implement a shared base interface**).
- Different id types across `EnumClass<T>`, `getId()`/`fromId()`, the entity field and the Liquibase column.
- Persisting a second field of the enum instead of the stable id.
- Declaring the entity field with the enum type instead of the id type.
- `@Nullable` on `fromId()` from a package other than `org.jspecify.annotations`.
- Missing enum message keys, or keys in the all-dots `FQCN.CONSTANT` form.
- `entityComboBox` for an enum attribute.

## Special cases

### When the project's enums implement a shared base interface

Many codebases have a shared base interface that every enum implements, often with a default `getId()`. Match it instead of introducing a second id scheme.

If its `getId()` returns `name()`, match that too. The `.name()` prohibition (in **Stable-id type-chain** and **Forbidden** above) is about not introducing that scheme into a project that has no id convention yet. It is not a reason to make one new enum the only member of the family with a different id scheme.
