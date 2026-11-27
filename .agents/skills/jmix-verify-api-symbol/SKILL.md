---
name: jmix-verify-api-symbol
description: Use before typing a class, method, enum constant, event inner class, or icon name that you have not seen in this project. Checks that it exists in the project's Jmix version.
---

# Verify API symbols before you type them

API names typed from memory often do not exist. They look right, then break
`compileJava` or, worse, fail only at render time. Check every symbol you have
not already seen in this project's source tree before you type it.

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

Skip it only when the same exact symbol is already in use in this project's
`src/`.

## How to verify

If the exact symbol is already used in this project's `src/`, copy that call
site; it is ground truth and needs no lookup:

```bash
grep -rn "DialogWindows" src --include='*.java'
grep -rn "VaadinIcon\." src --include='*.xml' --include='*.java'
```

For anything not already in the project, go down this list and stop at the
first check that confirms the symbol. Use a later check only when the earlier
ones are not available or do not confirm it. A tool listed by name only is
available: load it with your tool search.

1. **Docs MCP, the first check when connected.** Query the official Jmix docs
   for the exact symbol (a class, a `StandardOutcome` constant, the
   `DialogWindows.detail(...).editEntity(...)` shape, valid
   `@Install(subject=...)` names, `io.jmix.flowui.Dialogs` vs raw Vaadin
   `Dialog`). It is the best check for a symbol new to the project. Use the
   first of these two servers that works:

   - **Context7.** Query it with the Jmix library id
     `/jmix-framework/jmix-context7`.
   - **Jmix docs MCP.** Use it when no Context7 tool is available (their names
     contain `context7`), or when a Context7 call fails because the API key is
     missing or rejected. It is usually registered as `jmix-docs`; find it by
     its tool `search-jmix-docs`. Pass the symbol or the call shape as
     `queryText`, `jmixVersion: "v3"`, and `maxResults: 5` (without a limit the
     response can be tens of kilobytes). It needs no API key.

   Neither server is filtered to the version the project pins (the Jmix docs
   MCP covers all of `v3`). If the docs disagree with the jars of the version
   the project resolves (check 3), the jars win. For an XML attribute, or a
   member that may be newer than the pinned version, confirm it in the
   resolved jars even when the docs show it.

   If the docs example is close to what you need but not the same, read
   **When a docs example is close but not the same** below. If neither server
   is connected, go to check 2.

2. **IDE symbol search, if available.** Confirm that the class or constant
   exists, and read its fully-qualified name and members to settle package
   guesses and whether a constant or inner type exists.

3. **`javap` on the dependency jars, the fallback that always works.** It
   prints the real members and signatures from the compiled classes. It is the
   only check left for a symbol with zero call sites in the project (check 4
   has nothing to grep).

   ```bash
   V=3.1.0   # the Jmix version the project pins
   CP=$(find ~/.gradle/caches -name "jmix-*-$V.jar" | tr '\n' ':')
   javap -p -cp "$CP" io.jmix.flowui.Dialogs
   ```

   This puts all Jmix jars of the pinned version on one classpath, so you need
   not know which module owns the class (`-p` also lists non-public members).
   For a Vaadin or other library class, use that library's pinned jar.

   **Read the version the project resolves, not the newest one cached.**
   Whenever you read a framework artifact (a jar, an XSD, a sources archive),
   resolve the pinned version first (`gradle.properties`, the BOM, or
   `./gradlew dependencies`) and read only that artifact. The cache often holds
   several versions of a module, and the newest is often an unreleased
   `*.999-SNAPSHOT` whose API and XSD are supersets of the released one. This
   matters most for **XML descriptor attributes**: an attribute that exists
   only in the snapshot XSD compiles fine and shows up as an IDE inspection
   error or a render-time failure, never at `compileJava`.

4. **Last resort: grep a known-good example and reuse only what is there.**
   Find a real call site in the wider codebase or a reference app and copy its
   exact shape. This helps when the symbol is used somewhere, but finds nothing
   when it is new to the project (use check 3 then).

If nothing confirms a symbol, do not type it. Pick one you can confirm, or
omit the optional decoration (e.g. drop an icon attribute rather than guess a
constant).

## Commonly invented symbols

Never type these without verifying first:

| You might type                                       | Reality                                                                                                                                                     |
|------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| an invented `VaadinIcon` constant                    | does not exist; the icon enum is small and irregular — pick from existing constants or omit                                                                 |
| `JmixButton.ClickEvent`                              | use `com.vaadin.flow.component.ClickEvent<JmixButton>`                                                                                                      |
| `DataGrid.ReadEvent`, `DataGrid.SelectionEvent`      | use `com.vaadin.flow.data.selection.SelectionEvent<DataGrid<E>, E>`                                                                                         |
| `Target.DATA_GRID`                                   | not a Jmix `@Subscribe` target — use `Target.COMPONENT` with explicit id                                                                                    |
| `io.jmix.flowui.dialogs.Dialogs`                     | actual: `io.jmix.flowui.Dialogs`                                                                                                                            |
| `io.jmix.core.entity.EntityStates`                   | actual: `io.jmix.core.EntityStates`                                                                                                                         |
| `io.jmix.flowui.component.datagrid.DataGrid`         | actual: `io.jmix.flowui.component.grid.DataGrid`                                                                                                            |
| `io.jmix.flowui.component.markdown.Markdown`         | actual: `com.vaadin.flow.component.markdown.Markdown` — the `<markdown>` component is Vaadin's; `io.jmix.flowui` has a DIFFERENT `markdowneditor` component |
| `dialogs.createDetailView(this, entity, View.class)` | use `dialogWindows.detail(this, EntityClass.class).editEntity(entity).withViewClass(View.class)`                                                            |
| `dataGrid.addItemChangeListener(...)`                | use `addSelectionListener(...)` or `asSingleSelect().addValueChangeListener(...)`                                                                           |
| `dataGrid.getSingleSelected()`                       | use `getSingleSelectedItem()`                                                                                                                               |
| an add-on entity's JPQL name derived from its class name (`audit_EntityLogItem`) | actual: `audit_EntityLog` — an add-on entity's name comes from `@Entity(name = ...)` and need not match the class name; read it from the jar (check 3)      |

## Forbidden

- Typing a symbol that no check confirmed.
- Typing any symbol from the table above without verifying it.
- Guessing a `VaadinIcon` constant instead of omitting the icon.
- Deciding that a docs or IDE tool is not available without first searching
  tools that are loaded on demand.
- Stopping at a failed Context7 call instead of switching to the Jmix docs MCP.
- Calling `search-jmix-docs` without `maxResults: 5`.
- Following the docs over the jars of the version the project resolves when
  the two disagree, or skipping the jars for an XML attribute or a member that
  may be newer than the pinned version.
- Querying again for a closer docs example when the difference is only plain
  Java or JPA.
- Grepping for an example (check 4) for a symbol new to the project instead of
  using `javap` (check 3).
- Reading the newest cached jar, XSD, or sources archive (often a
  `*.999-SNAPSHOT`) instead of the version the project resolves.
- Treating a compiling XML descriptor attribute as verified: an attribute from
  a snapshot XSD compiles and fails only in the IDE inspection or at render
  time.
- Deriving an add-on entity's JPQL name from its class name.

## Special cases

### When a docs example is close but not the same

If the example differs from what you need only in plain Java or JPA (an
abstract vs concrete base class, one field more or less, another property
type), it still confirms that the symbol exists. An import line in the example
confirms the class's package. Stop querying for a closer match.

It confirms only the symbol and its package. It does not show that the
example's annotations or pattern are complete; for those, follow the skill of
the artifact you write (for example `jmix-create-entity` for an entity).

Keep verifying only when the difference is the Jmix API itself: another method
name, another package, another annotation member. Then use `javap` (check 3).
