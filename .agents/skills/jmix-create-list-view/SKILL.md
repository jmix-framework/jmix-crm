---
name: jmix-create-list-view
description: Create a Jmix Flow UI list view with XML descriptor, data loading, menu, messages, and policies.
---

# Create List View

Use this skill when creating a top-level list/search view for an entity.

## Steps

1. Create the Java controller under `view/<entityname>/`, extending `StandardListView<Entity>`.
2. Add `@Route(value = "...", layout = MainView.class)`. In a Jmix add-on module, which has no `MainView` and must not reference the application, use `layout = DefaultMainViewParent.class` (`io.jmix.flowui.view.DefaultMainViewParent`); the application's main view is used as the parent at runtime.
3. Add `@ViewController(id = "Entity.list")` and `@ViewDescriptor(path = "entity-list-view.xml")`.
4. Add `@LookupComponent("<entities>DataGrid")` when the view can be opened as a lookup: an entity-picker dialog via `@PrimaryLookupView` or an explicit lookup builder call. A management/browse screen that is never used as a picker can omit it; the annotation only backs `getLookupComponent()`/`findLookupComponent()` in the lookup flow.
5. Create the XML descriptor with collection container, loader, `dataLoadCoordinator`, grid actions, toolbar buttons, and columns.
   - If the entity has a reference to its own class, first read `references/hierarchy.md` in this skill's folder.
   - If the view has a filter, pagination, or other component whose state should survive a page reload, first read `references/url-query-parameters.md` in this skill's folder.
   - If the view has a `<settings>` facet, or the user's grid layout should persist between visits, first read `references/settings-facet.md` in this skill's folder.
6. Check that every JPQL query uses the JPA/Jmix entity name, not the database table name.
7. Render a visible button for every grid action that users must trigger.
8. Add a `menu.xml` item for list views that should appear in navigation.
9. Add message keys for title, menu, and custom button captions.
10. Grant access for roles that can open the view with `@ViewPolicy(viewIds = "Entity.list")` and `@MenuPolicy(menuIds = "Entity.list")`, declared on a method of a `@ResourceRole` interface (both are `@Target(METHOD)`), not on the view controller. Neither annotation has a `value()` member: `@ViewPolicy("...")` / `@MenuPolicy("...")` do not compile. See `jmix-create-resource-role`.

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

- `list_create` opens the detail view for a new entity.
- `list_edit` opens the detail view in edit mode.
- `list_read` opens the detail view in read-only mode: existing records are viewed but not modified. It is a mode, not a `readOnly` descriptor attribute.
- `list_remove` deletes the selected entity.

`list_read` replaces `list_edit`, not the whole CRUD bar. "The list opens records in read mode" or "use `read` instead of `edit`" still leaves `list_create` and `list_remove` in place; drop them only when creation or deletion is explicitly forbidden. A list with only a read action and no create/remove is almost always wrong unless a fully read-only list was requested.

## Custom buttons need their own caption

A button bound to a standard grid action (`createAction`, `editAction`, `removeAction`) gets its caption from the action. A custom button or action (for example, one that calls a service or opens a dialog) has no caption unless you give it one, and a UI test finds a button by its visible text, so it cannot click a blank one. Always add `text="msg://..."` and a matching key in `messages_en.properties`.

```xml
<hbox id="buttonsPanel" classNames="buttons-panel">
    <button id="createButton" action="customersDataGrid.createAction"/>
    <button id="removeButton" action="customersDataGrid.removeAction"/>
    <!-- custom button: needs its OWN caption (action-bound buttons do not) -->
    <button id="actionButton" text="msg://actionButton.text"/>
</hbox>
```

If you wire a custom `<action>` (not a standard `list_*` type) instead, the action carries the caption: `<action id="customAction" text="msg://customAction.text"/>`.

A custom button's `clickListener` / `@Subscribe` handler takes `ClickEvent<JmixButton>` (import `io.jmix.flowui.kit.component.button.JmixButton`). A handler with the wrong event type compiles, then throws `IllegalArgumentException: argument type mismatch` at the click. `jmix-add-dialog-detail-flow` has the full rule.

## Icons

Do not invent Vaadin icon names. The `VaadinIcon` enum is small and irregular; one wrong name, such as `VaadinIcon.ARROW_UP_DOWN` (no such constant), crashes the whole view at render time.

- When unsure which icon to use, leave out the `icon` attribute. The button works without it.
- Reuse only icon names you have seen in this project: grep existing view XML for `icon="` and copy what you find.
- Before typing a new `VaadinIcon` constant, verify it with `jmix-verify-api-symbol`.

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

## Data loading

A `StandardListView` loads through the `<loader><query>` you declare in the XML. You do not need an `@Install(target = Target.DATA_LOADER)` load delegate. If you write one, it must return `List<E>`: returning the `LoadContext` itself means the query never runs and the grid is empty at open.

## Overriding `beforeEnter`: always call `super`

The `dataLoadCoordinator` auto-load runs inside `StandardListView.beforeEnter(...)`. An override that skips the `super` call, or calls it only on one branch, leaves the grid empty with no exception, no warning, a clean compile and a clean render.

```java
public class OrderListView extends StandardListView<Order> implements BeforeEnterObserver {

    @ViewComponent
    private CollectionLoader<Order> ordersDl;

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // custom query/parameter handling FIRST
        List<String> customerIds = event.getLocation().getQueryParameters()
                .getParameters().getOrDefault("customerId", List.of());
        if (!customerIds.isEmpty()) {
            ordersDl.setQuery("select e from Order e where e.customer.id = :customerId");
            ordersDl.setParameter("customerId", UUID.fromString(customerIds.getFirst()));
        }

        // then ALWAYS, on every path — the auto-load happens in here
        super.beforeEnter(event);
    }
}
```

The same holds for every overridden view lifecycle method (`beforeEnter`, `afterNavigation`): do your work, then call `super`, on every code path. An early `return` before `super` is the defect.

## Column widths: `rem` or px, never `em`

```xml
<column property="number" width="9rem"/>   <!-- RIGHT -->
<column property="number" width="9em"/>    <!-- WRONG: header and body drift apart -->
```

`em` resolves against the font size of the element it is set on, and grid header and body cells are separate elements. Under the Lumo (compatibility) theme the header font is 14px (`--lumo-font-size-s`) and the body font 16px (`--lumo-font-size-m`): `9em` is 126px in the header and 144px in the body, and the difference adds up column by column to the right. Under the default Aura theme the two sizes are currently equal, but any theme or project CSS that sets the public `--vaadin-grid-header-font-size` brings the drift back. Vaadin's Grid documentation also says to use `rem`, not `em`. On a narrow table the first columns still line up, so a small grid does not show the defect.

If the view has a `<settings>` facet and you change column width, order or visibility, first read `references/settings-facet.md` in this skill's folder.

## Forbidden

- Declaring actions without visible buttons or another reachable UI trigger.
- Using `@Table` names in JPQL.
- Java controller without matching XML descriptor.
- XML descriptor without matching `@ViewDescriptor`.
- Hardcoded title text.
- A custom button or custom action without `text="msg://..."` (blank caption; a UI test cannot click it).
- A custom button handler typed with anything other than `ClickEvent<JmixButton>` (`IllegalArgumentException: argument type mismatch` at the click).
- Dropping `list_create` / `list_remove` when only "read instead of edit" was asked.
- Treating `list_read` as a `readOnly` descriptor attribute (it is an action type that opens the detail view in read-only mode).
- Invented or unverified icon names.
- `layout = MainView.class` in an add-on module (use `DefaultMainViewParent.class`).
- Missing role view policy.
- `@ViewPolicy` / `@MenuPolicy` on the view controller, or written as `@ViewPolicy("...")` (no `value()` member; does not compile).
- Adding menu policy for dialog-only detail views.
- A load delegate returning `LoadContext` instead of `List<E>` (the query never runs; the grid is empty at open).
- An overridden `beforeEnter` (or other lifecycle method) that does not call `super` on every path, including an early `return` (the auto-load never runs; the grid is empty).
- `<column width="…em">`: header and body resolve `em` against different font sizes and drift apart.
- `urlQueryParameters` references to component ids that are not declared in the XML (`Component with id '<x>' not found` at view init).
- On a view with a `<settings>` facet: verifying a change to column width, order or visibility in the browser without clearing saved user settings first (the check returns a false negative).
