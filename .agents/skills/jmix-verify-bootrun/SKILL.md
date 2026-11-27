---
name: jmix-verify-bootrun
description: Use when Gate 2 (gradle test) fails, and for Gate 3, which opens every view you created or changed with a @UiTest and, when a browser tool is connected, walks views and roles in a browser.
---

# Gate 2 (tests) and Gate 3 (render)

Run these after static analysis (`jmix-ide-static-analysis`). Resolve its
findings first; an empty inspection result you did not confirm is not a pass.

## Gate 2 — tests pass

1. Check whether an application from this checkout is running (a `bootRun` or
   `java` process from this directory, or the application port held). If one
   is, read **When an application from this checkout is already running**
   first.
2. Run `./gradlew --no-daemon test`. In a composite project, use the task
   paths from **Gradle tasks in a composite project** in the `jmix` skill.
3. It passes only with `BUILD SUCCESSFUL` and no failed tests.
4. If it fails, read **When Gate 2 fails**.

A green Gate 2 proves that the existing tests pass. It does not open your
views, use your roles, or run code outside a user request
(`jmix-run-background-code`). When you add a code path that nothing calls yet,
add the caller — a test or a render walk — or report the path as unverified.

## Gate 3 — render

Required for every view or role you created or changed. Run the mechanical
checks of `jmix-ide-static-analysis` first. Then:

1. For each view, write a minimal `@UiTest` that opens the view
   (`jmix-create-test`) and run it.
2. Check the connected tools for a browser tool; if one is listed by name
   only, load it with your tool search. If there is one, also walk
   each view and role in the browser — first read `references/browser-walk.md`
   in this skill's folder. If an application from this checkout is already
   running, read **When an application from this checkout is already
   running** before the walk.

If a view has no passing `@UiTest`, report `render NOT verified: <view>`. If no
browser tool is connected or the walk could not run, report the browser walk
as not done for each view and role, and why.

## Forbidden

- Reporting no browser tool without loading one that is listed by name only.
- Counting `Tests run: 0` as a pass.
- Calling a test that went red after your change "pre-existing", or reporting a
  red test you cannot explain as a footnote instead of a blocker.
- "All gates passed" for a code path that no gate entered.
- Overwriting the jars of a running application, starting a second instance, or
  stopping the owner's application without the owner's approval.
- Editing `application.properties` to point a walk at another database.
- Setting a component's value or property from the page or console during a
  walk.
- Leaving an application you started running after the walk.

## Special cases

### When Gate 2 fails

The console tail may show only a wrapper exception, such as `RuntimeException
at HikariConfig.java:514`. Read the JUnit XML first:

```bash
rg -n -C 3 "Failed to load driver class|Caused by" build/test-results/test/*.xml
```

- **`Failed to load driver class org.hsqldb.jdbc.JDBCDriver`** while
  `src/test/resources/application-test.properties` configures HSQLDB: the test
  runtime lacks the driver. Add `testRuntimeOnly 'org.hsqldb:hsqldb'` to
  `build.gradle` and run again. Do not change the test datasource or
  investigate unrelated application code.
- **`NoSuchViewException` after you added views**: one broken view breaks
  navigation to every view. Check in order: each new view `.java` has a
  `package` line that matches its directory; no two `@ViewController(id=…)`
  share an id; each `@ViewDescriptor` path resolves to an XML next to the class;
  no `*-view.xml` is empty or malformed (`SAXParseException: Premature end of
  file`).
- **`MetaClass not found for class X`**: the entity lacks `@JmixEntity`, or its
  package is outside the application scan root.
- **`ConstraintViolationException` on save**: a `@NotNull` field has no value on
  the `DataManager` path (see `jmix-create-entity`).
- **`ClassCastException` between two classes with the same name**
  (`OrderDetailView cannot be cast to OrderDetailView`): IDE hot deploy left a
  stale class in `.jmix/conf/`, which `test` does not remove. Delete the
  directory and run again.

If the suite passed before your change and a test is now red, your change broke
it: fix it. On the first run in a freshly generated project, read the XML and
the build dependencies before deciding whether your change caused the failure.

### When an application from this checkout is already running

- Before rebuilding, check whether it holds this checkout's output jars open;
  replacing them can cause ZIP or resource read failures. Agree on a stop,
  rebuild, and restart with its owner, or build into separate outputs.
- For Gate 3, use its base URL and port as given, and confirm that it serves
  the code you are verifying. If it needs a restart, get the owner's approval;
  without it, report the browser check as unverified. Report that the gate did
  not start the application.

### When the running application shows older code than the tests

If a test passes but the running view cannot find a component or `@Subscribe`
target, compare the source, the built resource, and the resource the process
actually loads, including any override under a module's `.jmix/conf/`, before
you change code that looks correct. Delete `.jmix/conf/`. Archive read failures
after a rebuild need a restart, with the owner's approval if you did not start
the application. Then repeat the walk.

### When the deliverable includes a container image

A local upload does not test the container's file permissions; run the upload
check against the built image or the deployed application. For an image that
runs as a non-root `app` user with `/app` as its working directory, create the
Jmix temporary directory before switching to that user:

```dockerfile
RUN mkdir -p /app/.jmix/temp && chown -R app:app /app/.jmix
```
