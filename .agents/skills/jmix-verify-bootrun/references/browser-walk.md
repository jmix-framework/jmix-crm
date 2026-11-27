# Gate 3 — walk views in a browser

Part of `jmix-verify-bootrun`. Read this before you walk a view with a browser tool.

**Actuator.** Readiness is checked on `/actuator/health`, and Jmix projects do
not include Actuator by default. Add it:

```groovy
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
```

```properties
# application.properties
management.endpoints.web.exposure.include=health
```

If you cannot add it, wait for `Started <App>Application in ...` in the log.

**Database.** A walk of a data-driven view usually has to create data, and
nothing removes it afterwards. The configured development database is often the
developer's own data. By default, use a separate temporary database:

- If HSQLDB is on the application's runtime classpath and the changelogs have
  no changesets for one database only (`dbms="..."`), use an in-memory
  database (`jdbc:hsqldb:mem:walk`). It needs no setup or cleanup and is gone
  when the application stops.
- Otherwise, create a new database on the project's database server, with a
  new name for each walk: a database kept from an earlier walk still holds its
  data.
- If you cannot create one, use the configured database only for a walk that
  creates nothing, or report the browser walk as not done.

These steps are for an application you start yourself.

1. Start the application with `bootRun` in the background, with its output in
   a log file. Point it at the temporary database by passing the datasource
   properties as application arguments, for example
   `--args="--main.datasource.url=<url>"`, plus the user name and password if
   they differ. A background start returns at once; before you start the
   application again, check its log and the port. A second start fails on the
   held port, and with a file database also on the database lock.
2. Wait until `/actuator/health` on the application port (8080 by default)
   answers, retrying for about two minutes. If it never becomes ready, read
   the log and stop the process. Then report the browser walk as not done.
3. Open each view you created, click each button and action, fill each field,
   and confirm there is no error overlay or server exception. When you created
   roles, log in through the login form as a user of each role, not only as
   `admin`: `admin` usually
   has `system-full-access`, which hides missing login, view, and menu policies.
   If the application accepts file uploads, upload a real file through the
   browser, confirm that the UI reports success, and confirm that the file can
   be opened or read back.
4. Stop the application (`kill` the PID; if the port stays held, kill the
   process that holds it). Drop a temporary database you created on a
   database server. Report which database the walk used and what data it
   created there.
5. If you edited code while the application was running, delete `.jmix/conf/`
   before the next Gate 2 run.

A Jmix UI is built from `vaadin-*` web components and does not behave like a
plain HTML page:

- Use only real input: clicks, typing, `Enter`. A value set by a script never
  reaches the server and can break the session; the application then returns to
  the login view with nothing in the browser console. Reading the DOM is fine.
- The first snapshot after navigation is often blank; retry until the content
  is there before reporting a render failure.
- Take a new snapshot before each interaction; references go stale after every
  action.
- Role/name locators often miss `vaadin-*` components; use a reference from the
  current snapshot, or a DOM query by tag name and visible text.
- A disabled action usually waits for a precondition; satisfy it before
  reporting a defect.
