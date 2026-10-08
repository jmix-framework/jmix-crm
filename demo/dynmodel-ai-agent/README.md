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

> **Preview.** The agent — the Dynamic Model AI Builder,
> [jmix-framework/jmix#5779](https://github.com/jmix-framework/jmix/issues/5779) — is merged into Jmix
> `master` and ships in Jmix 3.1, which is not released yet. Until then the branch builds against Jmix
> and Jmix Premium `3.1.999-SNAPSHOT` from their `master` branches, so building it requires read access
> to the Jmix Premium repository. Inside the Jmix team the stands can also be run from prebuilt jars.

## 📑 Table of Contents

- [What the demo shows](#-what-the-demo-shows)
- [Stands](#-stands)
- [Requirements](#-requirements)
- [Build](#-build)
- [Run and stop](#-run-and-stop)
- [Scenarios](#-scenarios)
- [Good to know](#-good-to-know)
- [What is in the branch](#-what-is-in-the-branch)
- [The demo/ai-app branch](#-the-demoai-app-branch)

## ✨ What the demo shows

- **Plain language in, a reviewable plan out.** One request creates two related entities with
  their fields, list and detail screens and menu items. The plan lists every change before anything
  happens; hovering a field shows its caption in every language of the application.
- **The human stays in control.** The user can refine the plan in the same chat, approve it, review
  the result in the visual editor and publish it with **Apply** — or not publish it at all. The agent
  never publishes by itself, and the editor is locked while it works.
- **Every step is visible.** A progress bar shows the stage — Request, Plan, Prepare, Review, Apply —
  and the **Workspace** next to the chat shows the current plan or model changes and any issues.
  Replies link to read-only snapshots of the plan, the prepared and applied changes and the run steps.
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

## 💻 Stands

The stands differ in appearance, so the agent can be shown in each theme; `aura-dark`
can also run on Claude, and `aura-light-tabbed` opens views in tabs. Every stand has its own database,
so changes on one are not visible on another. Without `OPENROUTER_API_KEY` the stands that run the
`demo/ai-app` jar use OpenAI `gpt-5.4` instead of DeepSeek (see [OpenAI-only setup](#openai-only-setup)).
`aura-light-tabbed` runs the `50-dynmodel-ai-agent-tabbed` jar, which has only the OpenRouter connection,
so it always needs `OPENROUTER_API_KEY`.

| Name | Port | Theme | Colour | Direction | Model |
|---|---|---|---|---|---|
| `aura-light` | 8091 | Aura | light | left to right | DeepSeek v4.1 Flash (`gpt-5.4` in the OpenAI-only setup) |
| `aura-dark` | 8092 | Aura | dark | left to right | Claude if `ANTHROPIC_API_KEY` is set, otherwise DeepSeek (`gpt-5.4` in the OpenAI-only setup) |
| `lumo-light` | 8093 | Lumo | light | left to right | DeepSeek (`gpt-5.4` in the OpenAI-only setup) |
| `lumo-dark` | 8094 | Lumo | dark | left to right | DeepSeek (`gpt-5.4` in the OpenAI-only setup) |
| `aura-rtl` | 8095 | Aura | light | right to left | DeepSeek (`gpt-5.4` in the OpenAI-only setup) |
| `aura-light-tabbed` | 8096 | Aura, views in tabs | light | left to right | DeepSeek only, needs `OPENROUTER_API_KEY` |

Open `http://localhost:<port>/b2b-crm/` and log in as `admin` / `admin`.

## 🧰 Requirements

- JDK 21 or newer, in `JAVA_HOME` or on `PATH` (a JDK, not a JRE: `stop` runs `Shutdown.java` as a
  source file).
- A model key for the agent: an [OpenRouter](https://openrouter.ai) key in the `OPENROUTER_API_KEY`
  environment variable (DeepSeek v4.1 Flash), or only an OpenAI key in `SPRING_AI_OPENAI_APIKEY` (see
  [OpenAI-only setup](#openai-only-setup)). The stands start without a key, but every request to the
  agent then fails with a model error.
- Optionally `ANTHROPIC_API_KEY`: with it the `aura-dark` stand runs on `claude-sonnet-5`; set
  `STAND_CLAUDE_MODEL` to use another Claude model.
- Memory: each stand's heap is capped at 768 MB, so plan on about 1 GB of free memory per running stand.
- Free ports 8091–8096 (HTTP) and 9191–9196 (JMX). The stands listen on `127.0.0.1` only; do not
  expose these ports.
- Read access to Jmix Premium for the build.

### OpenAI-only setup

With only an OpenAI key, the one the CRM AI assistant of `demo/ai-app` already reads, the agent runs on
OpenAI too. `stands.sh` and `stands.ps1` switch every stand except `aura-light-tabbed` to OpenAI when
`OPENROUTER_API_KEY` is not set, or when `STAND_PROVIDER=openai`:

| Variable | Meaning |
|---|---|
| `SPRING_AI_OPENAI_APIKEY` | The OpenAI key. The script hands it to the agent as `DYNMODEL_API_KEY` through the environment, so it never appears on the `java` command line. |
| `STAND_OPENAI_MODEL` | The agent's model, `gpt-5.4` by default (the model the CRM AI assistant uses). |
| `STAND_PROVIDER=openai` | Use OpenAI even when `OPENROUTER_API_KEY` is set. |

```bash
export SPRING_AI_OPENAI_APIKEY=…    # once, for example in ~/.zshrc
STAND_JAR="$HOME/demo-jars/crm.jar" ./stands.sh start aura-light
# prints "aura-light: started … with OpenAI gpt-5.4"
```

`STAND_JAR` points at the jar built from `demo/ai-app` (see [The demo/ai-app branch](#-the-demoai-app-branch));
without it the script takes `build/libs/crm.jar`, which may be a build of another branch with no OpenAI
connection for the agent.

The agent then calls `https://api.openai.com/v1` through `crm.dynmodel.provider=openai`
(`DynamicModelAgentConfiguration`). The OpenRouter connection cannot be reused as is: api.openai.com
rejects its request fields (`Unknown parameter: 'provider'`) and, for GPT-5 models, `max_tokens`, so this
connection sends `max_completion_tokens` and no sampling parameters. Native structured output
(`jmix.dynmodel.ai.native-structured-output=true`) stays on: OpenAI accepts the agent's strict JSON
Schema. `aura-dark` still runs on Claude when `ANTHROPIC_API_KEY` is set.

Measured with `gpt-5.4` on 9 October 2026, the whole demo scenario on fresh `aura-light` and `aura-dark`
stands, the UI driven by a Playwright script. Model turns, from sending a request to the agent's answer:

| Turn | Runs | Seconds |
|---|---|---|
| Step 1: the clients and deals plan | 9 | 18–28 |
| Step 2: the plan extended | 7 | 22–26 |
| Step 3: plan approval until the draft is ready | 8 | 1–4 |
| Step 8: **Apply** until the changes are published | 8 | 1–5 |
| Step 10: the extension in a new conversation | 4 | 17–25 |
| Step 13: each of the two boundary questions | 14 | 3–5 |

Every plan was complete on the first attempt, both boundary questions were refused without a plan, and
no turn came near the four-minute limit. The full version (steps 1, 2, 3, 8, 9, 13) took 88–93 s of
script time, 51–55 s of it waiting for the model; the short version (steps 1, 3, 8, 9, 13) 61 s, 28 s of
it waiting for the model; the reserve steps 10–11 another 27–35 s. A presenter adds the talking on top.

## 🔨 Build

The branch builds against Jmix and Jmix Premium `3.1.999-SNAPSHOT`, which you publish yourself from
their `master` branches: `mavenLocal()` is the first repository in `settings.gradle` and `build.gradle`,
so whoever works on this branch publishes both repositories to the local Maven before building it.
Publish them completely — an open-source `io.jmix` module missing locally is quietly taken from the
Jmix nightly snapshot of the same version, which may differ from your checkout; a missing premium
module stops the build. Without the Jmix Gradle plugin in the local Maven the build stops at once with
"Plugin [id: 'io.jmix', version: '3.1.999-SNAPSHOT'] was not found". On Windows use `.\gradlew.bat`
instead of `./gradlew`.

1. Clone `jmix` and `jmix-premium` on `master`, and `jmix-crm` on `50-dynmodel-ai-agent`:

   ```bash
   git clone -b master https://github.com/jmix-framework/jmix.git
   git clone -b master https://github.com/jmix-framework/jmix-premium.git   # needs access
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

4. In `jmix-crm`: `./gradlew bootJar`. The stands run `build/libs/crm.jar`. Republish steps 2–3
   whenever you pull new commits of `jmix` or `jmix-premium`.

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
| [Demo](scenarios/demo-scenario.md) | presenters, marketing | 13 steps: create entities with AI, refine and approve the plan, edit by hand, publish, enter data, extend the model in a new conversation, show the boundaries |

The scenario is written in Russian for the Russian UI: the prompts and the expected labels are
Russian. The agent also understands English and writes plan titles and summaries in the language of
the user's message.

## 💡 Good to know

- The agent never publishes by itself: after plan approval the changes are in the editor, and a person
  publishes them with **Apply** (in AI mode it sits at the bottom of the Workspace).
- The agent works with Dynamic Model entities. In Jmix 3.1 it cannot extend an existing application
  entity for the first time, create screens or menu items for one, or create enumeration and calculated
  fields.
- The stands raise the agent's limits in `application.properties` — up to 24 steps per plan and four
  minutes per request. The demo's plans are larger than the defaults allow.
- Ask for a field position ("put the date before the amount") as a separate request. Inside a large
  creation request it sometimes makes planning fail.
- In a long conversation the model can lose track — once, after 14 requests, it decided that an
  existing entity was missing. Before a new task, clear the conversation with the eraser button
  (**Clear conversation**); unpublished model changes stay in the editor unless you choose to discard
  them in the confirmation.
- The model sometimes answers the same request differently: DeepSeek Flash in content, `gpt-5.4` in
  details such as captions in lower case («телефон» instead of «Телефон»). If an answer looks odd,
  repeat it.
- After a page reload the AI mode is not restored from the address; select it in the switch again.
- The agent records every conversation and run in the database, but there is no screen for them.
  Developers can look at the `dmagent_*` entities in the Entity Inspector at
  `/b2b-crm/datatl/entity-inspector`.

## 🧩 What is in the branch

Compared with `main`:

- **The agent in the CRM** — the Dynamic Model and AI chat starters with
  `io.jmix.dynmodel:jmix-dynmodel-ai-starter` and `jmix-dynmodel-ai-flowui-starter`, their Liquibase
  includes (the agent's is `/io/jmix/dynmodelai/liquibase/changelog.xml`), the **Dynamic model
  settings** menu item, the agent's limits (`jmix.dynmodel.ai.*` properties) and its own model
  connection: an OpenAI-compatible chat model on OpenRouter, or directly on OpenAI with
  `crm.dynmodel.provider=openai` (`DynamicModelAgentConfiguration`, `crm.dynmodel.*` properties),
  separate from the CRM's AI assistant. `DynamicModelAgentIntegrationTest`
  checks that the agent and its Settings view are in place alongside the CRM.
- **The build** — Jmix `3.1.999-SNAPSHOT` with the local Maven as the first repository for the Gradle
  plugin and the libraries (`settings.gradle`, `build.gradle`), and Vaadin production mode with a
  production bundle rebuilt for Vaadin 25.3.
- **Stand switches** — theme, colour and direction from the system properties `stand.theme`,
  `stand.color`, `stand.direction` (`CRMApplication`), CRM styles for Lumo (`themes/lumo`), and a direct
  Claude connection for one stand (`StandAnthropicConfiguration`, `spring-ai-anthropic`).
- **Compatibility with Jmix 3.1** — small API adjustments, a route of its own for the main view so that
  closing a view returns to Home, and dynamic attributes initialised after application start.
- **The stand launcher** — `stands.sh`, `stands.ps1` and `Shutdown.java` (graceful stop over JMX).

The `50-dynmodel-ai-agent-tabbed` branch follows this one and adds the Tabbed Mode add-on and a tabbed
main view.

## 🎤 The demo/ai-app branch

`demo/ai-app` extends this branch for the "AI × Jmix" talk: the CRM AI assistant, AI-generated JPQL in
reports and the Dynamic Model agent on the same stand.

- **Build** — steps 2–3 of [Build](#-build), then in `jmix-crm` on `demo/ai-app` (not on
  `50-dynmodel-ai-agent` from step 1):

  ```bash
  demo/dynmodel-ai-agent/stands.sh stop     # first: running stands read ~/demo-jars/crm.jar
  ./gradlew bootJar && mkdir -p ~/demo-jars
  cp build/libs/crm.jar ~/demo-jars/crm.jar.new && mv ~/demo-jars/crm.jar.new ~/demo-jars/crm.jar
  cd demo/dynmodel-ai-agent && STAND_JAR="$HOME/demo-jars/crm.jar" ./stands.sh start aura-light
  ```

  Stop the stands before replacing the jar and wait until `demo/dynmodel-ai-agent/stands.sh status` shows them `stopped`: a
  running stand loads classes from its jar on demand, and a jar overwritten under it breaks the stand.
  Copy to `crm.jar.new` and `mv` it over `crm.jar`: the rename is atomic, so nothing ever sees a
  half-written jar. The copy outside the repository keeps the stand's jar intact when the checkout
  switches branches or `build/` is cleaned. Pass `STAND_JAR` on every start (also for `aura-dark`):
  without it `stands.sh` runs `build/libs/crm.jar` of the repository.
- **JPQL in the log** — `logging.level.io.jmix.aitools.dataload=DEBUG`: the stand's
  `instances/<name>/application.log` shows the query the assistant wrote (`executeQuery(jpql=…)`) and the
  row-level conditions AI Tools added to it (`Access conditions applied`).
- **Personal data closed to the AI** — `Contact.phone` and `Contact.email` carry `@ExcludeFromAi` (AI
  Tools, Jmix 3.1). The CRM's own channels to the model honour it too: a client added to the chat context
  comes without them, and the Client 360 report run by the assistant leaves them out. In the application
  they stay visible.
- **Row-level rights in value queries** — the Manager role may read `User.id`, which the Only My Accounts
  conditions compare; without it a joined query by `alice` (AI Tools or an AI-generated JPQL band) is
  refused.
- **AI-generated JPQL reports** — `demo/reports/ai-jpql-reports.zip` holds «Выручка клиентов (AI JPQL)»
  (stored query, `fromDate` and `toDate` date parameters, a Table template) and «Выручка клиентов (живая
  генерация)» (the same band without a query, for **Generate query**). Reports live in the stand database:
  import the archive in **Administration → Reports → Reports → Import** after every stand reset. The stored
  query was written by hand in the format the generator stores. With `gpt-5.4` **Generate query** took
  6–7 s, and in six runs (three on each report) the model named the columns `orderCount` and `totalSum`
  instead of the template's `ordersCount` and `ordersTotal`; on the report with the stored query the editor
  then warns that the template can no longer print them. Save a regenerated query only when the columns
  stay the same. A repeated import updates the reports with the same ids and overwrites what was
  saved on the stand, so after saving a regenerated query export both reports, replace the archive and run
  `AiJpqlReportsArchiveTest`: it imports the archive and runs it as admin and as alice.
- **Users** — `admin` / `admin` sees all 30 clients, `alice` / `alice` (Manager + Only My Accounts) her 13.
