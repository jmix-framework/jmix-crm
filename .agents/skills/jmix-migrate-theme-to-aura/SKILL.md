---
name: jmix-migrate-theme-to-aura
description: Use to switch a project upgraded from Jmix 2.x from the Lumo theme to Aura, including Lumo utility classes. The Studio 2.x→3.x migration keeps Lumo, so this is a separate manual task.
disable-model-invocation: true
---

# Migrate a Jmix 2.x project's theme to Aura

Applies after Studio's platform upgrade to Jmix 3.x, when the project should use Aura instead
of Lumo.

## Step 0 — know what the upgrade did and did not do

Studio's automatic migration updates the BOM, the Gradle plugin and wrapper, dependencies, and
some configuration. For theming it changes three places:

```diff
  // <App>Application.java
+ @StyleSheet(Lumo.UTILITY_STYLESHEET)                  // + the Lumo import
  @Theme(value = "<app>")                               // left alone, still Lumo

+ com.vaadin.experimental.themeComponentStyles=true     # new vaadin-featureflags.properties

- {"parent": "jmix-lumo", "lumoImports":["typography","color",...]}   # theme.json,
+ {"parent": "jmix-lumo"}                                            # but see below
```

All of it keeps the project on Lumo. The feature flag is a Lumo compatibility shim that keeps
Lumo component styling working on Vaadin 25, and the utility stylesheet is Lumo's too; Step 2
removes both. Aura is offered only when creating a new project; nothing below happens
automatically.

Studio does not always remove `lumoImports`. The key is dead in Vaadin 25 and Step 3 deletes
the folder, so a remaining `lumoImports` does not mean the platform upgrade did not run.

Check the current state before changing anything:

```bash
grep -rn "@Theme\|@StyleSheet" src/main/java --include='*.java'
# The Lumo theme folder belongs at src/main/frontend/themes/<app>/. A project
# still on the deprecated root-level frontend/themes/ will show up here too:
find . -name theme.json -not -path '*/generated/*' -not -path '*/node_modules/*' \
       -not -path '*/build/*' -print -exec cat {} +
cat src/main/resources/vaadin-featureflags.properties 2>/dev/null
```

Note the theme folder path: Step 3 deletes it, and `@Theme(value = "<app>")` names it.

## Step 1 — take the scaffold from a real Aura project, do not hand-write it

**Ask the user for a reference Aura project**: the path to one they have, or a throwaway Jmix 3
project created in Studio with **Aura**. Do not search the filesystem for one. Use it only if
its `<app>.css` is still the untouched `/* Define your styles here */`; otherwise you inherit
someone's project CSS.

Copy its theme folder. The generated Aura CSS has app-layout insets and radii, surface
gradients, the user-menu grid and the initial layout; porting the Lumo files by hand gives none
of it.

```
src/main/resources/META-INF/resources/themes/<app>-aura/
├── styles.css                     # @import list only
├── <app>.css                      # "/* Define your styles here */"
└── view/
    ├── main-view.css
    ├── main-view-top-menu.css
    └── login-view.css
```

Rename `<source-app>.css` to `<app>.css` and fix its `@import` line in `styles.css`. A fresh
project also has an `<app>-lumo` sibling; keep it to switch back with a one-line change.

Aura themes live under `src/main/resources/META-INF/resources/themes/`, not under
`src/main/frontend/themes/` where the Lumo theme was.

## Step 2 — load Aura from the app shell, not from theme.json

`Aura.STYLESHEET` and `JmixAura.STYLESHEET` are the entry points. Never use
`"parent": "jmix-aura"` in `theme.json`: the parent chain is the deprecated Lumo-era mechanism,
and both classes are plain constant holders that do not implement `AbstractTheme`, so neither
can be a `@Theme(themeClass=…)` or a `theme.json` parent.

```java
@Push
@StyleSheet(Aura.STYLESHEET)                        // com.vaadin.flow.theme.aura.Aura
@StyleSheet(JmixAura.STYLESHEET)                    // io.jmix.flowui.theme.aura.JmixAura
@StyleSheet("themes/<app>-aura/styles.css")         // your own CSS, loaded last
@SpringBootApplication
public class MyApplication implements AppShellConfigurator {
```

The theme stylesheets come before your own. Delete the `@Theme` annotation and its import:
with the folder gone it resolves nothing, and it is deprecated in Vaadin 25.

Also delete `@StyleSheet(Lumo.UTILITY_STYLESHEET)` and the `Lumo` import: the upgrade added
them, and they load a Lumo stylesheet next to Aura. There is no Aura replacement. **If the
project uses any Lumo utility class, do Step 6 first**, or the layout silently collapses when
this line goes.

## Step 3 — delete the old theme wiring

```bash
git rm -r <theme folder from Step 0>   # normally src/main/frontend/themes/<app>
git rm src/main/resources/vaadin-featureflags.properties
```

If the feature flags file holds other flags, remove only the `themeComponentStyles` line
instead of deleting the file.

Removing the feature flag is required. `themeComponentStyles` injects Lumo component styles;
under Aura the result is a half-styled UI that is easy to miss:

- text fields lose their borders (underline-only)
- checkboxes render blank
- icons render as empty squares
- `window.Vaadin.featureFlags.themeComponentStyles` reads `true` in the console

Do not treat `--_lumo-vaadin-*-inject` rules in `document.adoptedStyleSheets` as the symptom;
see **Verify**.

## Step 4 — clean the frontend bundle (mandatory, and easy to skip)

Required in every mode: the generated frontend is stale in all of them, and only `vaadinClean`
clears it. It removes `src/main/bundles`, `node_modules`, `.vaadin` and usually the generated
frontend, but not always all of them, so check what is left:

```bash
./gradlew clean vaadinClean
# Run this BEFORE starting the app — a successful start legitimately recreates
# src/main/bundles and node_modules, so afterwards it reports phantoms.
for d in src/main/bundles node_modules .vaadin src/main/frontend/generated; do
  [ -e "$d" ] && echo "STILL PRESENT: $d  (delete it)"
done
```

These paths are gitignored, so deleting them by hand is safe. Deleting only
`src/main/frontend/generated` is not enough.

How to confirm the clean depends on the mode. In the default dev-bundle mode the stale file is
the Lumo-era `src/main/bundles/dev.bundle`: Vaadin reuses it, keeps serving Lumo component
styles, and logs `A development mode bundle build is not needed`. The next start must log
`... is needed`. With `vaadin.frontend.hotdeploy=true` there is no bundle and that line never
appears, and a production build rebuilds the bundle anyway; in both cases the log tells you
nothing, and only the browser checks in **Verify** count.

## Step 5 — port your own CSS to Aura tokens

Every `--lumo-*` name is undefined under Aura and fails silently. The two families do not match
name for name; translate by meaning:

| Lumo | Aura |
|---|---|
| `--lumo-font-size-*` | `--aura-font-size-xs…xl` (5 steps, no xxs/xxl/xxxl) |
| `--lumo-space-*` | `--vaadin-padding-*` (margins/padding), `--vaadin-gap-*` (gap) |
| `--lumo-body-text-color` | `--vaadin-text-color` |
| `--lumo-secondary-text-color` | `--vaadin-text-color-secondary` |
| `--lumo-header-text-color` | `--vaadin-text-color` (approximate: Aura has no separate header-text token) |
| `--lumo-primary-color` / `-text-color` / `-contrast-color` | `--aura-accent-color` / `--aura-accent-text-color` / `--aura-accent-contrast-color` |
| `--lumo-success-*` | `--aura-green`, `--aura-green-text` |
| `--lumo-error-*` | `--aura-red`, `--aura-red-text` |
| `--lumo-warning-*` | `--aura-yellow` / `--aura-orange` (+ `-text`) |
| `--lumo-contrast-*` (the whole alpha scale) | **no scale.** Pick by role: border → `--vaadin-border-color` / `-secondary`; fill → `--vaadin-background-container` / `-strong`; text → `--vaadin-text-color-secondary` / `-disabled` |
| `--lumo-shade-*` / `--lumo-tint-*` | **no scale.** Same rule — pick the `--vaadin-*` token for the role, or `--aura-surface-color` for a raised panel |
| `--lumo-size-*` (control height) | **no equivalent** |

Lumo's `-primary-` / `-success-` / `-error-` / `-warning-` families each have `-color` /
`-text-color` / `-contrast-color`; Aura has a fill plus a `-text` variant and no contrast
token, so write "text on a coloured fill" by hand. Where you used `--lumo-size-*` or a
percentage step, define your own token; do not invent an `--aura-` name. The table only
translates; for what Aura defines and the command to list it, see `jmix-style-ui`.

Global density comes from two root numbers, not three separate scales, so a Lumo compact
preset becomes an override of these two:

```css
html {
  --aura-base-font-size: 14;   /* default 14; drives the font-size scale      */
  --aura-base-size: 16;        /* default 16; drives the gap and padding scales */
}
```

Aura has no compact preset and no standard compact values: lower the two defaults and check in
a browser. Unlike Lumo, spacing cannot shrink separately from control sizing.

## Step 6 — port the Lumo utility classes

Aura has no utility-class layer and nothing to migrate the classes to. Lumo shipped a
stylesheet of `p-m`, `gap-s`, `flex`, `text-secondary`, `bg-contrast-5` …; `aura.css` defines
only `.aura-accent-*`, `.aura-surface*` and the `.v-error` / `.v-success` state classes. There
is no `Aura.UTILITY_STYLESHEET` and no renamed equivalent. Every utility class stops resolving
when Step 2 drops the stylesheet, and nothing reports it: the class stays in the DOM, matches
no rule, and padding, gaps and flex direction disappear.

So this is not a rename: each utility class in use becomes a project CSS rule or an equivalent
declaration on the component.

### Find every use — the `LumoUtility` symbol is not enough

The constants resolve to plain strings, so the class name is often written as a literal
(`addClassName("p-m")`, a `classNames` attribute, or a name built by concatenation). Search for
the rendered names too:

```bash
# 1. the symbol
grep -rn "LumoUtility" src/main/java --include='*.java'

# 2. the rendered names, wherever they are written as strings
UTIL='\b(p|m)[xytrbl]?-(auto|none|xs|s|m|l|xl|[0-9]+)\b|\bgap-[xy]?-?(xs|s|m|l|xl)\b'
UTIL="$UTIL"'|\b(flex|inline-flex|grid|block|inline-block|hidden)\b'
UTIL="$UTIL"'|\bflex-(row|col)(-reverse)?\b|\bitems-(start|center|end|baseline|stretch)\b'
UTIL="$UTIL"'|\bjustify-(start|center|end|between|around|evenly)\b'
UTIL="$UTIL"'|\btext-(xxs|xs|s|m|l|xl|xxl|xxxl|left|center|right)\b'
UTIL="$UTIL"'|\btext-(header|body|secondary|tertiary|disabled|primary|error|success)\b'
UTIL="$UTIL"'|\bfont-(light|normal|medium|semibold|bold)\b|\bbg-[a-z0-9-]+\b'
UTIL="$UTIL"'|\brounded-(s|m|l|full)\b|\bshadow-(xs|s|m|l|xl)\b|\b[wh]-(full|auto)\b'

grep -rnE "$UTIL" src/main/java src/main/resources \
     --include='*.java' --include='*.xml'
```

The second search is broad on purpose and over-matches: `flex`, `grid` and `hidden` are
ordinary words. Read the hits; do not act on the count. The goal is the set of distinct utility
names the project uses, usually a dozen or fewer.

### Replace them

Write those names into your own theme CSS with Aura tokens. Most map to one declaration:

| Lumo utility | What to write |
|---|---|
| `p-*`, `m-*`, `px-*`, `pt-*`, … | `padding` / `margin` from `--vaadin-padding-*` |
| `gap-*` | `gap: var(--vaadin-gap-*)` |
| `text-xs…text-xxxl` | `font-size: var(--aura-font-size-*)` (5 steps only — see Step 5) |
| `font-bold`, `font-medium` | `font-weight: var(--aura-font-weight-*)` |
| `text-secondary`, `text-body` | `color: var(--vaadin-text-color-secondary)` / `--vaadin-text-color` |
| `text-error`, `text-success` | `color: var(--aura-red-text)` / `var(--aura-green-text)` |
| `bg-contrast-*`, `bg-base` | `background: var(--vaadin-background-container*)` — no alpha scale, see Step 5 |
| `rounded-*` | `border-radius: var(--vaadin-radius-s/-m/-l)` — not `--aura-base-radius`, a unitless multiplier that computes to `0` as a length |
| `shadow-*` | `box-shadow: var(--aura-shadow-xs/-s/-m)` |
| `flex`, `flex-col`, `items-center`, `justify-between`, `w-full`, `hidden`, … | plain CSS — no token involved, write the property directly |

The layout classes are most of real usage and need no tokens, so for a project with many of
them the cheapest port is a small project-owned sheet that redefines only the names in use:

```css
/* src/main/resources/META-INF/resources/themes/<app>-aura/<app>.css */
.flex         { display: flex; }
.flex-col     { flex-direction: column; }
.items-center { align-items: center; }
.gap-m        { gap: var(--vaadin-gap-m); }
.p-m          { padding: var(--vaadin-padding-m); }
```

This keeps the existing `classNames` and Java call sites unchanged. Define only the names the
project uses: re-creating Lumo's full utility sheet brings back the problem the migration
should end. Where you edit the component anyway, prefer Flow's layout API
(`setFlexDirection`, `setAlignItems`, `setPadding`).

Check the result in the browser (**Verify**, check 4), not in the build.

## Step 7 — audit theme variants and view class names

**Variants are theme-specific and compile either way.** Jmix 3.0 removed
`always-float-label`, `contained`, `outlined`; support now depends on the active theme. Under
Aura, `contrast` is styled only as `[theme~='badge'][theme~='contrast']`, so
`themeNames="contrast"` on a button does nothing.

```bash
grep -rn "themeNames=" src/main/resources --include='*.xml'
grep -rn "LUMO_" src/main/java --include='*.java'
```

**The class names that the Aura CSS selects on must exist in your descriptors.** The generated
`main-view.css` styles the header only through the app-layout class:

```css
vaadin-app-layout.jmix-main-view-app-layout:not([primary-section='navbar']) .jmix-main-view-header { … }
```

A 2.x `main-view.xml` has a bare `<appLayout>`. Without the class every header rule silently
does nothing (no surface, border, radius or padding), and the drawer toggle floats on the page
background:

```xml
<appLayout classNames="jmix-main-view-app-layout">
```

Diff your `main-view.xml` against the fresh project's. These rules are for the drawer-based
MainView; with the horizontal menu, the CSS selects on the `jmix-main-view-top-menu-*` classes
in `main-view-top-menu.css` instead.

`<listMenu themeNames="toggle-reverse"/>` moves the menu toggles to the trailing edge. It is
only cosmetic; use it only if you want that look.

## Verify — in a browser, on computed values

Compile, static analysis and `test` all pass while the app still renders Lumo, so they
prove nothing here. Start the app and check:

```js
// 1. Aura tokens must resolve. Empty string = Aura CSS is not loaded at all.
getComputedStyle(document.documentElement).getPropertyValue('--aura-base-size')

// 2. The three stylesheets must be linked, in this order.
[...document.querySelectorAll('link[rel=stylesheet]')].map(l => l.getAttribute('href'))
// ./aura/aura.css , ./themes/jmix-aura/jmix-aura.css , ./themes/<app>-aura/styles.css

// 3. No Lumo STYLING left. Test for real Lumo rules, not for the substring
//    "_lumo-vaadin" — see the warning below.
window.Vaadin.featureFlags.themeComponentStyles          // must be false
document.adoptedStyleSheets
  .map(s => [...s.cssRules].map(r => r.cssText).join('')).join('')
  .match(/--lumo-[a-z-]+\s*:/)                           // must be null

// 4. Every utility class you ported in Step 6 must still resolve. A class that
//    matches no rule is invisible in the DOM — check the computed property.
getComputedStyle(document.querySelector('.p-m')).padding   // not '0px'
```

**The component test is the main check**: tokens can resolve while components still render
Lumo. Compare with the reference Aura project: a text field's `[part="input-field"]` must have
a real border width and Aura's radius, not `0px` and Lumo's smaller radius. Read both values
from the reference project; do not hard-code numbers, they are theme CSS and can change between
releases. If tokens resolve but components still look like Lumo, Step 4 was skipped.

**Never test `adoptedStyleSheets` for `_lumo-vaadin`.** The substring is present on a
correctly migrated Aura app, even with the flag off. It is Vaadin's per-page
component-detection marker, a single `:root::before` rule whose `transition` lists
`--_lumo-vaadin-*-inject` properties; it declares no `--lumo-*` properties, and its token list
changes as you navigate, so do not recognise it by length either.

### The login view is not enough

Everything after login needs an authenticated session, and an agent must not type passwords
into the form. The Step 7 `jmix-main-view-app-layout` class, the edit most likely to fail
silently, can be verified only on MainView.

Run checks 1-3 and the component test on the login view yourself: they cover token resolution,
stylesheet order and component borders. Then ask the user to log in, and check the header:

```js
const h = getComputedStyle(document.querySelector('.jmix-main-view-header'));
[h.borderBottomWidth, h.borderStartStartRadius]   // real values, not ['0px','0px']
document.querySelector('vaadin-app-layout').classList.contains('jmix-main-view-app-layout')
```

`0px` / `0px` means the class is missing and every header rule does nothing. While logged in,
also go through the list views and check for an error overlay, a raw `msg://` caption, and a
clean server log; re-run check 3 there, since its token list differs per page.

## Forbidden

- Reading a remaining `lumoImports` as proof that the platform upgrade did not run.
- Searching the filesystem for a reference Aura project instead of asking the user.
- Copying a reference project whose `<app>.css` is no longer the untouched default.
- Hand-writing the Aura view CSS instead of copying the generated scaffold.
- Putting the Aura theme under `src/main/frontend/themes/`.
- `"parent": "jmix-aura"` in `theme.json`, or `Aura` / `JmixAura` as a `@Theme(themeClass=…)`.
- Loading your own stylesheet before `Aura.STYLESHEET` and `JmixAura.STYLESHEET`.
- Keeping `@Theme` together with the Aura `@StyleSheet` declarations.
- Keeping `@StyleSheet(Lumo.UTILITY_STYLESHEET)` after the switch, or looking for an
  `Aura.UTILITY_STYLESHEET` to replace it with; there is none.
- Dropping the Lumo utility stylesheet before porting the classes the project uses (Step 6).
- Searching only for `LumoUtility`; the class names are often written as string literals.
- Acting on the hit count of the broad utility-name search instead of reading the hits.
- Re-creating Lumo's full utility sheet instead of only the names in use.
- Leaving the old Lumo theme folder in place.
- Leaving `com.vaadin.experimental.themeComponentStyles=true` enabled.
- Deleting `vaadin-featureflags.properties` when it holds other flags.
- Starting the app without `./gradlew clean vaadinClean` and concluding the theme "did not
  change anything".
- Deleting only `src/main/frontend/generated` instead of running `vaadinClean`.
- Checking for leftover bundle folders after the app has started.
- Trusting `A development mode bundle build is not needed` after the switch.
- Leaving `--lumo-*` variables in the project CSS; they are undefined under Aura.
- Translating a `--lumo-` name to an `--aura-` prefix and assuming it exists.
- `--aura-base-radius` as a `border-radius` length; it is a unitless multiplier.
- Relying on `contrast` on a button, or on the removed `always-float-label`, `contained`,
  `outlined` variants.
- Leaving `LUMO_` constants in Java.
- Leaving a bare `<appLayout>` without `classNames="jmix-main-view-app-layout"`.
- Calling the migration done on a green compile or `test`; neither renders a page.
- Hard-coding border and radius numbers in the component test instead of reading them from the
  reference project.
- Treating `_lumo-vaadin` in `adoptedStyleSheets` as proof the migration failed, or trying to
  recognise that rule by length.
- Running check 3 only on the login view instead of on each page after login.
- Typing passwords into the login form.
- Calling the migration verified from the login view alone; the MainView header rules are
  untested until someone logs in.
