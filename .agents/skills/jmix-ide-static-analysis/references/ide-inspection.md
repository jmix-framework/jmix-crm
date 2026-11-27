# IDE inspection

Part of `jmix-ide-static-analysis`. Read this when a semantic inspection tool is connected.

It is the only static check for descriptor defects (unresolved `msg://` keys,
invalid property paths, missing data containers, broken bindings), and it also
reports the Java errors a compile does.

**Choose the project.** Read the connected tool's schema first. When a call
supports `projectPath`, pass the absolute root of the intended open project on
every call. Use the server's documented project-discovery mechanism if needed;
do not assume that leaving the argument out returns a list. Some servers have no
project argument: do not invent one. Confirm the intended project with a known
diagnostic (below), or report the inspection unavailable.

**The project must be open as a standalone Gradle project** (its own root, not
a subdirectory of another open project). A nested path resolves to the wrong
module and returns generic noise (for example "URI is not registered"), not
Jmix findings.

**Include warnings.** Jmix-plugin findings (unresolved `msg://`, bad fetch or
entity references, broken bindings) usually come as warnings, so an errors-only
view looks clean when it is not. Treat Jmix-inspection and
unresolved-reference warnings as blockers.

**Confirm an empty result.** An inspection that silently targeted the wrong
project or module returns "no problems" for a file it never looked at.

- Require a result for every requested path; a batch omission or timeout means
  unverified, not clean. Prefer individual calls for descriptors.
- Check a file with a known diagnostic in the intended project. If needed,
  inspect a disposable copy of a descriptor with a deliberately invalid
  `msg://` key, placed in an IDE-indexed resource directory; confirm the defect
  is reported, then remove only that probe. Leave the original unchanged.
- In an abstract descriptor with no resolvable data container, a clean
  property-path check does not prove the bindings are valid; inspect or test
  the concrete view that inherits it too.

**When the inspection is unavailable for this run.** Do not call the file
clean; fall back to `compileJava` plus the mechanical descriptor checks, and
record the file as compile-only. This applies when:

- it returns "URI is not registered" or only generic, non-Jmix findings on a
  file you know has a descriptor (a `*-view.xml` you just edited);
- it reports "Cannot resolve symbol" for almost every symbol, including JDK
  types such as `String`, `List`, `Map`. This means the IDE does not index the
  file; the result is wrong, not the file. These findings have severity ERROR,
  so an errors-only filter does not remove them; do not fix code from them and
  ignore the whole result. For an XML-only edit, the same sign is "message not
  found" on `msg://` keys that grep does find in the module's
  `messages_*.properties`.

The most common cause of the second case is a git worktree nested under the
project root (agent tools create these often) while the IDE has the main
checkout open: the file is inside the project directory, so the inspection
accepts the path and returns wrong findings instead of an error. Check before
you trust any file type: `git rev-parse --show-toplevel`, run in the edited
file's directory, must equal the confirmed IDE project root (`projectPath` when
supported). If it differs, the file is in another worktree and the inspection
of that path does not count. The same sign also comes from a file outside any
source root, a Gradle project that was never imported, or indexing lag right
after files were written from outside the IDE.

**Findings to fix.** These are true positives. `compileJava`, the mechanical
checks, and a green `test` do not catch them:

- **Unresolved `msg://` key** — renders as the literal key at runtime.
- **Invalid property path or missing data container** in a `*-view.xml`.
- **Unfetched-attribute risk** on a property a view reads but the fetch plan
  omits (see `jmix-configure-fetch-plan`).
- **`Result of DataManager.save() is not used; use saveWithoutReload()`** —
  `save()` selects the entity again after persisting; when the caller discards
  the result, that reload is wasted work. See `jmix-create-service`.
