# JST-5452 — testing in this project

Checks that the view designer preview picks up theme resources from **module dependencies**, and that
nothing else from those dependencies reaches the isolated dev server classloader.

Plugin side: `FlowThemeJarBuilder` + `FlowDevServerManager.loadThemeResourcesJar`, branch
`feature/JST-5452` of `jmix-studio`. This project is Jmix 3.0.1, so it covers the Vaadin 25 model:
`@Theme` is deprecated and styles are loaded with `@StyleSheet` from `META-INF/resources`.

## What was added here

| Path                  | Purpose                                                                   |
|-----------------------|---------------------------------------------------------------------------|
| `crm-theme/`          | reusable theme add-on, `java-library` with **no** dependencies            |
| `settings.gradle`     | `include 'crm-theme'`                                                     |
| `build.gradle`        | `implementation project(':crm-theme')` in the *JST-5452 TEST THEME* block |
| `CRMApplication.java` | `@StyleSheet("cobalt/master.css")` + a commented alternative              |

The add-on ships two theme layouts at once so both resolution paths can be tested:

| Layout                                 | Applied with                                                                 | Era                  |
|----------------------------------------|------------------------------------------------------------------------------|----------------------|
| `META-INF/resources/cobalt/master.css` | `@StyleSheet("cobalt/master.css")`                                           | Vaadin 25 / Jmix 3   |
| `META-INF/resources/themes/cobalt/`    | `@StyleSheet("themes/cobalt/styles.css")`, or `@Theme("cobalt")` on Jmix 2.x | Vaadin 24 / Jmix 2.x |

Each entry point paints a fixed banner in the preview — blue on top, brown at the bottom — and dashed
or dotted borders on `vaadin-button` coming from a relative `@import`. No DevTools needed.

It also ships four **poison** entries that must never reach the dev server classpath:

| Entry                                                           | What it would do if the artifact were added as is                                         |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| `META-INF/services/jakarta.servlet.ServletContainerInitializer` | Jetty scans the extra classpath for SCIs → class missing → context fails to start         |
| `com/company/crm/theme/CobaltThemeConfiguration.class`          | not a valid class file; classes of project artifacts must not be scanned or loaded at all |
| `META-INF/spring.factories`                                     | Spring descriptor; the dev server classloader has no Spring                               |
| `META-INF/resources/frontend/cobalt-addon.js`                   | imports a non-existent npm package → the frontend build of the preview breaks             |

## Preparation

1. `./gradlew :jmix-intellij:runIde` in `jmix-studio` on branch `feature/JST-5452`.
2. Open this project in the sandbox IDE, reimport Gradle (the new `crm-theme` module must appear).

Building the project is **not** required: for a module dependency the resources are taken from its
resource root (`crm-theme/src/main/resources`), not from `build/`.

The whole feature can be turned off in Registry: `io.jmix.designer.flowui.theme.dependencies.enabled`.

## Cases

After each annotation change: restart the dev server (Jmix tool window → *Restart View Designer Preview*),
then open any view and switch to the **Preview** tab.

### 1. Flat stylesheet path — Vaadin 25 layout (active by default)

```java
@StyleSheet("cobalt/master.css")
```

Expected: blue banner `COBALT master.css LOADED FROM DEPENDENCY` on top, dashed blue borders on buttons.

### 2. Reusable theme folder

In `CRMApplication`, comment out case 1 and uncomment:

```java
@StyleSheet("themes/cobalt/styles.css")
```

Expected: brown banner `COBALT themes/cobalt/styles.css LOADED FROM DEPENDENCY` at the bottom, dotted
borders on buttons.

### 3. Jar dependency instead of a module dependency

```bash
./gradlew :crm-theme:jar
```

In `build.gradle`, comment out variant A and uncomment variant B
(`implementation files('crm-theme/build/libs/crm-theme-1.0.0.jar')`), reimport Gradle, restart the
dev server. Expected: same picture as cases 1–2; the log line now names the jar instead of the
`src/main/resources` directory.

### 4. Project styles are not broken

`@StyleSheet(Aura.STYLESHEET)`, `@StyleSheet(JmixAura.STYLESHEET)` and
`@StyleSheet("themes/aura/styles.css")` stay in place during all cases above. Expected: the project theme
still applies as before, and its files are **not** in the generated jar — framework stylesheets and the
project's own `src/main/resources/META-INF/resources` are deliberately skipped.

## What to check besides the picture

**idea.log** (sandbox: `jmix-studio/.intellijPlatform/sandbox/jmix-studio-bootstrap/IU-2026.1/system/log/idea.log`):

```
[FlowDevServerManager]: Theme resources from module dependencies repacked to …/jmix/theme-preview/<hash>.jar
… resource files repacked from [/…/crm-theme/src/main/resources] to …
```

**The generated jar** — `<IDEA system dir>/jmix/theme-preview/<hash>.jar`
(sandbox: `jmix-studio/.intellijPlatform/sandbox/jmix-studio-bootstrap/IU-2026.1/system/jmix/theme-preview/`):

```bash
unzip -l <path-to-generated>.jar
```

Expected — these five files plus their directory entries, and nothing else. The directory entries matter:
Vaadin discovers jars with themes by looking up the `META-INF/resources/themes/` directory resource on the
classpath.

```
META-INF/resources/cobalt/master.css
META-INF/resources/cobalt/parts/buttons.css
META-INF/resources/themes/cobalt/styles.css
META-INF/resources/themes/cobalt/theme.json
META-INF/resources/themes/cobalt/views/main-view.css
```

No `*.class`, no `META-INF/services`, no `META-INF/spring.factories`, no `META-INF/resources/frontend`,
nothing from `themes/aura` or `themes/jmix-aura`.

**Caching:** a second dev server start must reuse the same `<hash>.jar` — no new file and no
"resource files repacked" line. Edit a CSS file in `crm-theme/src/main/resources`, restart the server →
a new `<hash>.jar` appears (for the jar variant, rebuild the jar first).

Note: this project is Jmix 3, so the theme folder keeps its name. On Jmix 2.x the theme is repacked as
`themes/preview-theme` because the 2.x dev server renders the preview with `@Theme("preview-theme")` —
see the same document in `jixflow28`.

**Negative case:** the fact that the preview renders at all is the check. The add-on declares a
`ServletContainerInitializer` that does not exist; had the dependency been put on the dev server
classpath as is, the Jetty context would have failed with a `ServiceConfigurationError`.

## Revert

Remove the `crm-theme` folder, the `include 'crm-theme'` line, the *JST-5452 TEST THEME* dependency block,
and the two `// JST-5452` annotations in `CRMApplication`.
