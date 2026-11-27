---
name: jmix-create-composition-detail-view
description: Add inline editable parent-child composition UI to a Jmix detail view, when a parent entity owns child records edited via a property-bound collection container (no query loader).
---

# Create Composition Detail Editing

Use this skill when a parent entity owns child entities edited inside the parent detail view through a property-bound `<collection property=...>` container (no query loader). For a button/action that opens a standalone detail dialog, or a master-row selection that filters an independently query-loaded child grid, use `jmix-add-dialog-detail-flow` instead.

## The child's own detail view must exist

The child grid's `list_create`/`list_edit` actions open the child's detail view as a dialog. That view must be a real, separately created view: controller and XML with the id `<Child>.detail` (step 2).

- Without it, clicking "+" throws `NoSuchViewException: View '<Child>.detail' is not defined`, and a `@ViewPolicy(viewIds = "<Child>.detail")` points at a view id that does not resolve.
- Each button's `action="<grid-id>.<actionId>"` must name the dataGrid's own `id` and an `<action>` declared inside that grid. Otherwise the button does nothing or the view fails to open.
- A clean compile shows none of these errors.

## Steps

1. Ensure the entity model is a real composition (model it with `jmix-create-entity`):
   - parent collection has `@Composition`;
   - child has non-null parent back reference;
   - parent has cascade delete if child lifecycle belongs to parent.
2. Create a child detail view with `jmix-create-detail-view`.
3. In the parent detail XML, add a collection container for the child collection. It is property-bound (`<collection property=...>`): no loader or query. The parent fetchPlan must include the child property so the children load with the parent.
4. Add a child `dataGrid` bound to that collection container.
5. Add `list_create`, `list_edit`, and `list_remove` actions to the child grid.
6. Render visible child-grid buttons for create, edit, and remove.
7. Add `openMode=DIALOG` to create/edit actions.
8. Add view policy for the child detail view (see `jmix-create-resource-role`).
9. Add messages for child entity and view title (see `jmix-add-i18n-keys`).
10. If the parent has a value computed from the children (an order total) that the view must update while children are edited, read **When the parent shows a value computed from the children** below.

## XML Pattern

```xml
<instance id="orderDc" class="com.company.app.entity.Order">
    <fetchPlan extends="_base">
        <property name="lines" fetchPlan="_base"/>
    </fetchPlan>
    <loader id="orderDl"/>
    <collection id="linesDc" property="lines"/>
</instance>

<hbox id="lineButtonsPanel" classNames="buttons-panel">
    <button id="createLineButton" action="linesDataGrid.createAction"/>
    <button id="editLineButton" action="linesDataGrid.editAction"/>
    <button id="removeLineButton" action="linesDataGrid.removeAction"/>
</hbox>

<dataGrid id="linesDataGrid" dataContainer="linesDc">
    <actions>
        <action id="createAction" type="list_create">
            <properties>
                <property name="openMode" value="DIALOG"/>
            </properties>
        </action>
        <action id="editAction" type="list_edit">
            <properties>
                <property name="openMode" value="DIALOG"/>
            </properties>
        </action>
        <action id="removeAction" type="list_remove"/>
    </actions>
</dataGrid>
```

The `<instance>`/`<collection>` containers go inside `<data>`; the `<hbox>` and `<dataGrid>` inside `<layout>` of the detail descriptor.

## Forbidden

- Child collection actions without a child detail view.
- A button `action` that does not name the grid's own `id` and an `<action>` declared inside that grid.
- Child collection actions without visible buttons or another reachable UI trigger.
- A loader or query on the child collection container, or a parent fetchPlan without the child property.
- Nullable child parent reference.
- A parent collection without `@Composition`.
- Child create or edit actions without `openMode=DIALOG`.
- Missing child detail view policy.
- Editing composition children outside the parent aggregate when the domain says they are owned.

## Special cases

### When the parent shows a value computed from the children

To update a computed parent value (an order total) while lines are edited, recalculate it from the child container's events, using the same bean as the save listener (`jmix-add-entity-event-listener/references/derived-from-children.md`, next to this skill's folder):

```java
@Subscribe(id = "linesDc", target = Target.DATA_CONTAINER)
public void onLinesDcCollectionChange(final CollectionContainer.CollectionChangeEvent<OrderLine> event) {
    if (event.getChangeType() != CollectionChangeType.REFRESH) {
        getEditedEntity().setTotal(orderTotals.total(getEditedEntity()));
    }
}

@Subscribe(id = "linesDc", target = Target.DATA_CONTAINER)
public void onLinesDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<OrderLine> event) {
    getEditedEntity().setTotal(orderTotals.total(getEditedEntity()));
}
```
