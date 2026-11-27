---
name: jmix-create-detail-view
description: Create a Jmix Flow UI detail view with XML descriptor, save-close action, messages, and policies.
---

# Create Detail View

## Steps

1. Create the Java controller under `view/<entityname>/`.
2. Extend `StandardDetailView<Entity>`.
3. Add `@Route(value = ".../:id", layout = MainView.class)`. In a Jmix add-on module, which has no `MainView` and must not reference the application, use `layout = DefaultMainViewParent.class` (`io.jmix.flowui.view.DefaultMainViewParent`); the application's main view is used as the parent at runtime.
4. Add `@ViewController(id = "Entity.detail")`.
5. Add `@ViewDescriptor(path = "entity-detail-view.xml")`.
6. Add `@EditedEntityContainer("<entity>Dc")`.
7. Create the XML descriptor with instance container, loader, `dataLoadCoordinator`, typed form fields, `detail_saveClose`, and `detail_close`. If the view shows a to-many attribute, first read `references/to-many-attribute.md` in this skill's folder. If the form has a `tabSheet`, read **When the form has a `tabSheet`** below.
8. Configure reference fields with a verified data source: lookup action, `itemsContainer`, or `itemsQuery`; first read `references/reference-fields.md` in this skill's folder.
9. Use `InitEntityEvent` for UI-only defaults only. A required persistent default belongs at the entity layer — see **Required-field defaults** in `jmix-create-entity`.
10. If the save needs cross-field or manual validation, read **When the save needs cross-field or manual validation** below. If the save must ask the user for confirmation, first read `references/save-confirmation.md` in this skill's folder.
11. Add message keys for the title and field labels.
12. Grant access for roles that can open the detail view with `@ViewPolicy(viewIds = "Entity.detail")` — declared on a **method of a `@ResourceRole` interface** (the annotation is `@Target(METHOD)`), not on the view controller. The annotation has no `value()` member: `@ViewPolicy("...")` does not compile; use `viewIds = "..."` (or `viewClasses = ...`). See `jmix-create-resource-role`.
13. Before finishing, compare every form field component against the Java property type (**Final XML Type Audit**).

## Two render-time failures that compile clean

`compileJava` does not check `*-view.xml`, so both pass it and throw when the view is opened. Only a Jmix-aware inspection catches them; the mechanical checks do not. With no inspection, write them right from the start:

1. **An enum attribute is never `entityComboBox`.** `entityComboBox` is for entity references; binding it to an enum (with or without a made-up `enumClass` attribute) throws `IllegalStateException: Range is enumeration` at render. There is no `enumClass` attribute on `entityComboBox`. For a Jmix enum property use a plain `<comboBox>` or `<select>`; Jmix fills it from the enum:

   ```xml
   <!-- WRONG: <entityComboBox property="category" enumClass="...SomeEnum"/> -->
   <comboBox id="categoryField" property="category"/>   <!-- enum: just bind the property -->
   ```

2. **`itemsQuery` must wrap its JPQL in a nested `<query>` element**, and that query must reference `:searchString` (the combo passes it for type-ahead). Raw CDATA directly under `<itemsQuery>` throws `GuiDevelopmentException: Nested 'query' element is missing`; a query that ignores `:searchString` throws `DevelopmentException: Parameter 'searchString' is not used in the query` at dropdown fetch. The correct shape is in `references/reference-fields.md` in this skill's folder.

## Controller Template

```java
@Route(value = "customers/:id", layout = MainView.class)
@ViewController(id = "Customer.detail")
@ViewDescriptor(path = "customer-detail-view.xml")
@EditedEntityContainer("customerDc")
public class CustomerDetailView extends StandardDetailView<Customer> {
}
```

## XML Skeleton

```xml
<view xmlns="http://jmix.io/schema/flowui/view"
      title="msg://customerDetailView.title"
      focusComponent="form">
    <data>
        <instance id="customerDc" class="com.company.app.entity.Customer">
            <fetchPlan extends="_base"/>
            <loader id="customerDl"/>
        </instance>
    </data>
    <facets>
        <dataLoadCoordinator auto="true"/>
    </facets>
    <actions>
        <action id="saveCloseAction" type="detail_saveClose"/>
        <action id="closeAction" type="detail_close"/>
    </actions>
    <layout>
        <formLayout id="form" dataContainer="customerDc">
            <textField id="nameField" property="name"/>
        </formLayout>
        <hbox id="detailActions">
            <button id="saveAndCloseButton" action="saveCloseAction"/>
            <button id="closeButton" action="closeAction"/>
        </hbox>
    </layout>
</view>
```

Always declare both `detail_saveClose` and `detail_close`: a detail view without a save action is broken for create flows. Use editable form components (`textField`, `comboBox`, `entityComboBox`, etc.) without hard-coded `readOnly="true"`, unless an attribute is permanently read-only in domain terms (for example audit timestamps). If the task says records open in read mode, first read `references/read-mode.md` in this skill's folder.

Do not expose technical fields (`id`, `version`) in user-facing forms. Hide parent/default fields only when they are initialized elsewhere.

To style a field, a card, or a component you build in the controller (CSS classes, theme variants, `getStyle().set(...)`, `--aura-*` / `--lumo-*` tokens), read `jmix-style-ui` first: a token the active theme does not define fails silently and passes every gate.

## Field Component Mapping

| Property type | Component |
| --- | --- |
| `String` short text | `textField` |
| `String` long text | `textArea` |
| `Integer` | `integerField` |
| `Long` | `integerField` or `numberField` according to project usage |
| `BigDecimal` | `bigDecimalField` |
| `Boolean` | `checkbox` |
| `LocalDate` | `datePicker` |
| `LocalDateTime` | `dateTimePicker` |
| Jmix enum | `select` or `comboBox` |
| Entity reference | `entityComboBox` or `entityPicker` |

If a free-text field uses `valueChangeMode` `EAGER`, `LAZY`, or `TIMEOUT`, first read `references/value-change-mode.md` in this skill's folder. If a `comboBox` gets its items in the controller, read **When a `comboBox` gets its items in the controller** below.

A checkbox bound to a `@NotNull Boolean` takes the required state from the entity, but Vaadin treats an unchecked checkbox as empty: the form rejects `false` and save leaves the view open. When unchecked `false` is a valid value, declare `required="false"`; the entity constraint still rejects `null`:

```xml
<checkbox id="subscribedField" property="subscribed" required="false"/>
```

## Final XML Type Audit

After creating or editing the descriptor, inspect each field:

- `Integer` is not a `textField`; use `integerField`.
- `BigDecimal` is not a `textField`; use `bigDecimalField`.
- Date/time properties use date/time picker components.
- Boolean properties use checkbox or the project's boolean component pattern. A checkbox bound to an `@NotNull Boolean` that may be `false` declares `required="false"`.
- Entity references use reference components, not text fields.
- A free-text field with `valueChangeMode` `EAGER`, `LAZY`, or `TIMEOUT` keeps whitespace while typing (`trimEnabled="false"`).
- A `comboBox` whose items are set in `BeforeShowEvent` is not bound in the descriptor.

If an existing project uses a different compiled pattern for a type, follow the existing pattern and keep it consistent.

## Forbidden

- Using list-view route or id patterns for detail views.
- Missing `detail_saveClose`, or a "read-only" detail view with only `detail_close` and every field `readOnly="true"`.
- Using `textField` for numeric, date/time, boolean, or reference properties.
- `entityComboBox` for an enum attribute, or an `enumClass` attribute on it (`Range is enumeration` at render).
- `itemsQuery` JPQL without a nested `<query>` element, or without `:searchString`.
- `itemsQuery` without `searchStringFormat` and `escapeValueForLike` (empty dropdown, every gate green).
- Reference fields without a working lookup action, `itemsContainer`, or verified `itemsQuery`.
- Using `@Table` names in `itemsQuery`.
- `itemsQuery` with unresolved `container_` or `component_` parameters.
- Invented action ids on `entityPicker`; use `entity_lookup` and `entity_clear`.
- A required persistent default set only in `InitEntityEvent`.
- Hardcoded labels or titles.
- Hiding required fields without setting defaults elsewhere.
- Exposing `id` or `version` in user-facing forms.
- A checkbox bound to an `@NotNull Boolean` without `required="false"` when unchecked `false` is a valid value.
- `@ViewPolicy` on the view controller, or `@ViewPolicy("...")` without `viewIds =`.
- Missing view policy for dialog-opened detail views.
- A `comboBox` bound in the descriptor while its items are set in `BeforeShowEvent`.
- A confirmation dialog in `ValidationEvent`, or in `BeforeSaveEvent` without `preventSave()` / `resume()`.
- A `class` attribute on a nested property-bound `<collection property="..."/>`.
- `<markdown>` with an empty or absent `content` attribute (throws at view load), or a guessed `io.jmix.flowui...Markdown` import.
- CSS custom properties from a theme the app does not run (e.g. `--lumo-*` in an Aura app) — see `jmix-style-ui`.

## Special cases

### When a `comboBox` gets its items in the controller

If the items are set in `BeforeShowEvent`, do not bind the `comboBox` in the descriptor: the binding sets the value when the entity arrives, before `setItems`, and opening a saved record fails. Either call `setItems(…)` in `InitEvent`, or bind with `setValueSource(new ContainerValueSource<>(container, "property"))`.

### When the save needs cross-field or manual validation

Add a `@Subscribe` handler on `ValidationEvent` and report failures with `event.getErrors().add("...")`. For programmatic checks (for example before a custom save) use the `ViewValidation` bean (`validateUiComponents`, `showValidationErrors`).

### When the form has a `tabSheet`

Do not rely only on `required` or field validators for an invariant on a secondary tab. Inactive tab content is disabled, and field validation skips disabled components. Keep those checks as UI hints, but enforce the save invariant in a `ValidationEvent` handler that reads `getEditedEntity()`, not only the enabled fields. Test saving without visiting the secondary tab and after switching away from it; neither path may save invalid state.

