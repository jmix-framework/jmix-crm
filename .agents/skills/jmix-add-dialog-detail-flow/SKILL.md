---
name: jmix-add-dialog-detail-flow
description: Open a Jmix entity detail view from a button/action and refresh related data after save.
---

# Add Dialog Detail Flow

Use this skill when a button or action should create or edit an entity in a modal detail view, or collect a small scalar value through a Jmix input dialog. For parent-owned children edited inside the parent's own detail view via a property-bound `<collection property=...>` container (no query loader), use `jmix-create-composition-detail-view` instead.

## Steps

1. Decide what the button does. If it only collects a scalar value for a service call and creates no entity, first read `references/scalar-value-dialog.md` in this skill's folder. If it now collects a value and calls a service but must create an entity instead, first read `references/service-call-to-entity.md` in this skill's folder. Otherwise continue with the next steps.
2. Inject `DialogWindows` with `@Autowired`. Inject with `@ViewComponent` the loaders to reload, the grid you read, and any child containers that must be cleared.
3. Type the button handler parameter as `ClickEvent<JmixButton>`; see **`IllegalArgumentException: argument type mismatch` at a button click** below.
4. Open `DialogWindows.detail(this, Entity.class).newEntity()` or `.editEntity(entity)`. Use `.withInitializer(...)` to set parent/default fields. If the new entity is a child of the selected master row, first read `references/child-of-master-row.md` in this skill's folder.
5. In `.withAfterCloseListener(...)`, reload every affected loader only after `StandardOutcome.SAVE`. Open an independent dialog without `withContainer`, `withField` or `withParentDataContext`; do not attach a container or field only to refresh the opener.
6. A plain `DialogWindows.detail(...)` writes to the database on Save; a later Cancel on the opening view does not undo it. If Save must wait for the opening view's Save, or you pass `withContainer`, `withField` or `withParentDataContext`, first read `references/deferred-save.md` in this skill's folder.
7. If a child grid shows the rows of the selected master row, first read `references/child-grid-filter.md` in this skill's folder. If you install a selection listener on the master grid, first read `references/master-selection-listener.md` in this skill's folder.
8. Make sure the opened detail view exists and roles have its `@ViewPolicy` (see `jmix-create-detail-view` and `jmix-create-resource-role`).

## Template

```java
@Subscribe("createButton")
public void onCreateButtonClick(final ClickEvent<JmixButton> event) {
    dialogWindows.detail(this, Order.class)
            .newEntity()
            .withAfterCloseListener(closeEvent -> {
                if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                    ordersDl.load();
                }
            })
            .open();
}
```

## `IllegalArgumentException: argument type mismatch` at a button click

This error at the click, before any dialog opens, means the `@Install(subject="clickListener")` / `@Subscribe` handler has the wrong parameter type. The code compiles.

- A `clickListener` delivers a `ClickEvent<JmixButton>` (import `io.jmix.flowui.kit.component.button.JmixButton`). A handler typed `Component` or `ActionPerformedEvent` cannot receive it, so reflection throws.
- The cause is not the `InputParameter` or the dialog. Changing the `InputParameter` type (`intParameter` / `stringParameter` / `withType` / parse) leaves the click broken; fix the handler parameter to `ClickEvent<JmixButton>` first.
- `InputParameter.intParameter("q")` is the correct scalar form; do not switch to `stringParameter` + `Integer.parseInt`.

```java
// WRONG — throws "argument type mismatch" at click:
@Install(to = "actionButton", subject = "clickListener")
private void onClick(final ActionPerformedEvent event) { ... }   // or (Component event)

// RIGHT:
@Subscribe("actionButton")
public void onClick(final ClickEvent<JmixButton> event) { ... }
```

## Forbidden

- Raw Vaadin `Dialog` for entity create/edit flows.
- Raw Vaadin `Dialog` for ordinary scalar input workflows.
- Direct service update when the domain requires creating an entity record.
- An `InputDialog` / `MessageDialog` / service call before the detail dialog when the button should open the detail view directly.
- Refreshing loaders after cancel/close when no save occurred.
- Refreshing grids with `getDataProvider().refreshAll()` instead of loader reload.
- Attaching a container or field to an independent dialog only to refresh the opener.
- Expecting Cancel on the opening view to undo the Save of a plain `DialogWindows.detail(...)`.
- Comparing entity-valued JPQL properties to UUID parameters.
- Using `.selected` inside a `:container_*` JPQL parameter.
- Calling `load()` on a loader after removing a JPQL parameter required by its query.
- A manual-`:param` loader (not `:container_*` / `:component_*`) under `<dataLoadCoordinator auto="true"/>` — it throws at view open.
- No-argument installed selection listeners for `DataGrid` selection changes.
- A button `clickListener` / `@Subscribe` handler typed `Component` or `ActionPerformedEvent` instead of `ClickEvent<JmixButton>`.
- `stringParameter` + `Integer.parseInt` for a numeric input — use `InputParameter.intParameter(...)`.
