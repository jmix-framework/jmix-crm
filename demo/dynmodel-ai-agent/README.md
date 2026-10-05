# 🤖 Dynamic Model AI Agent — B2B CRM demo stands

🌐 Languages: [English](README.md) | [Русский](README_ru.md)

Describe the data you need in plain words — the agent drafts the entities, fields, screens and menu
items, you review its plan and publish the result with one click, without writing code or
redeploying the application. Dynamic Model is the Jmix Premium add-on that lets a running application
grow new entities and screens; the AI agent drives it from a chat.

This branch adds the agent to the B2B CRM and packages the application as a set of demo instances —
"stands". A business user opens **Admin → Dynamic model settings**, switches the editor to **AI** and
describes a change. The agent proposes a plan, and the user approves it. Approving the plan changes
only the model in the editor; nothing reaches the running application until the user presses
**Apply**. After that the new screens appear in the menu straight away, ready for data entry.

> **Preview.** The agent is not released yet. The branch builds against Jmix `3.1.999-SNAPSHOT` and
> Jmix Premium from the `50-dynmodel-ai-agent` branches, so building it requires read access to the
> Jmix Premium repository. Inside the Jmix team the stands can also be run from prebuilt jars.

## 📑 Table of Contents

- [What the demo shows](#-what-the-demo-shows)
- [Stands](#-stands)
- [Requirements](#-requirements)
- [Build](#-build)
- [Run and stop](#-run-and-stop)
- [Scenarios](#-scenarios)
- [Good to know](#-good-to-know)
- [What is in the branch](#-what-is-in-the-branch)

## ✨ What the demo shows

- **Plain language in, a reviewable plan out.** One request creates two related entities with
  their fields, list and detail screens and menu items. The plan lists every change before anything
  happens; hovering a field shows its caption in every language of the application.
- **The human stays in control.** The user can refine the plan in the same chat, approve it, review
  the result in the visual editor and publish it with **Apply** — or not publish it at all. The agent
  never publishes by itself, and the editor is locked while it works.
- **AI and manual editing work on the same model.** Add a field in the **Visual** editor, rename a
  caption in **Code**, then ask the agent — it sees the unpublished manual changes.
- **Small precise changes.** "Put the next-contact date right before the amount" becomes a plan with
  a single field move.
- **Real screens, real data.** The published entities are ready for data entry at once: required
  fields are enforced, references show names, dates and decimals keep their types.
- **Growing the model later.** A new conversation adds a reference book, a reference to it, a
  collection of participants and a menu reorder — and keeps the existing fields, screens and data.
- **Clear boundaries.** The agent refuses to read business data ("sum up all deals of a client") and
  refuses unsafe changes to a published model (changing a field type).
- **Full history.** **Admin → Agent history** shows every conversation with its plans, results and
  publications as read-only snapshots, and the execution trace of each run.

## 💻 Stands

The stands differ in appearance, so the agent can be shown — and checked — in each theme; `aura-dark`
can also run on Claude, and `aura-light-tabbed` opens views in tabs. Every stand has its own database,
so changes on one are not visible on another.

| Name | Port | Theme | Colour | Direction | Model |
|---|---|---|---|---|---|
| `aura-light` | 8091 | Aura | light | left to right | DeepSeek v4.1 Flash |
| `aura-dark` | 8092 | Aura | dark | left to right | Claude if `ANTHROPIC_API_KEY` is set, otherwise DeepSeek |
| `lumo-light` | 8093 | Lumo | light | left to right | DeepSeek |
| `lumo-dark` | 8094 | Lumo | dark | left to right | DeepSeek |
| `aura-rtl` | 8095 | Aura | light | right to left | DeepSeek |
| `aura-light-tabbed` | 8096 | Aura, views in tabs | light | left to right | DeepSeek |

Open `http://localhost:<port>/b2b-crm/` and log in as `admin` / `admin`.

## 🧰 Requirements

- JDK 21 or newer, in `JAVA_HOME` or on `PATH` (a JDK, not a JRE: `stop` runs `Shutdown.java` as a
  source file).
- An [OpenRouter](https://openrouter.ai) key in the `OPENROUTER_API_KEY` environment variable. The stands
  start without it, but every request to the agent then fails with a model error.
- Optionally `ANTHROPIC_API_KEY`: with it the `aura-dark` stand runs on `claude-sonnet-5`; set
  `STAND_CLAUDE_MODEL` to use another Claude model.
- Memory: each stand's heap is capped at 768 MB, so plan on about 1 GB of free memory per running stand.
- Free ports 8091–8096 (HTTP) and 9191–9196 (JMX). The stands listen on `127.0.0.1` only; do not
  expose these ports.
- Read access to Jmix Premium for the build.

## 🔨 Build

The branch takes every `io.jmix*` artifact — the BOM included — **only from the local Maven
repository** (see `build.gradle`), so both Jmix branches are published there first. Never mix them
with nightly builds. On Windows use `.\gradlew.bat` instead of `./gradlew`.

1. Clone the three repositories on their `50-dynmodel-ai-agent` branches:

   ```bash
   git clone -b 50-dynmodel-ai-agent https://github.com/jmix-framework/jmix.git
   git clone -b 50-dynmodel-ai-agent https://github.com/jmix-framework/jmix-premium.git   # needs access
   git clone -b 50-dynmodel-ai-agent https://github.com/jmix-framework/jmix-crm.git
   ```

2. In `jmix`:

   ```bash
   ./gradlew publishToMavenLocal -x test -x javadoc \
       :jmix-build:publishToMavenLocal :jmix-gradle-plugin:publishToMavenLocal \
       :jmix-translations:publishToMavenLocal :jmix-templates:publishToMavenLocal
   ```

3. In `jmix-premium`:

   ```bash
   ./gradlew publishToMavenLocal -x test -x javadoc
   ```

4. In `jmix-crm`: `./gradlew bootJar`. The stands run `build/libs/crm.jar`.

5. For the tabbed stand only, build the `50-dynmodel-ai-agent-tabbed` branch in a sibling worktree:

   ```bash
   git worktree add ../jmix-crm-tabbed 50-dynmodel-ai-agent-tabbed
   (cd ../jmix-crm-tabbed && ./gradlew bootJar)
   ```

   The stand scripts look for `../jmix-crm-tabbed/build/libs/crm.jar`; set `STAND_TABBED_JAR` (and
   `STAND_JAR` for the other five) to use jars from elsewhere. A missing jar skips its stand.

Stop running stands before rebuilding: on Windows a running jar cannot be overwritten.

## 🚀 Run and stop

From this folder (`demo/dynmodel-ai-agent`). macOS and Linux:

```bash
./stands.sh start                 # all six; the first start takes about a minute
./stands.sh status
./stands.sh start aura-rtl        # one stand
./stands.sh stop                  # all; or one: ./stands.sh stop lumo-dark
```

Windows, PowerShell:

```powershell
.\stands.ps1 start
.\stands.ps1 status
.\stands.ps1 stop
```

If PowerShell refuses to run the script: `powershell -ExecutionPolicy Bypass -File .\stands.ps1 start`.

**Stop the stands with `stop` only.** It asks the application to shut down and save its database. A
killed process (`kill -9`, Task Manager, a reboot) leaves a database that cannot be replayed: the stand
then hangs on start with "Waiting for changelog lock" and only a reset helps.

To **reset a stand**, stop it and delete its folder `instances/<name>`. The next start creates a fresh
database with the CRM demo data. The stand's log is `instances/<name>/application.log`.

## 🎬 Scenarios

| Scenario | For | What it covers |
|---|---|---|
| [Demo](scenarios/demo-scenario.md) | presenters, marketing | 14 steps: create entities with AI, refine and approve the plan, edit by hand, publish, enter data, extend the model in a new conversation, show the boundaries and the history |
| [Regression](scenarios/regression-scenario.md) | developers, QA | 35 browser checks of the agent and the editor: plan revisions, manual edits, cancellation, conflicts, invalid input, history, restart |
| [Stand checklist](scenarios/stand-checklist.md) | developers, QA | Checks per theme, plus tabbed mode on 8096: publication message, permissions, errors, history, layout |

The scenarios are written in Russian for the Russian UI: the prompts and the expected labels are
Russian. The agent also understands English and writes plan titles and summaries in the language of
the user's message.

## 💡 Good to know

- The agent never publishes by itself: after plan approval the changes are in the editor, and a person
  publishes them with **Apply**.
- The agent works with Dynamic Model entities. In this preview it cannot extend an existing application
  entity for the first time, create screens or menu items for one, or create enumeration and calculated
  fields.
- The stands raise the agent's limits in `application.properties` — up to 24 steps per plan and four
  minutes per request, with manual plan approval. The demo's plans are larger than the defaults allow.
- Ask for a field position ("put the date before the amount") as a separate request. Inside a large
  creation request it sometimes makes planning fail.
- In a long conversation the model can lose track — once, after 14 requests, it decided that an
  existing entity was missing. Start a new task with **Reset dialog**.
- DeepSeek Flash sometimes answers the same request differently. If an answer looks odd, repeat it.
- After a page reload the AI mode is not restored from the address; select it in the switch again.

## 🧩 What is in the branch

Compared with `main`:

- **The agent in the CRM** — the Dynamic Model, AI chat and agent starters, their Liquibase changelogs,
  the **Dynamic model settings** and **Agent history** menu items, and the agent's own model
  connection: an OpenAI-compatible chat model on OpenRouter (`DynamicModelAgentConfiguration`,
  `crm.dynmodel.*` properties), separate from the CRM's AI assistant. `DynamicModelAgentIntegrationTest`
  checks that the agent and the agent's Settings view are in place alongside the CRM.
- **The build** — the Jmix Gradle plugin and every `io.jmix*` artifact from the local Maven
  (`settings.gradle`, `build.gradle`), and Vaadin production mode with a production bundle rebuilt for
  Vaadin 25.3.
- **Stand switches** — theme, colour and direction from the system properties `stand.theme`,
  `stand.color`, `stand.direction` (`CRMApplication`), CRM styles for Lumo (`themes/lumo`), and a direct
  Claude connection for one stand (`StandAnthropicConfiguration`, `spring-ai-anthropic`).
- **Compatibility with Jmix 3.1** — small API adjustments, a route of its own for the main view so that
  closing a view returns to Home, and dynamic attributes initialised after application start.
- **The stand launcher** — `stands.sh`, `stands.ps1` and `Shutdown.java` (graceful stop over JMX).

The `50-dynmodel-ai-agent-tabbed` branch adds one commit on top: the Tabbed Mode add-on and a tabbed
main view.
