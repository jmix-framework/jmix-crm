---
name: jmix-create-list-view
description: Create a Jmix Flow UI list view with XML descriptor, data loading, menu, messages, and policies.
---

# Create List View

Use this skill when creating a top-level list/search view for an entity.

## Steps

1. Create Java controller under `view/<entityname>/`.
2. Extend `StandardListView<Entity>`.
3. Add `@Route(value = "...", layout = MainView.class)`.
4. Add `@ViewController(id = "Entity.list")`.
5. Add `@ViewDescriptor(path = "entity-list-view.xml")`.
6. Add `@LookupComponent("<entities>DataGrid")`.
7. Create XML descriptor with collection container, loader, `dataLoadCoordinator`, grid actions, toolbar buttons, and columns. Add `<urlQueryParameters>` when the view includes filter or pagination — always paired with the matching components (see below).
8. Verify every JPQL query uses the JPA/Jmix entity name, not the database table name.
9. Render a visible button for every grid action that users must trigger.
10. For every `<urlQueryParameters>` child, confirm a component with the matching `id` exists in the same descriptor — otherwise the view crashes at init with `Component with id '<x>' not found`.
11. Add a `menu.xml` item for list views that should appear in navigation.
12. Add message keys for title, menu, and custom button captions.
13. Grant access for roles that can open the view with `@ViewPolicy(viewIds = "Entity.list")` and `@MenuPolicy(menuIds = "Entity.list")` — declared on a method of a `@ResourceRole` interface (both are `@Target(METHOD)`), not the view controller. Neither annotation has a `value()` member: `@ViewPolicy("...")` / `@MenuPolicy("...")` do not compile. See `jmix-create-resource-role`.

## Controller Template

```java
@Route(value = "customers", layout = MainView.class)
@ViewController(id = "Customer.list")
@ViewDescriptor(path = "customer-list-view.xml")
@LookupComponent("customersDataGrid")
@DialogMode(width = "64em")
public class CustomerListView extends StandardListView<Customer> {
}
```

## XML Skeleton

```xml
<view xmlns="http://jmix.io/schema/flowui/view"
      title="msg://customerListView.title"
      focusComponent="customersDataGrid">
    <data>
        <collection id="customersDc" class="com.company.app.entity.Customer">
            <fetchPlan extends="_base"/>
            <loader id="customersDl" readOnly="true">
                <query><![CDATA[select e from Customer e]]></query>
            </loader>
        </collection>
    </data>
    <facets>
        <dataLoadCoordinator auto="true"/>
    </facets>
    <layout>
        <hbox id="buttonsPanel" classNames="buttons-panel">
            <button id="createButton" action="customersDataGrid.createAction"/>
            <button id="editButton" action="customersDataGrid.editAction"/>
            <button id="removeButton" action="customersDataGrid.removeAction"/>
        </hbox>
        <dataGrid id="customersDataGrid" dataContainer="customersDc">
            <actions>
                <action id="createAction" type="list_create"/>
                <action id="editAction" type="list_edit"/>
                <action id="removeAction" type="list_remove"/>
            </actions>
            <columns>
                <column property="name"/>
            </columns>
        </dataGrid>
    </layout>
</view>
```

## Choosing list-action types

The grid action that opens a row is one of:

- `list_create` — opens detail view for a new entity.
- `list_edit` — opens detail view in edit mode.
- `list_read` — opens detail view in an open read-only MODE (existing
  records are viewed but not modified). It is a mode, not a `readOnly`
  descriptor attribute.
- `list_remove` — deletes the selected entity.

`list_read` REPLACES `list_edit`, not the whole CRUD bar. "The list
opens records in read mode" or "use `read` instead of `edit`" still
leaves `list_create` and `list_remove` in place — drop them only when
creation or deletion is explicitly forbidden. A list with only a read
action and no create/remove is almost always wrong unless a fully
read-only list was specifically requested.

## Custom (non-standard) buttons MUST carry their own caption

A button bound to a standard grid action (`createAction`, `editAction`,
`removeAction`) auto-resolves its caption from the action. A CUSTOM
button or action you add (e.g. one that calls a service or opens a
dialog) has NO caption unless you give it one — and a UI test locates a
button by its visible text, so a blank button is untargetable and the
test fails. Always add `text="msg://..."` and a matching message key.

```xml
<hbox id="buttonsPanel" classNames="buttons-panel">
    <button id="createButton" action="customersDataGrid.createAction"/>
    <button id="removeButton" action="customersDataGrid.removeAction"/>
    <!-- custom button: needs its OWN caption (action-bound buttons do not) -->
    <button id="actionButton" text="msg://actionButton.text"/>
</hbox>
```

If you instead wire a custom `<action>` (not a standard `list_*` type),
the action carries the caption: `<action id="customAction"
text="msg://customAction.text"/>`. Either way the visible text must
resolve, or it cannot be clicked. Add the key to
`messages_en.properties`.

A custom button's `clickListener` / `@Subscribe` handler takes
`ClickEvent<JmixButton>` (import `io.jmix.flowui.kit.component.button.JmixButton`).
The wrong event type fails `compileJava` with an argument type mismatch
at the click handler.

## Icons

DO NOT invent Vaadin icon names. The `VaadinIcon` enum is small and
irregular; a single typo like `VaadinIcon.ARROW_UP_DOWN` — no such
constant — crashes the entire view at render time.

- When unsure which icon to use, OMIT the `icon` attribute entirely.
  The button works without it.
- Reuse only icon names you have seen in this project. Grep existing
  view XML for `icon="` and copy what you find.
- Before typing a new `VaadinIcon` constant, verify it exists (grep the
  project, or use Context7 / IDE symbol lookup) — see `jmix-verify-api-symbol`.

## URL Query Parameters — add alongside filter and pagination

When a list view has filter, pagination, or other stateful components
whose state should survive page reload and be shareable via URL, add
`<urlQueryParameters>` bindings. This is the recommended pattern for
any non-trivial list view; the seed-scaffolded `user-list-view.xml`
ships it by default.

```xml
<facets>
    <urlQueryParameters>
        <genericFilter component="genericFilter"/>
        <pagination component="pagination"/>
    </urlQueryParameters>
</facets>
...
<genericFilter id="genericFilter" dataLoader="customersDl">...</genericFilter>
<simplePagination id="pagination" dataLoader="customersDl"/>
```

**Both halves are mandatory.** Every `<urlQueryParameters>` child binds
to a layout component BY ID; if that component is missing, the view
CRASHES AT INIT with `Component with id '<x>' not found` — taking down
every test that opens the view. When copying from another view (e.g.
`user-list-view.xml`), copy BOTH the binding AND the component, or
NEITHER.

Omit `<urlQueryParameters>` entirely when the view has no filter or
pagination (e.g. a small static reference table).

Self-check: for EVERY `<urlQueryParameters>` child, grep the SAME file
for a component whose `id` equals the `component="..."` value. If it
is absent, either add the matching component to the layout
(`<simplePagination id="pagination" dataLoader="...Dl"/>` or
`<genericFilter id="genericFilter" dataLoader="...Dl">`) or delete that
entry.

## JPQL Entity Names

JPQL queries use entity names, not table names. If an entity has a table suffix or custom table name, keep the JPQL entity name as the Java entity name unless the entity declares a custom JPA entity name.

```java
@Table(name = "PRODUCT_")
@Entity
public class Product {
}
```

```xml
<query><![CDATA[select e from Product e]]></query>
```

Do not write `select e from PRODUCT_ e` or `select e from Product_ e` unless the entity itself is named that way in JPA metadata.

## Standard load through `<loader><query>` — no delegate needed

A `StandardListView` loads through the `<loader><query>` you declare in the
XML. You do NOT need an `@Install(target = Target.DATA_LOADER)` load delegate;
if you DO write one, it must return `List<E>` — returning the `LoadContext`
itself means the query never runs and the grid is empty at open.

## Forbidden

- Declaring actions without visible buttons or another reachable UI trigger.
- Using `@Table` names in JPQL.
- `urlQueryParameters` references to component ids that are not declared in the XML.
- Java controller without matching XML descriptor.
- XML descriptor without matching `@ViewDescriptor`.
- Hardcoded title text.
- Invented or unverified icon names.
- Missing role view policy.
- Adding menu policy for dialog-only detail views.
- A load delegate returning `LoadContext` instead of `List<E>` (the query never runs; the grid is empty at open).
