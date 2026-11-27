---
name: jmix
description: Use before writing or changing Java code, view descriptors, Liquibase changelogs, or build files in a Jmix 2 application. Says which jmix-* skill covers each kind of artifact, which source to follow, and which checks close a task.
---

# Working on a Jmix application

Stack: Java 17, Jmix 2, Spring Boot 3, Vaadin 24, Gradle, relational database
with Liquibase migrations.

## Before you write

Wrong API names and broken descriptors usually come from writing from memory.

1. List the artifacts the task creates or changes, in the order you will
   write them.
2. Take them one at a time. Just before you write an artifact, look for a
   working example in the same module that covers the case you write.
3. No example → read the matching skill in full at this point. Example →
   follow it and check your change against the skill's **Forbidden** section.
   Do not read the skills for later artifacts before you reach them.
4. Verify every Jmix/Vaadin class, method, constant, or icon not already used in
   `src/` with `jmix-verify-api-symbol`.

Edits to documentation only, or to the text of existing message keys, need none
of this and no gates.

## Skill routing

- New project with Jmix CLI: `jmix-create-project`
- Verify a Jmix/Vaadin API: `jmix-verify-api-symbol`
- Static checks / inspections: `jmix-ide-static-analysis`
- Tests (Gate 2) failures, render walk: `jmix-verify-bootrun`
- Persistent entity: `jmix-create-entity`
- Enum used by an entity: `jmix-create-enum`
- List view: `jmix-create-list-view`
- Detail view: `jmix-create-detail-view`
- Inline editing of composition children (property-bound container, no query loader): `jmix-create-composition-detail-view`
- Service-layer business logic: `jmix-create-service`
- Code outside a user request (Quartz/`@Scheduled` job, `@Async`, startup runner, message listener): `jmix-run-background-code`
- Detail dialog from a button/action, or master-row selection → filtered child grid: `jmix-add-dialog-detail-flow`
- Entity lifecycle events: `jmix-add-entity-event-listener`
- Database schema: `jmix-create-liquibase-changelog`
- Resource and row-level roles, role assignments in seed data: `jmix-create-resource-role`
- User-visible text, entity and enum captions: `jmix-add-i18n-keys`
- Tests: `jmix-create-test`
- Fetch plans, unfetched references, N+1: `jmix-configure-fetch-plan`
- DTO / non-persistent UI-bound model: `jmix-create-dto-entity`
- Reusable Flow UI fragment: `jmix-create-fragment`
- Styling, theme tokens (`--lumo-*`), CSS classes: `jmix-style-ui`

A new persistent entity needs an entity, a changelog, a resource role, and
message keys. A user-facing one also needs a list and/or detail view, a menu
entry, and a view policy in every role that opens them — including dialog-only
detail views opened from a composition table.

## Which source to follow

Memory is never a source.

- **How this project does things** (id type, packages, naming, architecture):
  the project's code wins. Do not change existing code to match a skill.
- **Whether an API exists in this version** (class, method, constant, XML
  attribute): `jmix-verify-api-symbol` decides — the docs server if connected,
  otherwise `javap` on the resolved jars; if the two disagree, the jars win. If
  the feature needs an API this version lacks and you cannot confirm another
  way, stop and ask.
- **Known failures** (rules a skill lists under Forbidden, or for which it says
  what breaks): the skill wins over docs examples. Do not copy such a failure
  from existing code into new code, and leave the existing code as it is. Any
  other skill rule is a convention, and the project's way wins.

Sample code in a plan, brief, or issue ranks below all of these: it is usually
written without the skill and drops details that look optional. Before writing
it, read the matching skill in full and compare, especially entity annotations,
view descriptors, fetch plans, and lifecycle-method overrides.

In your report, list each conflict (file, skill rule, side followed), each
departure from sample code and why, each skill API missing in this version, and
each failure found in existing code.

## Closing a task — three gates

A task is not done when the code compiles. Run the gates in order and show the
evidence for each.

1. **Gate 1 — API and static checks.** Read `jmix-ide-static-analysis` once per
   session, then check every file you created or changed.
2. **Gate 2 — tests pass.** Run `./gradlew --no-daemon test` (in a composite
   project, see **Gradle tasks in a composite project** below). It passes
   only with `BUILD SUCCESSFUL` and no failed tests. If it fails, read
   **When Gate 2 fails** in `jmix-verify-bootrun`.
3. **Gate 3 — render.** For each view or role you created or changed, write
   and run a `@UiTest` that opens the view, or the role's views as a user of
   that role (`jmix-create-test`). If a browser tool is connected (for example
   Playwright MCP), also read `jmix-verify-bootrun` and walk them in the
   browser as it describes. Then Gate 3 passes only after the walk. Report the
   walk as not done only when something blocked it, such as the application
   not starting, and name what.

Report per changed file its static-check verdict, and per created view, button,
or field how you verified it. "BUILD SUCCESSFUL, all done" is not a report.

### Gradle tasks in a composite project

If the root `settings.gradle` has `includeBuild` lines, the root project has no
`compileJava` or `test` task. Run them from the root directory with the path of
each module that has `src/main/java` (for `compileJava`) or `src/test/java` (for
`test`): the module directory with `/` replaced by `:`. For example, for
`products/products` and `sales`, run
`./gradlew --no-daemon :products:products:test :sales:test`.

## Writing files

Pass absolute paths to file-writing tools. After a batch of writes, `ls` the
paths and confirm each file is non-empty: a 0-byte file passes compile and
`test` (an empty role drops its policies; an empty `*-view.xml` breaks the
view registry). Rewrite a missing or empty file; do not `rm -rf`.

Never edit generated frontend files — they are regenerated on every build.
