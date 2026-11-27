---
name: jmix-style-ui
description: Use before styling Jmix Flow UI components with getStyle().set(...), class names, theme variants, or CSS custom properties (--aura-*, --lumo-*, --vaadin-*) in views, fragments, or renderers.
---

# Style Flow UI components

Applies to any component you style: view and fragment layouts, grid renderers,
badges, cards, and components built in a controller.

## Step 0 — identify the active theme before typing a token

Each theme defines its own set of tokens (CSS custom properties). A token from
the wrong theme is undefined, and the browser silently falls back. So find the
theme first:

```bash
# the main application class declares the theme stylesheets
grep -rn "@StyleSheet\|@Theme" src/main/java --include='*.java'
```

- `@StyleSheet(Aura.STYLESHEET)` + `@StyleSheet(JmixAura.STYLESHEET)` → **Aura**,
  tokens are `--aura-*` (Jmix 3's newer default).
- A Lumo theme declaration (or neither of the above) → **Lumo**, tokens are
  `--lumo-*`.

The project's own CSS usually sits next to those declarations, e.g.
`@StyleSheet("themes/<app>-aura/styles.css")` →
`src/main/resources/META-INF/resources/themes/<app>-aura/styles.css`, or a
`src/main/frontend/themes/<name>/` folder. Your own rules go there.

Never mix the families: `--lumo-primary-text-color` in an Aura app resolves to
nothing.

## Where the styling belongs — in this order

**1. A CSS rule in the project theme + `classNames` — preferred.** Reusable,
inspectable, and it keeps values out of Java:

```css
/* src/main/resources/META-INF/resources/themes/<app>/<view>.css */
.order-card {
    border: 1px solid var(--aura-accent-border-color);
    border-radius: var(--vaadin-radius-m);
}
```

```xml
<vbox id="orderBox" classNames="order-card"/>
```

```java
card.addClassName("order-card");   // when the component is built in the controller
```

A new CSS file is not loaded on its own: add `@import url('<view>.css');` to
the theme's `styles.css` (the stylesheet the application declares). Otherwise
its rules never apply, with no error anywhere.

**2. A built-in component theme variant — for looks the component already has.**
Do not re-implement a badge or a size variant with hand-written CSS:

```java
avatar.addThemeVariants(AvatarVariant.LARGE);
badge.getElement().getThemeList().addAll(List.of("badge", "pill"));   // badge theme: Vaadin's under Lumo, Jmix's under Aura
```

`ThemeList` holds single tokens. `add("badge pill")` renders (the attribute
joins entries with spaces) but stores one wrong entry, so a later
`remove("pill")` or `contains("badge")` silently does nothing.

A variant name is theme-specific, like a token: a `*Variant` constant is only a
string the theme has to style. `ButtonVariant.LUMO_TERTIARY_INLINE` sets
`theme="tertiary-inline"`, which Aura does not style, so the button keeps its
default background, border, and shadow. One enum holds `LUMO_*`, `AURA_*`, and
theme-neutral constants (`PRIMARY`, `TERTIARY`, `SUCCESS`, `WARNING`, `ERROR`,
`SMALL`, `LARGE`), and they all compile the same. Prefer the theme-neutral
constant, and confirm that the active theme styles the name it maps to: first read
`references/theme-variant.md` in this skill's folder.

**3. Inline `getStyle().set(...)` — last resort.** Correct for a one-off dynamic
value (a color computed from data, a width from a measurement), not for a look
that repeats across components:

```java
statusLabel.getStyle().set("color", "var(--aura-accent-text-color)");
```

## Tokens that exist — enumerate them, do not invent them

Verify a token like an API symbol: it must appear in the active theme's
stylesheet. The full list is one command away, so do not guess:

```bash
# after a build, the theme is in node_modules
grep -rhoE '\-\-aura-[a-z0-9-]+' node_modules/@vaadin/aura/ | sort -u
grep -rhoE '\-\-lumo-[a-z0-9-]+' node_modules/@vaadin/vaadin-lumo-styles/ | sort -u

# no node_modules? read it from the resolved jar
JAR=$(find ~/.gradle/caches -name 'vaadin-aura-theme-*.jar' | grep -v sources | tail -1)
unzip -p "$JAR" 'META-INF/resources/aura/aura.css' | grep -ohE '\-\-aura-[a-z0-9-]+' | sort -u

# the same stylesheet also defines the shared --vaadin-* layer — enumerate it too
unzip -p "$JAR" 'META-INF/resources/aura/aura.css' | grep -ohE '\-\-vaadin-[a-z0-9-]+' | sort -u
```

Aura's set is small, under a hundred names. The ones you reach for most:

| Purpose | Aura token |
|---|---|
| accent / link text | `--aura-accent-text-color` |
| accent fill | `--aura-accent-color` |
| text ON an accent fill | `--aura-accent-contrast-color` |
| border | `--aura-accent-border-color` |
| panel / card background | `--aura-surface-color`, `--aura-surface-color-solid` |
| page background | `--aura-background-color`, `--aura-app-background` |
| corner radius | `--vaadin-radius-s` / `-m` / `-l`. Aura derives them from `--aura-base-radius`, which is a unitless multiplier (`3`), not a length — `border-radius: var(--aura-base-radius)` is invalid and computes to `0` |
| font size | `--aura-font-size-xs` … `-xl` |
| font weight | `--aura-font-weight-regular` / `-medium` / `-semibold` |
| line height | `--aura-line-height-xs` … `-xl` |
| shadow | `--aura-shadow-xs` / `-s` / `-m` |
| status colors | `--aura-red`, `--aura-green`, `--aura-yellow`, `--aura-orange`, `--aura-blue`, `--aura-purple` (each with a `-text` variant, e.g. `--aura-red-text`), `--aura-neutral` (no `-text` variant) |

**The same stylesheet defines a second, bigger family: `--vaadin-*`**, the
semantic tokens of Vaadin's base styles (~230 names in Aura 25.2, ~280 in Lumo
25.2). Look there first for common needs: Aura has no secondary-text token of
its own, but defines `--vaadin-text-color-secondary`. A `--vaadin-*` name means
the same under any theme, but each theme defines only the subset it restyles
(82 names are common to Aura and Lumo in 25.2), so verify it in the active
theme's stylesheet too (e.g. `--vaadin-gap-*` / `--vaadin-radius-*` exist in
Aura but not in Lumo). Both themes define this core in 25.2:

| Purpose | `--vaadin-*` token (in both Aura and Lumo) |
|---|---|
| body text | `--vaadin-text-color` |
| muted / secondary text | `--vaadin-text-color-secondary` |
| disabled text | `--vaadin-text-color-disabled` |
| plain border | `--vaadin-border-color`, `--vaadin-border-color-secondary` |
| background | `--vaadin-background-color`, `--vaadin-background-container`, `--vaadin-background-container-strong` |
| focus ring | `--vaadin-focus-ring-color`, `--vaadin-focus-ring-width` |
| per-component knobs | `--vaadin-button-padding`, `--vaadin-card-padding`, … (set ON the component to retune it) |

**The two themes are not name-for-name equivalents**, so do not translate a
Lumo token into an `--aura-` prefix. Lumo has `--lumo-body-text-color`,
`--lumo-secondary-text-color`, and a contrast scale (`--lumo-contrast-20pct` …);
Aura has none of those. In Aura, body text is
`--vaadin-text-color`, muted text is `--vaadin-text-color-secondary`, and a
plain border is `--vaadin-border-color` — the `--vaadin-*` layer, not the
`--aura-*` family. The other way, Lumo has no `--lumo-accent-surface` or
`--lumo-surface-level`.

If the active theme has no suitable token, first read `references/no-suitable-token.md` in this skill's folder.

When you rely on a token for something essential (a link's affordance, a status
color), add a theme-independent fallback too, e.g. `text-decoration: underline`
next to the accent color.

If a component paints to a `<canvas>` or passes color options directly to a
JavaScript drawing API (some charts and diagrams), first read `references/drawing-api-colors.md` in this skill's folder.

## Why a wrong token is invisible to every gate

An undefined CSS custom property is not an error anywhere in the stack. The
declaration parses, the build succeeds, and the browser applies the fallback:

| Property | Falls back to |
|---|---|
| `color` | the inherited color (often near-black) |
| `background-color` | transparent |
| `border-color` | `currentColor` |
| `border-radius` | `0` |
| any length | the property's initial value |

So the defect survives `compileJava`, the Jmix inspection, and a green
`test`, with no exception, no warning, and nothing in the log.

## Verify — the computed style, in a browser

The declaration looks the same whether the property resolves or not, so reading
the Java or the CSS proves nothing. Open the view with a browser tool (Gate 3 in
`jmix-verify-bootrun`) and read the computed value:

```js
getComputedStyle(document.querySelector('#statusLabel')).color
```

A resolved token gives a real color (`rgb(...)`); an undefined one gives the
inherited value — that difference is the check. Do the same for
`borderRadius`/`borderColor` when you set them. If no browser tool is available,
say `styling not browser-verified` rather than calling it done. For a value
passed to a drawing API, first read `references/drawing-api-colors.md` in this skill's folder.

The same check catches a dead theme variant, because a variant the theme does not
style computes exactly like the component with no variant at all:

```js
getComputedStyle(document.querySelector('#orderButton')).backgroundColor
```

Under Aura a working `tertiary` gives `rgba(0, 0, 0, 0)`; a name the theme never
styles gives the default button fill. Compare against a plain component of the
same type — if the two match, the variant did nothing.

## Forbidden

- `--lumo-*` tokens in an Aura app, or `--aura-*` in a Lumo app, even when the
  active theme has no suitable token; or a Lumo name with an `--aura-` prefix
  (or the reverse) assumed to exist.
- A `LUMO_*` theme variant in an Aura app (or `AURA_*` in a Lumo app) whose
  variant string the active theme does not style — it compiles and does nothing.
- A theme-neutral constant (e.g. `TERTIARY`) used without confirming that the
  active theme styles the name it maps to.
- A guessed token name — confirm it in the active theme's stylesheet first.
- An essential look (a link's affordance, a status color) that depends only on a
  token, with no theme-independent fallback.
- `border-radius: var(--aura-base-radius)` — it is a unitless multiplier and
  computes to `0`.
- A new CSS file that no stylesheet `@import`s — its rules never apply.
- `ThemeList.add("badge pill")` — add one token per entry.
- Re-implementing a badge or size variant with hand-written CSS.
- Passing `var(...)` to a component option consumed directly by a drawing API
  unless the component explicitly resolves CSS custom properties before drawing.
- Inline `getStyle().set(...)` for a look that a component theme variant or a
  reusable CSS class already provides.
- A repeated inline style across several components instead of one CSS class in
  the project theme.
- Claiming styling works from a green compile / `test` — neither renders
  anything. Without a browser check, report `styling not browser-verified`.
- Treating a reading of the Java or the CSS as proof of the look — only the
  computed style in a browser shows it.
- Editing generated frontend files — they are regenerated on every build.
