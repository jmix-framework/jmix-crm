---
name: jmix-configure-fetch-plan
description: Configure or audit Jmix fetch plans in XML views, fragments, DataManager loads, repositories, and entity events when loading references, avoiding N+1 queries, fixing unfetched attribute errors, or tuning data loading.
---

# Configure Fetch Plan

Use this skill when a task changes what entity attributes or references are loaded.

## Steps

1. Identify every property read by the view, service, listener, renderer, mapper, or assertion.
2. Start with `_base` unless there is a measured reason to load a partial entity.
3. Add reference properties explicitly when a loaded entity is detached or when a list/grid displays reference attributes.
4. For list views, include only references and scalar columns that are displayed or used by renderers/actions.
5. For detail views and compositions, include edited reference properties and child collections that the form or grid uses.
6. For service/listener code, add a fluent `DataManager.fetchPlan(...)` or named plan before reading references after load.
7. Avoid deep nested collections; prefer a second focused load when a graph becomes wide or multi-collection.
8. Check custom fetch plans against every `getX()` call after load.
9. Verify property names and run the load path before trusting the plan (see **Verify** below).

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

## Fetch Modes

Set per-property fetch mode to control how references are loaded:

- `AUTO` — framework picks the optimal mode (default).
- `JOIN` — loads the reference in the same SQL query; best for to-one references.
- `BATCH` — loads references in a separate `IN`-clause query; best for to-many collections (avoids N+1).
- `UNDEFINED` — separate SELECT per reference attribute.

Set it with the `fetch` attribute on a fetch-plan property (the XML attribute is `fetch`, NOT `fetchMode`):

```xml
<fetchPlan extends="_base">
    <property name="customer" fetch="JOIN"/>  <!-- to-one -->
    <property name="lines" fetch="BATCH"/>     <!-- to-many collection -->
</fetchPlan>
```

## JmixDataRepository

Select the plan in one of two ways: pass a `FetchPlan` as the **last** method argument, or annotate the method with `@FetchPlan("name")` (`io.jmix.core.repository.FetchPlan`). A plain `String` parameter is bound as an ordinary query parameter, NOT a plan selector. Build complex plans with the `FetchPlans` bean: `fetchPlans.builder(Order.class).addFetchPlan(FetchPlan.BASE).add("customer", FetchPlan.INSTANCE_NAME).build()`.

## Partial Entity Audit

Use a partial fetch plan only when it is intentionally narrower than `_base`:

- The loaded entity is wide or the result list is large.
- Every local property read later is listed in the plan.
- UI components, renderers, validators, and mappers do not access omitted attributes.
- Tests cover the path that previously caused the performance issue or unfetched attribute error.

## Shared Plans

Prefer inline XML or fluent `DataManager` fetch plans for feature-local needs.

Use `fetch-plans.xml` only when the same complex graph is reused in multiple places. If you add a shared plan, configure or verify the project's `jmix.core.fetch-plans-config` property and keep the name stable.

## Verify — compile is blind to fetch plans

`compileJava` never reads view XML or fetch-plan property names, so a missing
or misspelled reference is invisible until the load path runs: a detached
entity throws `IllegalStateException: Cannot get unfetched attribute [...]` at
RENDER time, or when a service/test reads `getX()` after load. The gate is
never the compiler.

1. **Property names — verify before you type them.** Every `<property
   name="...">` in a `<fetchPlan>` and every `.add("...")` in a fluent plan
   must be a real attribute of the entity, and the `FetchPlan` constants
   (`FetchPlan.BASE`, `FetchPlan.INSTANCE_NAME`) and built-in plan names
   (`_base`, `_instance_name`) must be spelled exactly. Confirm against the
   entity source, Context7 (`/jmix-framework/jmix-context7`), or IDE symbol
   search — see `jmix-verify-api-symbol`.
2. **Static inspection (Gate 1).** Run `jmix-ide-static-analysis`
   (get_file_problems) on the view/fragment XML — the Jmix-XSD-aware
   inspection flags an invalid property path inside a `<fetchPlan>` that the
   compiler ignores.
3. **Run the load path (Gate 2).** Exercise the smallest test or view that
   reads the references; only this catches an attribute that is mapped but
   never fetched.

## Forbidden

- `FetchType.EAGER` to solve loading problems.
- Reading local attributes omitted from a partial fetch plan.
- Loading references inside loops when a fetch plan can load them with the root query.
- Deep multi-collection graphs in a single list load.
- Using fetch plans as a security boundary.
- `@Table` names in JPQL queries.
- Shared named fetch plans for one-off local view needs.
