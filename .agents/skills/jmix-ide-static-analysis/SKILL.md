---
name: jmix-ide-static-analysis
description: Gate 1. Use to check every Java file or view descriptor you created or changed — with an IDE inspection when one is connected, otherwise with compileJava and mechanical descriptor checks.
---

# Static analysis (Gate 1)

Every file you created or edited passes a static check before you move on.

## Steps

1. **Choose the path by the connected tools.** Look for a Jmix-aware IDE or
   semantic inspection tool (for example JetBrains `get_file_problems`). If it
   is listed by name only, load it with your tool search, then call it once on
   a changed file. It is not connected only if no such tool is listed or that
   call fails or finds no open project. If it is connected, it is your primary
   check: run it per file on every `.java` and especially every `*-view.xml`
   you touched. First read `references/ide-inspection.md` in this skill's
   folder. If not, or it turns out to be unavailable, compile (step 2) is your
   check for `.java`. Run steps 2 and 3 in both cases.
2. **Compile.** Run it in every case: it is cheap and is the precondition for
   Gate 2.
   ```bash
   ./gradlew --no-daemon compileJava
   ```
   In a composite project, use the task paths from **Gradle tasks in a
   composite project** in the `jmix` skill.
   It is authoritative for `.java`: unresolved symbols, wrong imports or
   packages, type mismatches, bad handler signatures. It does not read XML
   descriptors: an enum bound to `entityComboBox` instead of `comboBox`, an
   `itemsQuery` without `:searchString`, a `msg://` typo, or an action that
   opens a non-existent view id compiles clean and then throws at render time.
   A clean compile proves nothing about any `.xml`.
3. **Run the mechanical descriptor checks** (below) in every case. An
   inspection that works can still miss unknown components and bad
   attributes.
4. **Run the per-file loop** (below) on every file.
5. **Report each file.** In your completion report, give each file you
   created or changed its inspection result, or the reason the inspection could
   not run (quote the error). List the compile-only files and re-inspect them
   the next time a session has the inspection. A green compile misses
   every Jmix semantic finding (unresolved `msg://` keys, invalid property
   paths, missing data containers, unfetched attributes).

To verify a symbol before you type an unfamiliar API, see
`jmix-verify-api-symbol` — the cheapest and earliest defense against
invented names.

## Mechanical descriptor checks

Run them from the project directory. Each one maps to a defect that passes a
clean compile and even a green `test`. Run them in every case. When you
have neither an inspection nor a Gate 3 render walk, they are the only check for
these defects.

- The `find` checks are pass/fail: any output is a defect to fix.
- The `grep` checks only show candidates. A hit is not automatically a failure,
  but you must explain every hit: each `msg://` key must resolve in a
  `messages_*.properties`; each `= :` loader parameter must be bound or guarded;
  each required field must have the right default (see the comment below and
  **Required-field defaults** in `jmix-create-entity`).

```bash
# package line on every new .java (missing → view-registry / import breakage)
find src/main/java -name '*.java' | while read f; do head -1 "$f" | grep -q '^package ' || echo "MISSING package: $f"; done
# required fields: one whose default value the requirements define needs an
# entity-layer default (not InitEntityEvent); one the user enters needs none
grep -rn "nullable = false\|@NotNull" src/main/java --include='*.java'
# manual :param loaders must be bound (:container_* / :component_*) or guarded
grep -rn "= :" src/main/resources --include='*-view.xml'
# CREATE ⇒ MODIFY in every role; @MenuPolicy lists leaf item ids
grep -rn "CREATE\|MODIFY\|VIEW" src/main/java --include='*Role.java'
# itemsQuery must reference :searchString (or switch to itemsContainer)
grep -rn "itemsQuery" src/main/resources --include='*-view.xml'
# no raw Vaadin Dialog in a Jmix view
grep -rn "com.vaadin.flow.component.dialog.Dialog" src/main/java --include='*.java'
# every msg:// key must resolve in a messages_*.properties (else literal key renders)
grep -rhoE 'msg://[^"<> ]+' src/main/resources --include='*.xml' | sort -u
# no 0-byte source file (empty role drops policies; empty *-view.xml poisons registry)
find src/main -type f \( -name '*.java' -o -name '*.xml' \) -size 0
```

## Per-file loop

For each file you created or edited: inspection if connected (else compile) →
fix every error and every unresolved-reference or Jmix-inspection warning →
repeat until clean. Run it on every file, not a sample: the defects that
survive are the ones you were sure about and never checked. If the inspection
reports "is never used", "unused", "is not used", or "can be converted to a
local variable", first read `references/unused-findings.md` in this skill's folder before you change code. After the last edit, a full `compileJava` is the precondition for Gate 2
(`test`).

## What no static check covers

- Code paths that run only outside a user request — schedulers, `@Async`,
  message listeners. See `jmix-run-background-code`.

## Forbidden

- Reporting the inspection as not connected without the step 1 call.
- Calling a file clean from an empty inspection result you did not confirm, or
  from a batch omission or timeout.
- Reading only errors from the inspection: Jmix findings come as warnings.
- Calling a file clean when the inspection returned "URI is not registered" or
  only generic, non-Jmix findings on a `*-view.xml`.
- Fixing code from a result where "Cannot resolve symbol" covers almost every
  symbol, including JDK types — the IDE is not indexing the file.
- Inventing a project argument for an inspection tool that has none, or
  assuming that leaving the argument out returns a project list.
- Changing the original descriptor for a `msg://` probe, or leaving the probe
  copy behind.
- Treating a clean `compileJava` as proof about any `.xml`.
- Skipping the mechanical checks, even when an inspection is connected.
- Checking a sample of files instead of every file, or a completion report
  without a result per file.
- Discarding the result of `DataManager.save()` — use `saveWithoutReload()`.
- Setting the default of a required field whose default the requirements define
  in `InitEntityEvent` or only in a view, instead of at the entity layer.
- A `.java` file without a `package` line, or a 0-byte `.java` or `.xml`.
- `CREATE` without `MODIFY` in a role; a `@MenuPolicy` that does not list leaf
  item ids.
- A manual `= :` loader parameter that is neither bound (`:container_*` /
  `:component_*`) nor guarded.
- `itemsQuery` without `:searchString`; a raw Vaadin `Dialog` in a Jmix view.
- Changing code because of a finding listed in `references/unused-findings.md` in this skill's folder.
