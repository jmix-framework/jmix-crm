---
name: jmix-style-ui
description: Style Jmix Flow UI components in Jmix 2 / Vaadin 24 — views, fragments, grid renderers, or components built in a controller. Read this before writing getStyle().set(...), classNames, themeNames, addThemeVariants(...), or CSS custom properties (--lumo-*, occasionally --vaadin-*). A token or theme name the active theme does not define fails silently — code compiles, tests pass, rendering is wrong.
---

# Style Flow UI components

Applies to any component you style: view and fragment layouts, grid renderers,
badges, cards, and components built in a controller.

## Step 0 — identify the ACTIVE theme before typing a token or variant

Jmix 2 Flow UI is Lumo-based, but the active theme is still a concrete set of
stylesheets: Vaadin Lumo + Jmix Lumo + the project's own theme. A token or
`themeNames` value only works when one of those layers actually styles it.

Find the project theme name first:

```bash
# the application class / app shell usually declares the project theme name
grep -rn "@Theme" src/main/java --include='*.java'
```

In a normal Jmix 2 app this looks like:

```java
@Theme(value = "demo-theme")
```

That means the project's own theme usually lives under:

- `src/main/frontend/themes/<theme-name>/`

Treat that folder as the first place to look for application styling. Only then
remember that Vaadin Lumo and Jmix Lumo also contribute styles underneath it.

Jmix 2 ships the base Jmix theme as **`jmix-lumo`**, whose `theme.json`
imports Lumo's `typography`, `color`, `spacing`, `badge`, and `utility`
modules; the project theme plugs into that layer through
`"parent": "jmix-lumo"` in its own `theme.json`. That makes **`--lumo-*` the
primary token family** for application styling in Jmix 2.x. Jmix may add its own layer on top (for example
`--jmix-lumo-*` custom properties), and Vaadin components may expose some
lower-level `--vaadin-*` customization hooks.

All shell snippets below are **Unix-like / bash examples**, not guaranteed
cross-platform commands. On Windows, adapt them to PowerShell or use IDE/global
search against the same target files and folders.

## Where the styling belongs — in this order

**1. A CSS rule in the project theme + `classNames` — preferred.** Reusable,
inspectable, and it keeps look-and-feel values out of Java:

```css
/* src/main/frontend/themes/<app-theme>/<view>.css */
.order-card {
    border: 1px solid var(--lumo-contrast-20pct);
    border-radius: var(--lumo-border-radius-m);
}
```

```xml
<vbox id="orderBox" classNames="order-card"/>
```

```java
card.addClassName("order-card");   // when the component is built in the controller
```

A new CSS file is not loaded automatically — wire it into the theme's
`styles.css` or theme import chain. A file nothing imports fails exactly like an
undefined token: no error anywhere, the rules just never apply.

**2. A built-in component theme variant / `themeNames` / utility class — for
looks the framework already provides.** Do not re-implement badges, sizes, or
known Lumo/Jmix utility styles with hand-written CSS:

```java
button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
avatar.addThemeVariants(AvatarVariant.LUMO_LARGE);
badge.getElement().getThemeList().addAll(List.of("badge", "pill"));
layout.addClassName(LumoUtility.Gap.MEDIUM);
layout.addClassName(JmixLumoUtility.Container.BUTTONS_PANEL);
```

```xml
<span id="statusBadge" themeNames="badge primary pill"/>
```

`ThemeList` holds single tokens — `add("badge pill")` happens to render (the
attribute joins entries with spaces) but stores one bogus entry, so a later
`remove("pill")` or `contains("badge")` silently does nothing. When you need
several names, add them separately or use `addAll(List.of("badge", "pill"))`.

**A theme name is component-specific, not just globally "supported".** In Jmix
2.x, names such as `badge`, `pill`, `primary`, `small`, and
`tertiary-inline` are real, but that does NOT mean every component reacts to
every one of them. A name can exist in XSD / Studio metadata and still do
nothing on the concrete component you put it on. Read the selector, not just the
name.

For example, `themeNames="badge primary small"` is valid for HTML badge-like
containers, and `ButtonVariant.LUMO_TERTIARY_INLINE` is a real Lumo button
variant used by Jmix itself. But a badge-specific theme name on a plain button,
or a component-specific theme name on the wrong host element, still compiles and
silently renders as the default component.

**3. Inline `getStyle().set(...)` — last resort.** Correct for a one-off dynamic
value (a color computed from data, a width from a measurement), not for a look
that repeats across components. The same applies to the declarative XML
`css="..."` attribute — fine for a one-off, poor for reusable styling:

```java
statusLabel.getStyle().set("color", "var(--lumo-primary-text-color)");
```

```xml
<span id="statusLabel" css="color: var(--lumo-primary-text-color)"/>
```

## Tokens that exist — enumerate them, do not invent them

Verify a token the way you verify an API symbol: it must appear in the active
theme's styles. In a Jmix 2 project do NOT expect `node_modules` — the frontend
is served from a pre-compiled bundle and npm usually never runs, even after a
full build. The bundle itself is the source of truth, and it contains every
layer at once (Vaadin base styles, Lumo modules, Jmix, add-ons, the project
theme):

```bash
# after the first build/run the compiled bundle is on disk
grep -rhoE '\-\-lumo-[a-z0-9-]+' build/dev-bundle/ | sort -u

# some lower-level Vaadin customization hooks live there too
grep -rhoE '\-\-vaadin-[a-z0-9-]+' build/dev-bundle/ | sort -u

# no build/ yet? the same bundle ships with the project as a plain ZIP
DIR=$(mktemp -d)
unzip -oq src/main/bundles/dev.bundle -d "$DIR"
grep -rhoE '\-\-lumo-[a-z0-9-]+' "$DIR" | sort -u

# the Jmix Lumo layer is also extracted into the project by the frontend build
grep -rhoE '\-\-jmix-lumo-[a-z0-9-]+' \
  src/main/frontend/generated/jar-resources/themes/jmix-lumo/ | sort -u
```

If neither `build/dev-bundle/` nor `src/main/bundles/dev.bundle` exists (a
freshly created project that has never been built), build or run the app once —
you need a running app anyway to verify the result (see below). Do not dig the
Gradle cache for Lumo styles instead: in Vaadin 24 no jar carries the Lumo
stylesheets — `vaadin-lumo-theme-<version>.jar` exists but holds only the
`LumoUtility` Java classes, so unzipping it yields no tokens and no error. The
only theme jar worth opening is `jmix-flowui-themes-<jmix version>.jar`
(`META-INF/resources/themes/jmix-lumo`), and only for the `--jmix-lumo-*`
layer. When `node_modules` does exist (the bundle had to be rebuilt),
`grep -rhoE '\-\-lumo-[a-z0-9-]+' node_modules/@vaadin/vaadin-lumo-styles/`
works as well.

The tokens you reach for most in Jmix 2.x are usually the Lumo ones:

| Purpose | Typical token |
|---|---|
| accent / link text | `--lumo-primary-text-color` |
| accent fill | `--lumo-primary-color` |
| text ON accent fill | `--lumo-primary-contrast-color` |
| body text | `--lumo-body-text-color` |
| muted / secondary text | `--lumo-secondary-text-color` |
| disabled text | `--lumo-disabled-text-color` |
| border / contrast surfaces | `--lumo-contrast-10pct`, `--lumo-contrast-20pct` |
| background | `--lumo-base-color`, `--lumo-tint-5pct` |
| size | `--lumo-size-xs` … `--lumo-size-xl` |
| corner radius | `--lumo-border-radius-s`, `--lumo-border-radius-m`, `--lumo-border-radius-l` |
| spacing | `--lumo-space-xs` … `--lumo-space-xl` |
| tint / shade scale | `--lumo-tint-*`, `--lumo-shade-*` |
| font size | `--lumo-font-size-xs` … `--lumo-font-size-xxxl` |
| line height | `--lumo-line-height-xs` … `--lumo-line-height-m` |
| shadow | `--lumo-box-shadow-s`, `--lumo-box-shadow-m`, `--lumo-box-shadow-xl` |
| status colors | `--lumo-success-*`, `--lumo-warning-*`, `--lumo-error-*` |

**The same theme may also expose some `--vaadin-*` tokens.** In Jmix 2.x these
are usually a secondary, lower-level layer for component tuning, not the first
place to look for ordinary app styling. Use them when you specifically need a
Vaadin component hook rather than a normal Lumo theme token. Being
theme-neutral does not mean "invent any `--vaadin-*` token you like" — verify
that the active theme actually defines it.

Jmix Lumo may add its own small layer of custom properties such as
`--jmix-lumo-warning-background-color`. Treat these exactly like any other
token: if the stylesheet does not define it, do not use it.

## Theme names and variants that exist — enumerate them, do not guess them

`themeNames` and `*Variant` constants are just strings. They compile even when
the active component/theme combination does nothing with them.

Enumerate the names from the same compiled bundle — one grep covers every
layer (Vaadin component styles, Lumo's badge module, Jmix, add-ons):

```bash
grep -rhoE "theme~=['\"]?[a-z0-9-]+" build/dev-bundle/ \
  | sed -E "s/theme~=['\"]?//" | sort -u
# or run it over the extracted src/main/bundles/dev.bundle from the previous section
```

The XML-side `themeNames` values that XSD / Studio metadata suggest are in the
resolved `jmix-flowui` jar:

```bash
FLOWUI=$(find ~/.gradle/caches -name 'jmix-flowui-2.*.jar' ! -name '*-sources.jar' | tail -1)
unzip -p "$FLOWUI" 'io/jmix/flowui/view/layout.xsd' \
  | grep -n "badgeThemeNames\|tertiary-inline"
```

What this tells you in Jmix 2 — and why only the bundle grep is conclusive:

- `badge` / `pill` are real, but they are styled by Lumo's **badge module**
  (enabled through the theme's `lumoImports`), not by the `jmix-lumo` theme
  folder — a grep of the Jmix layer alone reports them as unstyled.
- `tertiary-inline` is real, but it lives in the Vaadin button component's own
  Lumo styles — again visible in the bundle, absent from the theme folders.
- A name being present in XSD metadata does NOT prove it applies to your
  component.
- The project's own theme (`src/main/frontend/themes/<theme-name>/`) can add
  more selectors, so grep that folder too. When you need to see which names
  come from Jmix itself, the extracted Jmix layer is at
  `src/main/frontend/generated/jar-resources/themes/jmix-lumo/`.

## When the active theme has no suitable token or variant

Four acceptable options, in this order:

1. **A verified `--lumo-*` token** from the active theme.
2. **A verified `--vaadin-*` token** when you specifically need a lower-level
   Vaadin component hook and the active theme defines it.
3. **A project-defined CSS class or custom property** in the theme stylesheet.
4. **A literal, theme-neutral value** for a genuinely neutral decoration:

```java
card.getStyle()
        .set("border", "1px solid rgba(128, 128, 128, 0.3)")
        .set("border-radius", "6px");
```

When you rely on a token for something essential (a link's affordance, a status
color), add a theme-independent fallback too, e.g. `text-decoration: underline`
next to the accent color.

## Why a wrong token or theme name is invisible to every gate

An undefined CSS custom property is not an error anywhere in the stack. The
declaration parses, the build succeeds, and the browser applies the fallback:

| Property | Falls back to |
|---|---|
| `color` | the inherited color (often near-black) |
| `background-color` | transparent |
| `border-color` | `currentColor` |
| `border-radius` | `0` |
| any length | the property's initial value |

The same is true for a dead `themeNames` / variant combination: the component
just renders as though you had not applied that style at all. So the defect
survives `compileJava`, the Jmix inspection, and a green `clean test`. The
rendering is merely wrong — no exception, no warning, nothing in the log.

**CSS tokens do not resolve automatically in drawing APIs.** Some chart,
diagram, or other components paint to a `<canvas>` and pass colour options
directly to a JavaScript drawing API. Such APIs do not interpret `var(...)` as
CSS. Before passing a theme token to a component option, check whether the
component resolves CSS custom properties itself. If it does not, use the
component's palette/theme API, a literal colour, or resolve the custom
property first (for example with
`getComputedStyle(...).getPropertyValue(...)`) and pass the resulting colour
value.

## Verify — the COMPUTED style, in a browser

The declaration looks identical whether the property resolves or not, so reading
the Java or the CSS proves nothing. Open the view with a browser tool (Gate 3 in
`jmix-verify-bootrun`) and read the computed value:

```js
getComputedStyle(document.querySelector('#statusLabel')).color
```

A resolved token gives a real color (`rgb(...)`); an undefined one gives the
inherited value — that difference is the check. Do the same for
`borderRadius`/`borderColor` when you set them. If no browser tool is available,
say `styling not browser-verified` rather than calling it done.

This check applies only when the value becomes a CSS declaration. If a
component passes the value directly to a drawing API, there may be no
corresponding computed style to inspect; verify the rendered result and, when
necessary, inspect the component's frontend implementation to confirm whether it
resolves CSS custom properties.

The same check catches a dead theme variant, because a variant the component or
theme does not style computes exactly like the component with no variant at all:

```js
getComputedStyle(document.querySelector('#orderButton')).backgroundColor
```

Under Lumo a working `tertiary-inline` button computes differently from a plain
default button. Compare against the same component type without the variant — if
the two match, the variant did nothing.

## Forbidden

- A guessed token name — confirm it in the compiled bundle or the active
  theme's styles first.
- A guessed `themeNames` or variant string — confirm it in the compiled bundle
  (or XSD / constants / the active theme CSS) first.
- Assuming a valid theme name works on every component without checking the
  selector that styles it.
- Passing `var(...)` to a component option consumed directly by a drawing API
  unless the component explicitly resolves CSS custom properties before drawing.
- Inline `getStyle().set(...)` for a look that a component theme variant,
  utility class, or reusable CSS class already provides.
- A repeated inline style across several components instead of one CSS class in
  the project theme.
- Claiming styling works from a green compile / `clean test` — neither renders
  anything.
- Editing generated frontend files — they are regenerated on every build.
