---
name: jmix-create-composition-detail-view
description: Add inline editable parent-child composition UI to a Jmix detail view, when a parent entity owns child records edited via a property-bound collection container (no query loader).
---

# Create Composition Detail Editing

Use this skill when a parent entity owns child entities edited inside the parent detail view through a property-bound `<collection property=...>` container (no query loader). For a button/action that opens a standalone detail dialog, or a master-row selection that filters an independently query-loaded child grid, use `jmix-add-dialog-detail-flow` instead.

## The child's OWN detail view must exist

A child `dataGrid` with a `list_create`/`list_edit` action opens the
child's detail view as a DIALOG. That view must EXIST as a real,
separately created view — controller + XML with a stable id of the form
`<Child>.detail`. Wiring the action without creating the view throws
`NoSuchViewException: View '<Child>.detail' is not defined` the moment
the user clicks "+", and any `@ViewPolicy(viewIds = "<Child>.detail")` you grant
would point at a view id that does not resolve. Creating the child detail
view (step 2) is not optional.

Each button's `action="<grid-id>.<actionId>"` must name the dataGrid's
own `id` and an `<action>` declared inside that grid — otherwise the
button silently does nothing or the view fails to open. A clean compile
never surfaces any of this.

## Steps

1. Ensure the entity model is a real composition (model it with `jmix-create-entity`):
   - parent collection has `@Composition`;
   - child has non-null parent back reference;
   - parent has cascade delete if child lifecycle belongs to parent.
2. Create a child detail view with `jmix-create-detail-view`.
3. In the parent detail XML, add a collection container for the child collection.
4. Add a child `dataGrid` bound to that collection container.
5. Add `list_create`, `list_edit`, and `list_remove` actions to the child grid.
6. Render visible child-grid buttons for create, edit, and remove.
7. Add `openMode=DIALOG` to create/edit actions.
8. Add view policy for the child detail view (see `jmix-create-resource-role`).
9. Add messages for child entity and view title (see `jmix-add-i18n-keys`).

The child collection container is property-bound (`<collection
property=...>`): no loader or query, and the parent fetchPlan must
include the child property so the children load with the parent.

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
- Child collection actions without visible buttons or another reachable UI trigger.
- Nullable child parent reference.
- Missing child detail view policy.
- Editing composition children outside the parent aggregate when the domain says they are owned.
