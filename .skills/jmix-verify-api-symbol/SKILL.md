---
name: jmix-verify-api-symbol
description: Before typing a class, enum constant, inner-class event, method, or icon name you have not personally verified in this project, confirm it exists. Costs seconds and prevents the most expensive class of failures (hallucinated icon constants, fake event inner classes, wrong package paths). Primary check when connected = the Context7 docs MCP (/jmix-framework/jmix-context7); falls back to grepping the project for a working example or an IDE symbol search.
---

# Verify API symbols before you type them

A top cause of failed runs is confidently typed API names that do not exist —
they look right, survive typing, then break compile or, worse, at render time.
This is a seconds-long pre-flight check. Run it BEFORE writing any symbol you
have not already seen in this project's source tree.

## When to run

Before typing any of:

- A Vaadin enum constant (e.g. a `VaadinIcon.X`)
- A class qualified by a guessed package ("is `Dialogs` in `io.jmix.flowui` or
  `io.jmix.flowui.dialogs`?")
- An event inner class ("does `JmixButton.ClickEvent` exist, or is it
  `com.vaadin.flow.component.ClickEvent<JmixButton>`?")
- A builder-style method ("`.withEntity()` or `.editEntity()` on
  `DialogWindows.detail(...)`?")
- A Jmix action type id or a security-role enum constant you have not used yet

Skip it only when you can point at the same exact symbol already in use in this
project's `src/`.

## How to verify — MCP first, floor always

**Free short-circuit:** if the EXACT symbol is already used in this project's
`src/`, copy that call site — it is ground truth, no lookup needed:

```bash
grep -rn "DialogWindows" src/main/java --include='*.java'
grep -rn "VaadinIcon\." src/main --include='*.xml' --include='*.java'
```

For anything NOT already in the project, verify it before you type it:

1. **Context7 — PRIMARY when connected.** Query the Context7 docs MCP with the
   Jmix library id `/jmix-framework/jmix-context7` for the exact symbol (a class,
   an `StandardOutcome` constant, the `DialogWindows.detail(...).editEntity(...)`
   shape, valid `@Install(subject=...)` names, `io.jmix.flowui.Dialogs` vs raw
   Vaadin `Dialog`). It resolves the API from the official Jmix docs — your best
   check for a symbol new to the project.

2. **IDE symbol search — if available.** Confirm the class/constant exists and read
   its fully-qualified name and members to settle package guesses and whether a
   constant or inner type exists.

3. **Floor (no MCP): grep a known-good example and reuse only what is actually
   there.** Find a real call site in the wider codebase or a reference app and copy
   its exact shape — the portable equivalent when (1) and (2) are unavailable.

Never invent and ship. If nothing confirms a symbol, do not type it — pick one you
CAN confirm, or omit the optional decoration (e.g. drop an icon attribute rather
than guess a constant).

## The recurring garbage list

Symbols commonly invented. NEVER type these — verify first:

| You might type                                       | Reality                                                      |
|------------------------------------------------------|--------------------------------------------------------------|
| an invented `VaadinIcon` constant                    | does not exist; the icon enum is small and irregular — pick from existing constants or omit |
| `JmixButton.ClickEvent`                              | use `com.vaadin.flow.component.ClickEvent<JmixButton>`       |
| `DataGrid.ReadEvent`, `DataGrid.SelectionEvent`      | use `com.vaadin.flow.data.selection.SelectionEvent<DataGrid<E>, E>` |
| `Target.DATA_GRID`                                   | not a Jmix `@Subscribe` target — use `Target.COMPONENT` with explicit id |
| `io.jmix.flowui.dialogs.Dialogs`                     | actual: `io.jmix.flowui.Dialogs`                             |
| `io.jmix.core.entity.EntityStates`                   | actual: `io.jmix.core.EntityStates`                          |
| `io.jmix.flowui.component.datagrid.DataGrid`         | actual: `io.jmix.flowui.component.grid.DataGrid`             |
| `dialogs.createDetailView(this, entity, View.class)` | use `dialogWindows.detail(this, EntityClass.class).editEntity(entity).withViewClass(View.class)` |
| `dataGrid.addItemChangeListener(...)`                | use `addSelectionListener(...)` or `asSingleSelect().addValueChangeListener(...)` |
| `dataGrid.getSingleSelected()`                       | use `getSingleSelectedItem()`                                |

## Cost vs benefit

A verification check takes ~1 second; a failed `compileJava` cycle costs
15–30 seconds plus error-log parsing, and a passed compile that fails at render
time in a UI test costs the whole test run. The break-even is one prevented
failure per session — run the check: Context7 if connected, else grep a
known-good call site (step 3); never ship an unverified symbol.
