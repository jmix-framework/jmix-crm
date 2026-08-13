# JST-5452 — reusable theme preview testing

This branch verifies that View Designer Preview loads CSS resources from project module dependencies
without adding the complete dependency artifact to the isolated dev-server classloader.

The project uses Jmix 3.0.1 and Vaadin 25.1.7. Vaadin 25 recommends `@StyleSheet`, while deprecated
`@Theme` is still available as a compatibility path. Both models are covered here.

Studio implementation under test:

- `FlowThemeJarBuilder` resolves project, framework, and dependency resources and creates a resource-only JAR;
- `FlowDevServerManager.loadThemeResources` adds that JAR and the resolved stylesheet URLs to the preview;
- Registry key `io.jmix.designer.flowui.theme.dependencies.enabled` disables the feature when needed.

## Fixture layout

| Path                  | Purpose                                                                    |
|-----------------------|----------------------------------------------------------------------------|
| `crm-theme/`          | Dependency module containing modern and legacy reusable-theme layouts      |
| `settings.gradle`     | Includes the `crm-theme` module                                            |
| `build.gradle`        | Uses the module dependency by default and contains an optional JAR variant |
| `CRMApplication.java` | Contains mutually exclusive annotation scenarios                           |
| `themes/crm-preview/` | Project theme that inherits dependency theme `cobalt` for case 4           |

The dependency publishes these CSS entry points:

| Resource                                      | Usage                                                      |
|-----------------------------------------------|------------------------------------------------------------|
| `META-INF/resources/cobalt/master.css`        | Modern `@StyleSheet("cobalt/master.css")` layout           |
| `META-INF/resources/themes/cobalt/styles.css` | Reusable-theme layout for `@StyleSheet` or legacy `@Theme` |
| `META-INF/resources/themes/cobalt/theme.json` | Declares framework theme `jmix-lumo` as parent             |

`master.css` displays a blue top banner and gives buttons a dashed blue border. The reusable theme
displays a brown bottom banner and gives buttons a dotted brown border. Relative `@import` files verify
that resources next to the entry point are also available.

The dependency also contains `META-INF/resources/frontend/cobalt-addon.js`, which imports a nonexistent
npm package. It must not be copied into the resource-only preview JAR.

## Preparation

1. Run `./gradlew :jmix-intellij:runIde` in `jmix-studio` on `feature/JST-5452`.
2. Open this project from branch `qa/JST-5452` in the sandbox IDE.
3. Reimport Gradle and check that `crm-theme` is shown as a module.

Do not build `crm-theme` for the default module-dependency tests. Studio resolves its
`src/main/resources` source root through the IntelliJ module model; no `build` directory is involved.

After every annotation or dependency change, use **Restart View Designer Preview**, reopen a view, and
switch to its **Preview** tab. Reopening matters because an already open preview can retain old state.

## Case 1 — flat dependency stylesheet, active by default

Keep the existing project/framework stylesheets and this annotation:

```java
@StyleSheet("cobalt/master.css")
```

Leave the other JST-5452 annotations commented. Expected:

- the preview starts without HTTP 500;
- the blue `COBALT master.css LOADED FROM DEPENDENCY` banner is visible;
- buttons have dashed blue borders;
- the existing Aura/Jmix/project styles remain applied.
- `GET /b2b-crm/cobalt/master.css` returns HTTP 200 in the running application; the test security
  configuration explicitly permits `/cobalt/**` because this path is outside Vaadin's standard theme folders.

This is the primary Vaadin 25/Jmix 3 scenario. `theme.json` inheritance is intentionally not consulted
for a pure `@StyleSheet` declaration.

## Case 2 — stylesheet inside a reusable-theme folder

Comment out case 1 and uncomment:

```java
@StyleSheet("themes/cobalt/styles.css")
```

Keep the three existing Aura/Jmix/project `@StyleSheet` annotations. Expected:

- the brown `COBALT themes/cobalt/styles.css LOADED FROM DEPENDENCY` banner is visible;
- buttons have dotted brown borders;
- the existing project styles remain applied.

This checks that a `themes/<name>` path works as a normal public stylesheet without invoking legacy
theme inheritance.

## Case 3 — standalone dependency theme through legacy `@Theme`

Comment out **all** `@StyleSheet` annotations in `CRMApplication`, then uncomment:

```java
@Theme("cobalt")
```

Only one `@Theme` annotation may be active. Expected:

- the preview starts and the brown dependency-theme banner is visible;
- `themes/cobalt/theme.json` is resolved;
- parent entry point `themes/jmix-lumo/styles.css` is applied before cobalt;
- the generated JAR keeps the `themes/cobalt` name because this is Jmix 3.

The `@StyleSheet` annotations must be disabled for this case because Studio deliberately prefers the
modern stylesheet model when both models are present.

## Case 4 — project theme with a dependency parent

Comment out all `@StyleSheet` annotations and case 3, then uncomment:

```java
@Theme("crm-preview")
```

The project file `src/main/resources/META-INF/resources/themes/crm-preview/theme.json` declares:

```json
{
  "parent": "cobalt"
}
```

The resulting chain is:

```text
project crm-preview -> dependency cobalt -> framework jmix-lumo
```

Expected:

- a green project-theme banner and the brown cobalt parent-theme banner are both visible;
- the generated JAR contains dependency resources but no `themes/crm-preview` project resources;
- parent styles are registered before child styles.

After this case, restore the default annotations before testing cases 5–8.

## Case 5 — JAR dependency instead of a module dependency

Build only the fixture artifact:

```bash
./gradlew :crm-theme:jar
```

In `build.gradle`, comment variant A and uncomment variant B:

```groovy
// implementation project(':crm-theme')
implementation files('crm-theme/build/libs/crm-theme-1.0.0.jar')
```

Reimport Gradle and repeat cases 1–4. Expected behavior is identical; the Studio log names the JAR
instead of `crm-theme/src/main/resources`.

The production mechanism does not depend on module output directories: this case exists only to test
an actual external JAR dependency.

## Case 6 — fail-fast diagnostics

With the module dependency restored, temporarily change the active custom stylesheet to:

```java
@StyleSheet("cobalt/missing.css")
```

Restart preview. Expected:

- preview startup fails before Jetty renders a partially styled page;
- the Studio notification and `idea.log` name `cobalt/missing.css` as a missing required resource.

Restore `cobalt/master.css` afterwards. To check invalid parent diagnostics, temporarily set the parent
in `crm-theme/.../themes/cobalt/theme.json` to a nonexistent theme and repeat case 3 or 4.

## Case 7 — feature kill switch

Open Registry and disable:

```text
io.jmix.designer.flowui.theme.dependencies.enabled
```

Restart preview with case 1 active. Expected: preview starts with the normal project styles, but the blue
dependency banner is absent and no theme-preview JAR is generated. Enable the key again afterwards.

## Case 8 — cache behavior

Start the same preview twice without modifying resources. The second start must reuse the same
`<hash>.jar`. Then edit a CSS file under `crm-theme/src/main/resources` and restart preview. A different
hash must be generated even if file size or timestamp happens to be unchanged.

The cache is cleaned on preview startup. JARs unused for seven days are eligible for deletion.

## Log and generated-JAR checks

Sandbox log:

```text
jmix-studio/.intellijPlatform/sandbox/jmix-studio-bootstrap/IU-2026.1/system/log/idea.log
```

Expected messages include:

```text
[FlowDevServerManager]: Theme resources from module dependencies repacked to .../jmix/theme-preview/<hash>.jar
... resource files repacked from [.../crm-theme/src/main/resources] to ...
```

Generated JAR directory:

```text
jmix-studio/.intellijPlatform/sandbox/jmix-studio-bootstrap/IU-2026.1/system/jmix/theme-preview/
```

Inspect the newest JAR:

```bash
unzip -l <path-to-generated.jar>
```

With the current fixture it must contain these files plus parent directory entries:

```text
META-INF/resources/cobalt/master.css
META-INF/resources/cobalt/parts/buttons.css
META-INF/resources/themes/cobalt/styles.css
META-INF/resources/themes/cobalt/theme.json
META-INF/resources/themes/cobalt/views/main-view.css
```

It must not contain:

```text
META-INF/resources/frontend/cobalt-addon.js
META-INF/resources/themes/crm-preview/**
*.class
META-INF/services/**
META-INF/spring.factories
```

The explicit `META-INF/resources/themes/` ZIP directory entry is required for Vaadin theme discovery.

## Restore the default state

Before finishing, restore:

```java
@StyleSheet(Aura.STYLESHEET)
@StyleSheet(JmixAura.STYLESHEET)
@StyleSheet("themes/aura/styles.css")
@StyleSheet("cobalt/master.css")
```

Keep both `@Theme` annotations and the reusable-theme stylesheet case commented. Restore module dependency
variant A and enable the Registry key.

To remove the fixture completely, remove `crm-theme`, its `settings.gradle` include, the JST-5452 block
from `build.gradle`, the four JST-5452 annotation lines/import from `CRMApplication`, this document, and
`src/main/resources/META-INF/resources/themes/crm-preview`.
