---
name: jmix-create-project
description: Create a new Jmix application with Jmix CLI using non-interactive project generation, an explicit platform version, and the requested package, template, and location.
---

# Create a Jmix Project with Jmix CLI

Use [Jmix CLI](https://github.com/jmix-framework/jmix-cli#readme) to generate the
initial project from official templates. For changes inside an existing
application, use the matching artifact skills in `jmix`.

## Prepare

1. Determine the project name, target directory, base package, Java or Kotlin,
   and Jmix version from the request. This toolkit branch targets **Jmix 3**.
   Use an exact published 3.x version; if none was specified, check the available
   versions in the official [template metadata](https://global.repo.jmix.io/repository/public/io/jmix/templates/studio/jmix-studio-templates/maven-metadata.xml).
   Prefer a stable release; use a prerelease only when the task calls for it.
   `--jmix-version 3` is not a major-version selector. Omitting the option picks
   the latest stable version across majors, which may differ from the requested
   platform version.
2. Check `jmix --no-update --help` and `jmix --no-update new --help` for the
   installed CLI's options.
   The CLI bundles its runtime; building a Jmix 3 project requires a separate
   **JDK 21 or 25**. Check `java -version` and use the compatible JDK for Gradle.
3. Use an absent or empty target directory. `--path` is the **final project
   directory**, not its parent; the default is `./<project-name>`. Do not add
   `--force` to bypass a non-empty-directory error: it can overwrite user files.

If `jmix` is missing, install it using the official installer with the automatic
wizard launch disabled, then add its bin directory to the current shell's PATH:

```bash
curl -fsSL https://github.com/jmix-framework/jmix-cli/releases/latest/download/install.sh | JMIX_CLI_NO_RUN=1 bash
export PATH="$HOME/.local/bin:$PATH"
```

```powershell
$env:JMIX_CLI_NO_RUN = '1'
irm https://github.com/jmix-framework/jmix-cli/releases/latest/download/install.ps1 | iex
Remove-Item Env:JMIX_CLI_NO_RUN
$env:PATH = "$env:LOCALAPPDATA\Jmix\bin;$env:PATH"
```

## Generate

Use `new` with **`--non-interactive`** so the CLI does not wait for terminal input.
Replace the example name, package, version placeholder, and path with the selected
values. This single-line command also works in PowerShell with a Windows path:

```shell
jmix --no-update new demo --non-interactive --jmix-version "<exact-3.x-version>" --template application --package com.company.demo --path "/absolute/path/demo"
```

- Use `--template application-kotlin` for a Kotlin application; `application`
  generates Java. Other template IDs depend on the selected Jmix version; check
  the CLI's reported choices instead of inventing a template ID or list command.
- Add `--locales en,de` when multiple locales are requested; the first is the
  default. Without this option the CLI uses `en`.
- Add `--addons quartz,...` only for requested add-ons supported by that version
  and template. Add-on installation requires a compatible JDK. Commercial add-ons
  also need subscription credentials configured for Gradle before generation;
  follow the [CLI add-on documentation](https://github.com/jmix-framework/jmix-cli/blob/main/docs/features/add-ons.md#commercial-add-ons).
- Use `--project-id` only when needed for the requested naming prefix or template;
  it is at most seven characters. Jmix 3 application templates default to Aura.
- The CLI initializes Git and stages generated files when Git is available. Use
  `--no-git` when creating inside an existing repository or when Git is unwanted.
- The CLI installs Agent Toolkit guidelines and local skills by default. Use
  `--no-agents-toolkit` only when that setup is unwanted; non-interactive mode
  does not require skipping it.

## Verify and continue

1. Inspect the generated Gradle files, source package, and application properties
   to confirm the requested Jmix version, language, package, and location.
2. Check generation warnings. Toolkit setup can fail without failing project
   generation; when enabled, verify `AGENTS.md` and `.agents/skills` exist before
   reporting toolkit installation as successful.
3. In the generated directory, run `./gradlew test` on macOS/Linux or
   `.\gradlew.bat test` on Windows with the compatible JDK. Report dependency or
   environment failures separately from successful project generation.
4. Before adding entities, views, or other application code, read `jmix` and its
   matching artifact skills. Use `jmix-verify-bootrun` for context and render
   verification when continuing application development.
