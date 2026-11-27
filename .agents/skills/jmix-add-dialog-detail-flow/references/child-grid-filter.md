# Child grid filtered by the master grid selection

Part of `jmix-add-dialog-detail-flow`. Read this when a child grid is filtered by the master grid selection.

Prefer binding to the selected master container:

```xml
<loader id="orderLinesDl" readOnly="true">
    <query><![CDATA[
        select e from OrderLine e
        where e.order = :container_ordersDc
        order by e.createdDate desc
    ]]></query>
</loader>
```

The container parameter itself is the selected item. Do not append `.selected`; never write:

```xml
where e.order = :container_ordersDc.selected
```

A `:container_<DcId>` parameter is bound automatically and may stay under `<dataLoadCoordinator auto="true"/>`. A manual parameter (not `:container_*` / `:component_*`, for example `:param`) is not: a loader that runs with it unset (under `auto="true"`, or `load()` before `setParameter`) throws an `IllegalStateException` at view open about the missing query parameter. Keep such a loader out of the coordinator and load it from the selection handler:

```xml
<facets>
    <!-- coordinator lists only auto-safe loaders; the manual-param loader is NOT here -->
    <dataLoadCoordinator auto="true"/>
</facets>
```

When passing an entity manually, compare the entity-valued property to the entity parameter:

```xml
where e.order = :param
```

When passing only an id manually, compare to the id field:

```xml
where e.order.id = :paramId
```

Never call `load()` after removing a required manual parameter. Bind to the selected master container, skip loading until a master is selected, or clear the child container when no master is selected:

```java
Order order = ordersDataGrid.getSingleSelectedItem();
if (order == null) {
    orderLinesDc.getMutableItems().clear();
    return;
}

orderLinesDl.setParameter("order", order);
orderLinesDl.load();
```
