---
name: jmix-migrate-theme-to-aura
description: Switch a project that came from Jmix 2.x over to the Aura theme, including porting Lumo utility classes. Studio's 2.x→3.x migration deliberately keeps Lumo and there is no automated conversion, so every step here is manual. Each step has a SILENT failure mode — the app boots, compiles, and passes a green clean test while still rendering Lumo, or renders Aura tokens on top of Lumo components.
disable-model-invocation: true
---

# Migrate a Jmix 2.x project's theme to Aura

Applies after Studio's platform upgrade to Jmix 3.x, when the project should use
Aura instead of Lumo.

## Step 0 — know what the upgrade did and did NOT do

Studio's automatic migration updates the BOM, the Gradle plugin and wrapper,
dependencies, and some configuration. On theming it touches three places:

```diff
  // <App>Application.java
+ @StyleSheet(Lumo.UTILITY_STYLESHEET)                  // + the Lumo import
  @Theme(value = "<app>")                               // left alone, still Lumo

+ com.vaadin.experimental.themeComponentStyles=true     # new vaadin-featureflags.properties

- {"parent": "jmix-lumo", "lumoImports":["typography","color",...]}   # theme.json,
+ {"parent": "jmix-lumo"}                                            # but see below
```

All of it keeps the project **on Lumo**. The feature flag is a **Lumo
compatibility shim** that keeps Lumo component styling working on Vaadin 25, and
the utility stylesheet is Lumo's too — Step 2 removes both. Aura is offered only
when *creating* a new project; nothing below happens automatically.

**Studio does not always strip `lumoImports`.** The key is dead in Vaadin 25
either way and Step 3 deletes the folder, so a surviving `lumoImports` is not
evidence that the platform upgrade did not run.

Confirm where you actually are before changing anything:

```bash
grep -rn "@Theme\|@StyleSheet" src/main/java --include='*.java'
# The Lumo theme folder belongs at src/main/frontend/themes/<app>/. A project
# still on the deprecated root-level frontend/themes/ will show up here too:
find . -name theme.json -not -path '*/generated/*' -not -path '*/node_modules/*' \
       -not -path '*/build/*' -print -exec cat {} +
cat src/main/resources/vaadin-featureflags.properties 2>/dev/null
```

Keep that folder path — Step 3 deletes it and `@Theme(value = "<app>")` names it.

## Step 1 — take the scaffold from a real Aura project, do not hand-write it

**Ask the user for a reference Aura project** — the path to one they already
have, or a throwaway Jmix 3 project created in Studio choosing **Aura**. Do not
search the filesystem for one. Use it only if its `<app>.css` is still the
untouched `/* Define your styles here */`, or you inherit someone's project CSS.

Copy its theme folder. The generated Aura CSS is much richer than a translated
Lumo file — app-layout insets and radii, surface gradients, the user-menu grid,
the initial layout — and hand-porting the Lumo boilerplate reproduces none of it.

```
src/main/resources/META-INF/resources/themes/<app>-aura/
├── styles.css                     # @import list only
├── <app>.css                      # "/* Define your styles here */"
└── view/
    ├── main-view.css
    ├── main-view-top-menu.css
    └── login-view.css
```

Copy the folder, rename `<source-app>.css` to `<app>.css`, and fix that one
`@import` line in `styles.css`. A fresh project also ships an `<app>-lumo`
sibling; keeping it makes switching back a one-line change.

**Note the location.** Aura themes live under
`src/main/resources/META-INF/resources/themes/`, not under the
`src/main/frontend/themes/` tree the Lumo theme used.

## Step 2 — load Aura from the app shell, not from theme.json

`Aura.STYLESHEET` and `JmixAura.STYLESHEET` are the entry points. **Never
`"parent": "jmix-aura"` in `theme.json`** — the parent chain is the deprecated
Lumo-era mechanism, and both classes are plain constant holders that do not
implement `AbstractTheme`, so neither can be a `@Theme(themeClass=…)` or a
`theme.json` parent at all.

```java
@Push
@StyleSheet(Aura.STYLESHEET)                        // com.vaadin.flow.theme.aura.Aura
@StyleSheet(JmixAura.STYLESHEET)                    // io.jmix.flowui.theme.aura.JmixAura
@StyleSheet("themes/<app>-aura/styles.css")         // your own CSS, loaded last
@SpringBootApplication
public class MyApplication implements AppShellConfigurator {
```

Order matters: the theme stylesheets come before your own. **Delete the `@Theme`
annotation** and its import — with the folder gone it has nothing to resolve,
and it is deprecated in Vaadin 25 anyway.

**Delete `@StyleSheet(Lumo.UTILITY_STYLESHEET)` and the `Lumo` import too** —
the upgrade added them, and they load a Lumo stylesheet next to Aura. There is
no Aura counterpart to swap in. **Do Step 6 first if the project uses any Lumo
utility class**, or the layout silently collapses the moment this line goes.

## Step 3 — delete the old theme wiring

```bash
git rm -r <theme folder from Step 0>   # normally src/main/frontend/themes/<app>
git rm src/main/resources/vaadin-featureflags.properties
```

Removing the feature flag is required, not cleanup. `themeComponentStyles`
injects **Lumo** component styles; under Aura it produces a half-styled UI that
looks plausible enough to miss:

- text fields lose their borders (underline-only)
- checkboxes render blank
- icons render as empty squares
- `window.Vaadin.featureFlags.themeComponentStyles` reads `true` in the console

Do NOT read `--_lumo-vaadin-*-inject` rules in `document.adoptedStyleSheets` as
the symptom — they are present under Aura too, with the flag off. See **Verify**.

If the file holds other flags, drop only the `themeComponentStyles` line.

## Step 4 — clean the frontend bundle (mandatory, and easy to skip)

The documented post-migration step, and required in every mode — the generated
frontend is stale in all of them and `vaadinClean` is the only thing that clears
it. It removes `src/main/bundles`, `node_modules`, `.vaadin` and usually the
generated frontend, but not always everything, so check what survived:

```bash
./gradlew clean vaadinClean
# Run this BEFORE starting the app — a successful start legitimately recreates
# src/main/bundles and node_modules, so afterwards it reports phantoms.
for d in src/main/bundles node_modules .vaadin src/main/frontend/generated; do
  [ -e "$d" ] && echo "STILL PRESENT: $d  (delete it)"
done
```

Those paths are gitignored, so deleting them by hand is safe. Deleting
`src/main/frontend/generated` alone is NOT enough.

**How you confirm it worked depends on the mode.** In the default dev-bundle
mode the stale artefact is the Lumo-era `src/main/bundles/dev.bundle`, which is
otherwise reused and **keeps serving Lumo component styles** while Vaadin logs
the reassuring `A development mode bundle build is not needed`. You want the
next start to log `... is needed`. Under `vaadin.frontend.hotdeploy=true` there
is no bundle and that line never appears, and a production build rebuilds it
regardless — in both cases the log tells you nothing and the browser checks
under **Verify** are the only real signal.

## Step 5 — port your own CSS to Aura tokens

Every `--lumo-*` name is undefined under Aura and fails silently. The two
families are not name-for-name equivalents — translate by meaning:

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

Lumo's `-primary-` / `-success-` / `-error-` / `-warning-` families each carry a
`-color` / `-text-color` / `-contrast-color` trio; Aura's counterparts are a fill
plus a `-text` variant with no contrast token, so "text on a coloured fill" has
to be written by hand. Where you relied on `--lumo-size-*` or on a percentage
step, define your own token rather than inventing an `--aura-` name. The table
translates only — for what Aura actually defines, and the command to enumerate
it, see `jmix-style-ui`.

Global density comes from two root numbers rather than three independent
scales, so a Lumo "compact preset" collapses to overriding these two knobs:

```css
html {
  --aura-base-font-size: 14;   /* default 14; drives the font-size scale      */
  --aura-base-size: 16;        /* default 16; drives the gap and padding scales */
}
```

Aura ships no compact preset and there are no canonical "compact" values — lower
the two defaults to taste and check in a browser. Unlike Lumo, you cannot shrink
spacing independently of control sizing.

## Step 6 — port the Lumo utility classes

**Aura has no utility-class layer, and there is nothing to migrate them *to*.**
Lumo shipped a whole stylesheet of `p-m`, `gap-s`, `flex`, `text-secondary`,
`bg-contrast-5` …; `aura.css` defines only `.aura-accent-*`, `.aura-surface*`
and the `.v-error` / `.v-success` state classes. There is no
`Aura.UTILITY_STYLESHEET` and no renamed equivalent. Every utility class in the
project stops resolving the moment Step 2 drops the stylesheet, and **nothing
reports it** — the class attribute stays in the DOM and matches no rule, so
padding, gaps and flex direction simply vanish.

So this is not a rename. Each utility class in use becomes either a project CSS
rule or an equivalent declaration on the component.

### Find every use — the `LumoUtility` symbol is not enough

The constants resolve to plain strings, so the class name is just as likely to
be written as a literal — `addClassName("p-m")`, a `classNames` attribute, or a
name built by concatenation. Search for the rendered names, not only the symbol:

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

The second search is deliberately broad and will over-match — `flex`, `grid`
and `hidden` are ordinary words. Read the hits; do not act on the count. What
you want out of it is **the set of distinct utility names the project actually
uses**, which is usually a dozen or fewer.

### Replace them

Write the survivors into your own theme CSS, against Aura tokens. Most map to
one declaration:

| Lumo utility | What to write |
|---|---|
| `p-*`, `m-*`, `px-*`, `pt-*`, … | `padding` / `margin` from `--vaadin-padding-*` |
| `gap-*` | `gap: var(--vaadin-gap-*)` |
| `text-xs…text-xxxl` | `font-size: var(--aura-font-size-*)` (5 steps only — see Step 5) |
| `font-bold`, `font-medium` | `font-weight: var(--aura-font-weight-*)` |
| `text-secondary`, `text-body` | `color: var(--vaadin-text-color-secondary)` / `--vaadin-text-color` |
| `text-error`, `text-success` | `color: var(--aura-red-text)` / `var(--aura-green-text)` |
| `bg-contrast-*`, `bg-base` | `background: var(--vaadin-background-container*)` — no alpha scale, see Step 5 |
| `rounded-*` | `border-radius: var(--aura-base-radius)` |
| `shadow-*` | `box-shadow: var(--aura-shadow-xs/-s/-m)` |
| `flex`, `flex-col`, `items-center`, `justify-between`, `w-full`, `hidden`, … | plain CSS — no token involved, write the property directly |

The layout family is the bulk of real usage and needs no tokens at all, so the
cheapest port for a project with many of them is a small project-owned utility
sheet that redefines just the names in use:

```css
/* src/main/resources/META-INF/resources/themes/<app>-aura/<app>.css */
.flex         { display: flex; }
.flex-col     { flex-direction: column; }
.items-center { align-items: center; }
.gap-m        { gap: var(--vaadin-gap-m); }
.p-m          { padding: var(--vaadin-padding-m); }
```

That keeps the existing `classNames` and Java call sites untouched. Do it only
for names the project uses — re-creating Lumo's full utility sheet re-creates
the problem the migration is supposed to end. Prefer Flow's own layout API
(`setFlexDirection`, `setAlignItems`, `setPadding`) where you are editing the
component anyway.

Verify in the browser, not in the build — a missing utility class is exactly
the silent failure this skill is about. See **Verify**.

## Step 7 — audit theme variants and view class names

**Variants are theme-specific and compile either way.** Jmix 3.0 removed
`always-float-label`, `contained`, `outlined`; support now depends on the active
theme. Under Aura, `contrast` is styled only as
`[theme~='badge'][theme~='contrast']`, so `themeNames="contrast"` on a button
does nothing at all.

```bash
grep -rn "themeNames=" src/main/resources --include='*.xml'
grep -rn "LUMO_" src/main/java --include='*.java'
```

**Class names the Aura CSS keys off must exist in your descriptors.** The
generated `main-view.css` styles the header only through the app-layout class:

```css
vaadin-app-layout.jmix-main-view-app-layout:not([primary-section='navbar']) .jmix-main-view-header { … }
```

A 2.x `main-view.xml` has a bare `<appLayout>`. Without the class every header
rule silently no-ops — no surface, no border, no radius, no padding, leaving the
drawer toggle floating on the page background:

```xml
<appLayout classNames="jmix-main-view-app-layout">
```

Diff your `main-view.xml` against the fresh project's. The rules above are for
the drawer-based MainView; a project using the horizontal menu keys off the
`jmix-main-view-top-menu-*` classes in `main-view-top-menu.css` instead.

`<listMenu themeNames="toggle-reverse"/>` moves the menu toggles to the trailing
edge — purely cosmetic, adopt it only if you want that look.

## Verify — in a browser, on computed values

Compile, static analysis and `clean test` all pass while the app still renders
Lumo. They prove nothing here. Start the app and check:

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

**The component test is the primary check** — tokens can resolve while components
still render Lumo. Compare against the reference Aura project: a text field's
`[part="input-field"]` should have a real border width and Aura's radius, not
`0px` and Lumo's smaller one. Read the pair off your reference project rather
than hard-coding numbers — they are theme CSS and can move between releases. If
tokens resolve but components still look Lumo, you skipped Step 4.

**Never test `adoptedStyleSheets` for `_lumo-vaadin`.** That substring is present
on a correctly migrated Aura app, so it reports failure on a success: it is
Vaadin's per-page component-detection sentinel, a single `:root::before` rule
whose `transition` lists `--_lumo-vaadin-*-inject` properties and which kept the
legacy prefix. It declares no `--lumo-*` properties, and its token list changes
as you navigate — so do not try to recognise it by length either.

### The login view is not enough

Everything past login needs an authenticated session, and an agent must not type
passwords into the form. That matters here because **the Step 7
`jmix-main-view-app-layout` class — the most failure-prone edit in this skill,
and a silent one — can only be verified on MainView.**

Run checks 1-3 and the component test on the login view yourself: they cover
token resolution, stylesheet order, and component borders. Then ask the user to
log in, and check the header:

```js
const h = getComputedStyle(document.querySelector('.jmix-main-view-header'));
[h.borderBottomWidth, h.borderStartStartRadius]   // real values, not ['0px','0px']
document.querySelector('vaadin-app-layout').classList.contains('jmix-main-view-app-layout')
```

`0px` / `0px` means the class is missing and every header rule is a no-op. While
logged in, also walk the list views for an error overlay, a raw `msg://` caption,
and a clean server log — and re-run check 3 there, since its token list differs
per page.

## Forbidden

- `"parent": "jmix-aura"` in `theme.json` instead of `JmixAura.STYLESHEET`.
- Keeping `@Theme` together with the Aura `@StyleSheet` declarations.
- Keeping `@StyleSheet(Lumo.UTILITY_STYLESHEET)` after the switch, or looking
  for an `Aura.UTILITY_STYLESHEET` to replace it with — there is none.
- Dropping the Lumo utility stylesheet without porting the classes the project
  uses (Step 6), or clearing the project by grepping for `LumoUtility` alone —
  the names are plain strings and are often written as literals.
- Leaving `com.vaadin.experimental.themeComponentStyles=true` enabled.
- Starting the app without `./gradlew clean vaadinClean` and concluding the
  theme "did not change anything".
- Translating a `--lumo-` name to an `--aura-` prefix and assuming it exists.
- Hand-writing the Aura view CSS instead of copying the generated scaffold.
- Calling the migration done on a green compile or `clean test` — neither
  renders a page.
- Treating `_lumo-vaadin` in `adoptedStyleSheets` as proof the migration failed;
  it is present under Aura too.
- Calling the migration verified from the login view alone — the MainView header
  rules are untested until someone logs in.
