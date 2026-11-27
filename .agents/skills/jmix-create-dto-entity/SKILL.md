---
name: jmix-create-dto-entity
description: Create or change Jmix DTO entities and other non-persistent transfer models for UI data containers, REST/API payloads, custom data stores, external data, or calculated read models.
---

# Create DTO Entity

Use this skill when a non-JPA model must take part in Jmix metadata, UI data containers, or DataManager-backed custom data stores.

## Steps

1. Decide whether the model needs Jmix metadata. It needs it when the model is bound to Flow UI components, localized as an entity, or handled by a custom data store. If the model is an internal service or API payload that is not shown in Jmix UI, read **When the model is not shown in Jmix UI** below.
2. Create the DTO class in a package consistent with the project: usually `entity`, or a dedicated DTO package if the project already has one.
3. Add `@JmixEntity`; use `annotatedPropertiesOnly = true` when only selected fields should become entity attributes.
4. Add `@JmixId` on a stable unique attribute. Add `@JmixGeneratedValue` for generated UUID ids.
5. Add `@InstanceName` when the DTO appears in pickers, grids, or captions.
6. Add `@JmixProperty(mandatory = true)` for required DTO attributes (they have no JPA `@Column`).
7. Instantiate UI-bound DTO entities with `Metadata.create()` or `DataManager.create()`. A constructor leaves a generated id null and the instance outside Jmix metadata.
8. Add message keys for the DTO entity and its attributes when they are user-facing.
9. Do not create Liquibase changes for pure DTO entities.
10. If the DTO must support generic CRUD through `DataManager`, read **When the DTO belongs to a custom data store** below. For a calculated grid or API response, return a DTO list from a service instead (see UI Usage).
11. Verify (below).

## DTO Entity Template

```java
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;

import java.math.BigDecimal;
import java.util.UUID;

@JmixEntity(annotatedPropertiesOnly = true)
public class CustomerSummary {
    @JmixId
    @JmixGeneratedValue
    @JmixProperty(mandatory = true)
    private UUID id;

    @InstanceName
    @JmixProperty(mandatory = true)
    private String name;

    @JmixProperty
    private BigDecimal totalAmount;

    // getters and setters
}
```

## UI Usage

For a DTO collection shown in a view, use a collection container and load it explicitly from a service or load delegate:

```xml
<collection id="customerSummariesDc" class="com.company.app.entity.CustomerSummary">
    <loader id="customerSummariesDl" readOnly="true"/>
</collection>
```

```java
@Install(to = "customerSummariesDl", target = Target.DATA_LOADER)
private List<CustomerSummary> customerSummariesDlLoadDelegate(LoadContext<CustomerSummary> loadContext) {
    return summaryService.loadSummaries();
}
```

## Verify

Binding errors show at view init, not at compile time: a DTO whose UI-bound attribute is not a real `@JmixProperty`, or a data container whose `class` points at the wrong type, compiles and then fails when the view initializes or renders.

1. Annotations and packages: `@JmixId` and `@JmixGeneratedValue` come from `io.jmix.core.entity.annotation`; `@JmixEntity`, `@InstanceName` and `@JmixProperty` from `io.jmix.core.metamodel.annotation`. Confirm the package and members of any annotation new to the project with `jmix-verify-api-symbol` before you type them.
2. Static inspection (Gate 1): run `jmix-ide-static-analysis` (get_file_problems) on the view XML that binds the DTO container. It flags a container `class` or property path that does not resolve, which the compiler never checks.

## Forbidden

- `@Entity`, `@MappedSuperclass`, `@Table`, `@Column`, or Liquibase for pure DTO entities; an abstract DTO ancestor needs only `@JmixEntity`.
- Constructor calls for UI-bound DTO entities with generated ids.
- Missing `@JmixId` when identity matters in UI, data containers, or stores.
- A Jmix annotation imported from the wrong package.
- A UI-bound attribute that is not a `@JmixProperty`, or a container `class` that points at the wrong type (compiles, fails at view init).
- Lombok `@Data` or generated `equals()` / `hashCode()` on Jmix entities.
- Making every API record a Jmix DTO entity by default.
- Inventing a custom data store for a calculated grid or API response.
- Using a DTO entity to bypass proper fetch plans or security policies.

## Special cases

### When the model is not shown in Jmix UI

Use a plain Java class or record for internal service or API payloads that need no Jmix metadata:

```java
public record CustomerSummaryResponse(UUID customerId, String name, BigDecimal totalAmount) {
}
```

Do not add `@JmixEntity` just because a class is called DTO.

### When the DTO belongs to a custom data store

Use `@Store(name = "...")` only when a real custom data store exists and the DTO should support generic CRUD through `DataManager`.
