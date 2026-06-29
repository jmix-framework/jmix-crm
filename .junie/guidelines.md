# Agent Instructions

Use these instructions when working on a Jmix 2 application.

## Project stack

- Java 17
- Jmix 2, Spring Boot 3, Vaadin 24
- Gradle
- Relational database with Liquibase migrations

## Step 0 — map the task to artifacts, READ the matching skill BEFORE writing

The most common cause of defects is writing a Jmix artifact from memory instead
of from the rule that governs it. Your Jmix/Vaadin priors are the single biggest
source of wrong API names and broken descriptors.

Before writing a single file:

1. List every artifact the task implies — entities, enums, list views, detail
   views, composition children, services, event listeners, resource roles,
   changelogs, menu entries, message bundles.
2. For EACH artifact, READ the matching skill in **Skill routing** before you
   write it.
3. Only then start writing.

The verification skills (`jmix-ide-static-analysis`, `jmix-verify-bootrun`) are
gates, not how-to. They do not replace the artifact skill.

## Tooling — MCP first, universal floor always

This profile may ship MCP servers — a Jmix-aware IDE inspection (e.g. JetBrains
`get_file_problems`), Context7 (`/jmix-framework/jmix-context7`), and Playwright
for the browser. **When a server is connected, it is your PRIMARY check — reach
for it first.** ANY server may be absent; when one is, do NOT skip the check —
fall back to the universal floor: `compileJava`, `./gradlew clean test`, and
the mechanical-floor commands in `jmix-ide-static-analysis`.

## Gates before declaring a task done

A task is NOT done after the code compiles. Three gates, in order; never assert
a gate passed without showing the evidence. At each gate use the MCP tool if it
is connected (primary); fall back to the universal check only when it is not.

| Gate            | Primary — MCP, if connected                                                                                                                                                                                                                                                                                     | Fallback — always available                                                                                             |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------|
| 1 API & static  | verify EVERY Jmix/Vaadin symbol via **Context7** (`/jmix-framework/jmix-context7`) before you type it, AND run the IDE inspection (**`get_file_problems`**) on every file you wrote — for `*-view.xml` it is the only static catch for unresolved `msg://`, invalid property paths, and missing data containers | `compileJava` + the mechanical-floor commands in `jmix-ide-static-analysis`                                             |
| 2 Context loads | *(no MCP substitute — always run the fallback)*                                                                                                                                                                                                                                                                 | `./gradlew --no-daemon clean test` — boots the Spring/Jmix context, runs Liquibase + project tests, then EXITS          |
| 3 Render        | render-walk every view/button/field with the **browser tool** (Playwright) — confirm no error overlay, server exception, or raw `msg://` caption                                                                                                                                                                | no universal substitute — run the mechanical checks (the render-defect floor), then state `render not browser-verified` |

NEVER use `bootRun` (or any non-terminating server start) as the Gate-2 check —
it does not exit and will hang your turn. Gate 2 is `clean test`. If you DO
start a server to render-walk, run it in the background and poll
`/actuator/health` until it is UP before driving the browser, then shut it down
cleanly.

`compileJava` is BLIND to XML descriptors. Every `*-view.xml` defect — a
reference/enum field bound wrong, a broken `itemsQuery`, an action opening a
view id that does not exist (`NoSuchViewException`) — compiles perfectly clean.
A green `clean test` is necessary but NEVER sufficient: the context-load tests
boot the Spring/Jmix context but do NOT open your new views or exercise your
new roles.

Emit the evidence in your completion report. Per file you touched: its
static-check verdict. Per view/button/field you created: how you verified it
(inspection, mechanical check, or render walk). "BUILD SUCCESSFUL, all done"
with no per-file check and no render evidence is a non-answer.

## Anti-hallucination — verify a symbol before you type it

Inventing plausible-looking API names is a top failure mode: they survive
typing but blow up at compile or runtime. Before you type any Jmix/Vaadin
symbol not already used in this project's `src/`, verify it — Context7 is your
PRIMARY check when connected, else an IDE symbol search, else grep this project
for a working example. (If the exact symbol is already used in `src/`, copy
that call site.) High-frequency wrong→right traps are catalogued in
`jmix-verify-api-symbol`.

## Skill routing

READ the most specific skill for each artifact:

- Verify a Jmix/Vaadin API: `jmix-verify-api-symbol`
- Static checks / inspections / mechanical floor: `jmix-ide-static-analysis`
- Gate-2 context-load test (+ optional Gate-3 render walk): `jmix-verify-bootrun`
- Persistent entity: `jmix-create-entity`
- Enum used by an entity: `jmix-create-enum`
- List view: `jmix-create-list-view`
- Detail view: `jmix-create-detail-view`
- Parent-child composition editing (property-bound container, NO query loader): `jmix-create-composition-detail-view`
- Service-layer business logic: `jmix-create-service`
- Detail dialog from a button/action, OR master-row selection → filtered child grid: `jmix-add-dialog-detail-flow`
- Entity lifecycle/event business logic: `jmix-add-entity-event-listener`
- Database schema: `jmix-create-liquibase-changelog`
- Resource roles: `jmix-create-resource-role`
- User-visible text / entity-enum captions: `jmix-add-i18n-keys`
- Tests: `jmix-create-test`
- Fetch plans / unfetched-reference / N+1 tuning: `jmix-configure-fetch-plan`
- DTO / non-persistent UI-bound model: `jmix-create-dto-entity`
- Reusable Flow UI fragment: `jmix-create-fragment`

## Cross-cutting checklist for a new entity / view

For each new persistent entity, run through: `jmix-create-entity` +
`jmix-create-liquibase-changelog` + `jmix-create-resource-role` +
`jmix-add-i18n-keys`. For a user-facing entity, also add a list and/or detail
view (`jmix-create-list-view`, `jmix-create-detail-view`) and a view policy in
every role that can open them — **including dialog-only detail views opened
from a composition table**.

Service- or listener-level defaulting does NOT relieve the entity from
defaulting required fields on initial persist — defaults must work through
`DataManager.create()` + `DataManager.save()` directly (tests bypass the view
layer). See `jmix-create-entity`.

## When tests fail — it is almost never "pre-existing"

If the project ships a passing test suite and a test goes red after your change,
assume you broke it. A red `clean test` means the task is not done; investigate
before declaring a red gate "pre-existing." Common causes:

- **`NoSuchViewException` after you added views** → you broke the VIEW REGISTRY;
  it scans all `@ViewController` classes at startup and one broken view poisons
  navigation to EVERY view, including pre-existing ones. Check, in order: (1) every new
  view `.java` has a `package` line matching its directory — a class in the
  default package registers its `@Route`/`@ViewController` wrong; (2) no two
  `@ViewController(id=…)` share an id; (3) every `@ViewDescriptor` path resolves
  to a real XML next to the class; (4) no `*-view.xml` is empty/malformed — an
  empty descriptor throws `SAXParseException: Premature end of file` and poisons
  the registry.
- **`MetaClass not found for class X`** → the entity is missing `@JmixEntity`, or
  its package is outside the application scan root.
- **`ConstraintViolationException` on save** → a `@NotNull` persistent field has
  no value on the `DataManager` path (see `jmix-create-entity`).

Fix the cause, re-run `clean test` until green. A test that goes red and you
cannot explain is a blocker, never a footnote in your "done" summary.

## File-write trap

Always pass absolute paths to file-writing tools; in nested-project layouts the
working directory may not be what you assume. After a batch of writes, `ls` the
path you intended AND confirm each file is NON-EMPTY — a tool that silently
writes a 0-byte file leaves a defect that compile and `clean test` will NOT
catch (an empty role class drops all its policies; an empty `*-view.xml`
poisons the view registry). If a file is missing or empty, find and rewrite
it; do NOT `rm -rf` to "clean up".

Never edit generated frontend files — they are regenerated on every build.
