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
2. If `jmix` is not installed, read **When `jmix` is not installed** below.
   Check `jmix --no-update --help` and `jmix --no-update new --help` for the
   installed CLI's options.
3. The CLI bundles its runtime, but building a Jmix 3 project needs a separate
   **JDK 21 or 25**. Check `java -version` and use the compatible JDK for Gradle.
4. Use an absent or empty target directory. `--path` is the final project
   directory, not its parent; the default is `./<project-name>`.

## Generate

Use `new` with `--non-interactive` so the CLI does not wait for terminal input.
Replace the example name, package, version placeholder, and path with the selected
values. This single-line command also works in PowerShell with a Windows path:

```shell
jmix --no-update new demo --non-interactive --jmix-version "<exact-3.x-version>" --template application --package com.company.demo --path "/absolute/path/demo"
```

- `--template application` generates Java; use `--template application-kotlin`
  for Kotlin. Other template IDs depend on the selected Jmix version; take them
  from the CLI's reported choices.
- `--locales en,de` when multiple locales are requested; the first is the
  default. Without it the CLI uses `en`.
- `--addons quartz,...` only for requested add-ons supported by that version and
  template. Add-on installation needs a compatible JDK. Commercial add-ons also
  need subscription credentials configured for Gradle before generation; follow
  the [CLI add-on documentation](https://github.com/jmix-framework/jmix-cli/blob/main/docs/features/add-ons.md#commercial-add-ons).
- `--project-id` only when needed for the requested naming prefix or template; at
  most seven characters. Jmix 3 application templates default to Aura.
- The CLI initializes Git and stages generated files when Git is available. Use
  `--no-git` when creating inside an existing repository or when Git is unwanted.
- The CLI installs Agent Toolkit guidelines and local skills by default. Use
  `--no-agents-toolkit` only when that setup is unwanted.

## Verify and continue

1. Inspect the generated Gradle files, source package, and application properties
   to confirm the requested Jmix version, language, package, and location.
2. Check generation warnings. Toolkit setup can fail without failing project
   generation; when enabled, verify that `AGENTS.md` and `.agents/skills` exist
   before reporting toolkit installation as successful.
3. In the generated directory, run `./gradlew test` on macOS/Linux or
   `.\gradlew.bat test` on Windows with the compatible JDK. Report dependency or
   environment failures separately from successful project generation.
4. Before adding entities, views, or other application code, read `jmix` and its
   matching artifact skills. Use `jmix-verify-bootrun` for context and render
   verification when continuing application development.

## Forbidden

- A target directory that already has files.
- `--force` to bypass a non-empty-directory error: it can overwrite user files.
- A JDK other than 21 or 25 for Gradle.
- A prerelease version the task did not ask for.
- `--jmix-version 3`: it is not a major-version selector.
- Omitting `--jmix-version`: it picks the latest stable version across majors,
  which may differ from the requested platform version.
- `new` without `--non-interactive`.
- The parent directory as `--path`.
- An invented template ID or template list command.
- Add-ons that were not requested or that the version and template do not
  support; commercial add-ons without subscription credentials for Gradle.
- A `--project-id` longer than seven characters.
- Creating the project inside an existing repository without `--no-git`.
- `--no-agents-toolkit` only because of non-interactive mode; non-interactive
  mode does not require skipping it.
- Reporting toolkit installation as successful without checking `AGENTS.md` and
  `.agents/skills`.
- Reporting dependency or environment failures as project generation failures.

## Special cases

### When `jmix` is not installed

Install it using the official installer with the automatic wizard launch
disabled, then add its bin directory to the current shell's PATH:

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
