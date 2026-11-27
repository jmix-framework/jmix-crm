---
name: jmix-configure-fetch-plan
description: Configure or audit Jmix fetch plans in XML views, fragments, DataManager loads, repositories, and entity events when loading references, avoiding N+1 queries, fixing unfetched attribute errors, or tuning data loading.
---

# Configure Fetch Plan

Use this skill when a task changes what entity attributes or references are loaded.

## Steps

1. Identify every property read by the view, service, listener, renderer, mapper, or assertion. If code reads a subclass-only attribute of an entity loaded or referenced through its base class, first read `references/single-table-inheritance.md` in this skill's folder.
2. Start with `_base` unless there is a measured reason to load a partial entity (see **Partial Entity Audit**).
3. Check the effective inherited plan before adding references for detached access or list/grid display. Add only missing properties or nested attributes.
4. For list views, include only references and scalar columns that are displayed or used by renderers/actions.
5. For detail views and compositions, include edited reference properties and child collections that the form or grid uses. If the loader of a list or detail view calls a `JmixDataRepository` method, read **When the load goes through a `JmixDataRepository`** below.
6. For service/listener code, add a fluent `DataManager.fetchPlan(...)` or named plan before reading references after load. If the load goes through a `JmixDataRepository` method, read **When the load goes through a `JmixDataRepository`** below.
7. Avoid deep nested collections; prefer a second focused load when a graph becomes wide or multi-collection. If you set a fetch mode (`fetch="JOIN"`, `fetch="BATCH"`) or the task is about N+1 queries, first read `references/fetch-modes.md` in this skill's folder.
8. Check custom fetch plans against every `getX()` call after load.
9. Verify property names and run the load path before trusting the plan (see **Verify** below).

`_base` can already include references needed by the `@DependsOnProperties` of an `@InstanceName` method. Inspect them and the effective nested plan before repeating the reference or adding a shared plan. Inclusion alone does not prove that every nested attribute a consumer reads is fetched: compare with step 1 and run the detached access path.

## XML Pattern

```xml
<collection id="ordersDc" class="com.company.app.entity.Order">
    <fetchPlan extends="_base">
        <property name="customer" fetchPlan="_instance_name"/>
        <property name="lines" fetchPlan="_base"/>
    </fetchPlan>
    <loader id="ordersDl" readOnly="true">
        <query><![CDATA[select e from Order e]]></query>
    </loader>
</collection>
```

Use the JPA/Jmix entity name in JPQL, not the database table name.

## DataManager Pattern

```java
List<Order> orders = dataManager.load(Order.class)
        .query("select e from Order e")
        .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                .add("customer", FetchPlan.INSTANCE_NAME))
        .list();
```

For an event listener that reads a reference:

```java
Order order = dataManager.load(event.getEntityId())
        .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                .add("customer", FetchPlan.BASE))
        .one();
```

### A reference property needs the two-argument `.add(...)`

```java
.add("customer", FetchPlan.BASE)   // RIGHT: nested plan for the reference
.add("customer")                   // WRONG for a reference: EMPTY nested plan
```

The one-argument `.add("customer")` compiles, runs, and loads the reference with an empty nested fetch plan. Nothing fails at load time; it fails the first time anything reads an attribute of `customer`:

```
IllegalStateException: Cannot get unfetched attribute [name] from detached object
```

The one-argument form is correct for a scalar property (`.add("orderDate")`); only references need the second argument: `FetchPlan.INSTANCE_NAME` when a grid or caption shows the reference, `FetchPlan.BASE` when code reads its other attributes.

### A dotted path `add("a.b", PLAN)` leaves `a` without its own attributes

```java
.add("product.supplier", FetchPlan.BASE)          // WRONG if you also read product's own fields
.add("product", p -> p.addFetchPlan(FetchPlan.BASE)
        .add("supplier", FetchPlan.BASE))         // RIGHT
```

A dotted path adds the intermediate reference only to hold the nested attribute: `product` is in the plan for the sake of `supplier`, and its own attributes are not loaded. The first `product.getName()` throws the same `Cannot get unfetched attribute`. Read it as "product at a minimum, plus its supplier", not "product in full, plus its supplier".

## What a missing attribute does depends on its kind

| missing from the plan | behaviour |
| --- | --- |
| a local (scalar) attribute | `IllegalStateException: Cannot get unfetched attribute` — no rescue |
| a reference, ordinary code | lazy-loaded: a silent extra SELECT per row, and the reference arrives with all its local attributes |
| a reference, inside `EntitySavingEvent` | `IllegalStateException` — lazy loading is not available there |

## Partial Entity Audit

Use a partial fetch plan only when it is intentionally narrower than `_base`:

- The loaded entity is wide or the result list is large.
- Every local property read later is listed in the plan.
- UI components, renderers, validators, and mappers do not access omitted attributes.
- Tests cover the path that previously caused the performance issue or unfetched attribute error.

## Shared Plans

Prefer inline XML or fluent `DataManager` fetch plans for feature-local needs. Use `fetch-plans.xml` only when the same complex graph is reused in multiple places. If you add a shared plan, configure or verify the project's `jmix.core.fetch-plans-config` property and keep the name stable.

## Verify — compile is blind to fetch plans

`compileJava` never reads view XML or fetch-plan property names. A missing or misspelled reference shows up only when the load path runs: the detached entity throws `IllegalStateException: Cannot get unfetched attribute [...]` at render time, or when a service or test reads `getX()` after load.

1. **Property names — verify before you type them.** Every `<property name="...">` in a `<fetchPlan>` and every `.add("...")` in a fluent plan must be a real attribute of the entity. The `FetchPlan` constants (`FetchPlan.BASE`, `FetchPlan.INSTANCE_NAME`) and built-in plan names (`_base`, `_instance_name`) must be spelled exactly. Confirm attributes against the entity source, and anything else with `jmix-verify-api-symbol`.
2. **Static inspection (Gate 1).** Run `jmix-ide-static-analysis` (get_file_problems) on the view/fragment XML. The Jmix-XSD-aware inspection flags an invalid property path inside a `<fetchPlan>` that the compiler ignores.
3. **Run the load path (Gate 2).** Run the smallest test or view that reads the references; only this catches an attribute that is mapped but never fetched.

## Forbidden

- `FetchType.EAGER` to solve loading problems.
- The one-argument `.add("reference")` for a reference property — it builds an empty nested plan.
- A dotted path `.add("a.b", ...)` when code also reads the own attributes of `a`.
- Casting a base-class-typed load to a subclass, or using a reference typed as the base class, to read a subclass-only attribute.
- Reading local attributes omitted from a partial fetch plan.
- Reading a reference inside `EntitySavingEvent` that the plan does not load.
- Loading references inside loops, or leaving a reference read on every row to lazy loading, when a fetch plan can load them with the root query.
- `FetchMode.JOIN` on a to-one nested inside a `FetchMode.BATCH` collection — EclipseLink throws at query time.
- Trusting `fetch="BATCH"` to remove N+1 on a nested collection without counting the executed SQL.
- Deep multi-collection graphs in a single list load.
- Using fetch plans as a security boundary.
- `@Table` names in JPQL queries.
- Shared named fetch plans for one-off local view needs.
- Unchecked property or plan names, or `compileJava` as proof that a fetch plan is complete.

## Special cases

### When the load goes through a `JmixDataRepository`

Select the plan in one of two ways: pass a `FetchPlan` as the last method argument, or annotate the method with `@FetchPlan("name")` (`io.jmix.core.repository.FetchPlan`). A plain `String` parameter is bound as an ordinary query parameter, not a plan selector. Build complex plans with the `FetchPlans` bean: `fetchPlans.builder(Order.class).addFetchPlan(FetchPlan.BASE).add("customer", FetchPlan.INSTANCE_NAME).build()`.
