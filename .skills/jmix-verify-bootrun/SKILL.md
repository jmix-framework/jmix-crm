---
name: jmix-verify-bootrun
description: Gate 2 (mandatory) — verify the Spring/Jmix context loads via a TERMINATING `gradle clean test`, never a long-running bootRun. Gate 3 (optional) — if you have a browser/UI-automation tool, walk the views/buttons/fields you created to catch render-time defects that compile and clean test miss. A green compile/clean test is NOT proof a view renders.
---

# Gate 2 (context load) + Gate 3 (optional render walk)

## When to use

After writing your Jmix artifacts (entities, views, services, roles, …):
- **Gate 2 (mandatory):** confirm the Spring/Jmix context loads cleanly.
- **Gate 3 (optional):** confirm each view you created actually renders.

These run AFTER static analysis. Treat inspection findings (from MCP
inspection tools when connected, your IDE otherwise) as WARNINGS to resolve
first — and remember an EMPTY inspection result you did not confirm is
false-clean, not a pass.

## Gate 2 — `clean test` (NEVER bootRun)

Run the project's terminating context-load test:

```bash
./gradlew --no-daemon clean test
```

It boots the Spring/Jmix context, runs the project's seed tests, and EXITS.
Confirm `BUILD SUCCESSFUL` and no failed tests. Also confirm tests actually
ran: `Tests run: 0` (or no test task at all) means the context was never
booted and Gate 2 did NOT pass — ensure at least one context-load test exists
and executed.

**NEVER use `./gradlew bootRun` (or any non-terminating server start) as the
Gate-2 check.** bootRun does not exit — it hangs your turn and leaves the HTTP
port locked by a zombie process. bootRun is not a gate; Gate 2 is `clean test`.

If a seed test is RED after your changes, YOU broke it — fix it to green. Do
not call a red `clean test` "pre-existing".

A green Gate 2 is necessary but NOT sufficient: the seed tests load the context
but do NOT open your new views or exercise your new roles. It catches
catastrophic breakage (broken view registry, schema/Liquibase error, missing
`@JmixEntity`), not render-time UI defects — those are caught by the mechanical
checks and the optional Gate 3.

## Gate 3 — render walk (REQUIRED for every view/role you created or changed)

Gate 3 is the ONLY gate that opens your new views/roles at runtime (Gate 2 does
not), so it is NOT optional when you created or changed one. Per such view, pick
ONE:
- a browser/UI-automation tool is connected → render-walk it (below); or
- no browser tool → write a minimal headless `@UiTest` that navigates to the
  view and asserts it opens (see `jmix-create-test`).

Do not write "no browser tool" without checking the connected tools. Only if a
view can be reached by NEITHER a browser tool NOR a `@UiTest`, skip it — and then
report `render NOT verified: <view>`, never "all gates passed".

**Prerequisite — Spring Boot Actuator.** The readiness probe below polls
`/actuator/health`, so the project must include the Actuator starter and expose
the `health` endpoint:

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

```properties
# application.properties
management.endpoints.web.exposure.include=health
```

Jmix projects do NOT bundle Actuator by default — add the dependency and
property above (verify with `./gradlew dependencies | grep actuator`). If you
cannot add/configure it, use the `@UiTest` path instead, or an alternative
readiness signal: tail the boot log for `Started <App>Application in ...`.

If you do have one, run the mechanical checks first, then:

1. Start the app in the BACKGROUND so it does not block your turn, capturing its
   log — e.g. `nohup ./gradlew --no-daemon bootRun > /tmp/jmix_app.log 2>&1 &`.
2. Poll readiness on the Spring Boot Actuator health endpoint, then proceed — do
   not wait on the (non-terminating) process:
   ```bash
   curl --retry-connrefused --retry 40 --retry-delay 2 -sf -m 5 \
        http://localhost:8080/actuator/health && echo READY
   ```
   Use `/actuator/health` — Spring Boot does not expose a bare `/health`. If it
   never becomes ready, tail the log, skip Gate 3, and shut the process down.
3. With your browser/UI-automation tool, navigate to each view you created, click
   each button/action, fill each field, and confirm no error overlay or server
   exception.
4. **Shut the background app down** when finished (`kill` the bootRun PID; if the
   port stays held, kill whatever holds it). Always leave the port free.

## Honest scope — Gate 3 is a render gate

A browser walk catches the gross render exceptions a user would hit, but the
test suite may exercise a different, headless server-side code path
(`jmix-flowui-test-assist` `@UiTest`). A defect can therefore pass in the
browser and still fail a test (e.g. a required default set only in the view, or
a non-standard save-action id). Never let a green render walk override the
mechanical checks.
