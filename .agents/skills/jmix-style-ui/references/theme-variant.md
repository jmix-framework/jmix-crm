# Theme names and variants

Part of `jmix-style-ui`. Read this when you add a theme name or a theme variant.

`themeNames` and `*Variant` constants are just strings. They compile even when
the active component/theme combination does nothing with them.

Enumerate the names from the compiled bundle (see **Tokens that exist** in
`SKILL.md`). One grep covers every layer: Vaadin component styles, Lumo's badge
module, Jmix, add-ons:

```bash
grep -rhoE "theme~=['\"]?[a-z0-9-]+" build/dev-bundle/ \
  | sed -E "s/theme~=['\"]?//" | sort -u
# or run it over the extracted src/main/bundles/dev.bundle
```

The XML-side `themeNames` values that XSD / Studio metadata suggest are in the
resolved `jmix-flowui` jar:

```bash
FLOWUI=$(find ~/.gradle/caches -name 'jmix-flowui-2.*.jar' ! -name '*-sources.jar' | tail -1)
unzip -p "$FLOWUI" 'io/jmix/flowui/view/layout.xsd' \
  | grep -n "badgeThemeNames\|tertiary-inline"
```

What this tells you in Jmix 2, and why only the bundle grep is conclusive:

- `badge` / `pill` are real, but they are styled by Lumo's **badge module**
  (enabled through the theme's `lumoImports`), not by the `jmix-lumo` theme
  folder. A grep of the Jmix layer alone reports them as unstyled.
- `tertiary-inline` is real, but it lives in the Vaadin button component's own
  Lumo styles. It is visible in the bundle and absent from the theme folders.
- A name being present in XSD metadata does NOT prove it applies to your
  component.
- The project's own theme (`src/main/frontend/themes/<theme-name>/`) can add
  more selectors, so grep that folder too. To see which names come from Jmix
  itself, the extracted Jmix layer is at
  `src/main/frontend/generated/jar-resources/themes/jmix-lumo/`.

Read the selector, not just the name: a hit does not mean the variant works on
your component. `themeNames="badge primary small"` is valid for HTML badge-like
containers, and `ButtonVariant.LUMO_TERTIARY_INLINE` is a real Lumo button
variant used by Jmix itself. But a badge-specific theme name on a plain button,
or a component-specific theme name on the wrong host element, still compiles and
renders as the default component.
