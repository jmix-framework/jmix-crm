# To-many attribute in a detail view

Part of `jmix-create-detail-view`. Read this when the view shows a to-many attribute.

To show a to-many composition or association (`Order.products`, `Customer.tags`), nest a property-bound `<collection>` inside the `<instance>` container and bind a `dataGrid` to it:

```xml
<instance id="orderDc" class="com.company.app.entity.Order">
    <fetchPlan extends="_base">
        <property name="products" fetchPlan="_instance_name"/>
    </fetchPlan>
    <loader id="orderDl"/>
    <!-- property-bound: NO class, NO loader, NO query -->
    <collection id="productsDc" property="products"/>
</instance>
```

```xml
<dataGrid id="productsDataGrid" dataContainer="productsDc" width="100%">
    <columns>
        <column property="name"/>
    </columns>
</dataGrid>
```

A top-level `<collection>` (a list view's own loader) requires `class`. A nested property-bound collection takes `property` and nothing else — no `class`, no loader, no query; the container gets its metaclass from the property, so a `class` attribute is a descriptor defect. The parent's fetch plan must include the property, or the grid is empty (see `jmix-configure-fetch-plan`).

For a read-only grid, declare no `list_create` / `list_edit` / `list_remove` actions on it.
