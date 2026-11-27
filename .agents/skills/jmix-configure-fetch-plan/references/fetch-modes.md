# Fetch modes and N+1 queries

Part of `jmix-configure-fetch-plan`. Read this when you set a fetch mode or fix N+1 queries.

Set the fetch mode per property to control how references are loaded:

- `AUTO` — framework picks the optimal mode (default).
- `JOIN` — loads the reference in the same SQL query; best for to-one references.
- `BATCH` — loads references in a separate `IN`-clause query; meant for to-many collections (avoids N+1), but see below.
- `UNDEFINED` — separate SELECT per reference attribute.

The XML attribute on a fetch-plan property is `fetch`, not `fetchMode`:

```xml
<fetchPlan extends="_base">
    <property name="customer" fetch="JOIN"/>  <!-- to-one -->
    <property name="lines" fetch="BATCH"/>     <!-- to-many collection -->
</fetchPlan>
```

**`BATCH` may do nothing for a nested collection — measure, do not assume.** A plan built with `FetchPlanBuilder` is installed as a `LOAD_GROUP`, not a `FETCH_GROUP`, because its `loadPartialEntities()` is false. The mode is still recorded (the DEBUG log of `io.jmix.eclipselink.impl.FetchGroupManager` prints `Fetch modes for ...: e.orderLines=BATCH`), but EclipseLink loads such a collection with one query per parent anyway:

```
... WHERE ORDER_ID = ?        -- repeated once for every row of the page
```

Calling `partial()` does not change it. When a measurement shows the per-parent queries, take the collection out of the fetch plan and load it for the whole page with one repository query:

```java
// one query for the page, not one per parent
List<OrderLine> lines = orderLineRepository.findByOrderIdIn(orderIds);
```

Only a count of executed SQL shows which shape you got: `compileJava`, the IDE inspection, and a green `test` all pass either way. Guard it with a test that asserts the query count does not grow with page size.

Never set `FetchMode.JOIN` on a to-one nested inside a `BATCH` collection. It looks like a way to add that reference to the collection query, but EclipseLink throws at query time:

```
NullPointerException: Cannot invoke "java.util.Collection.toArray()" because "c" is null
```
