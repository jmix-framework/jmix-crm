---
name: jmix-run-background-code
description: Use when writing code that runs outside a user request — a Quartz/scheduled job, an @Async method, an application-startup runner, or an integration event listener. Such threads have no authenticated user, so DataManager fails with "Authentication is not set" the first time the code actually runs. Compile, static analysis, and a green clean test all pass; only the real firing fails.
---

# Code that runs outside a user session

`DataManager` and every other secured Jmix API require an authentication in the
Spring `SecurityContext`. A normal UI or REST request has one — the logged-in
user. These entry points do NOT:

- a Quartz `Job`, a Spring `@Scheduled` method
- an `@Async` method (it runs on a different thread, and the security context is
  not propagated by default)
- an `ApplicationRunner` / `CommandLineRunner` / startup `@EventListener`
  (`ApplicationStartedEvent`, `ApplicationReadyEvent`)
- a message/integration listener (JMS, Kafka, an inbound webhook processed off
  the request thread)

Without an authentication, the first `DataManager` call throws:

```
IllegalStateException: Authentication is not set. Use SystemAuthenticator in
non-user requests like schedulers or asynchronous calls.
```

**No gate catches this.** It compiles, passes the Jmix inspection, and passes a
green `./gradlew clean test` — the context-load tests never fire your scheduler.
It fails the first time the job actually runs, which in production may be minutes
after you declared the task done.

## Pattern 1 — `@Authenticated` on the entry method (preferred)

`io.jmix.core.security.Authenticated` runs the annotated method under the system
user. Put it on the method that the background thread ENTERS — the Quartz job's
`execute`, the `@Scheduled` method, the `@Async` method, or the service method
they call:

```java
import io.jmix.core.security.Authenticated;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class ReportJob implements Job {

    @Autowired
    private DataManager dataManager;

    @Authenticated
    @Override
    public void execute(JobExecutionContext context) {
        List<Order> orders = dataManager.load(Order.class).all().list();
        // ...
    }
}
```

When the logic belongs in a service, annotate the service method instead and let
the job be a thin trigger:

```java
import io.jmix.core.security.Authenticated;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final DataManager dataManager;

    public ReportService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Runs under the system user, so it works from the scheduler thread,
     * which has no authenticated user.
     */
    @Authenticated
    public void generateDailyReports() {
        List<Order> orders = dataManager.load(Order.class)
                .query("e.status = 'NEW'")
                .list();
        // ...
    }
}
```

```java
public class ReportJob implements Job {

    @Autowired
    private ReportService reportService;

    @Override
    public void execute(JobExecutionContext context) {
        reportService.generateDailyReports();   // through the proxy → @Authenticated applies
    }
}
```

### Where the annotation applies — and where it does not

`@Authenticated` is applied by a Spring proxy, so it works wherever Spring builds
the instance. **A Quartz `Job` qualifies:** Spring Boot's Quartz auto-configuration
installs Spring's `SpringBeanJobFactory`. So the job gets its `@Autowired` fields AND
gets proxied, and `@Authenticated` directly on `execute` takes effect.

It is NOT applied when the proxy is out of the picture:

- A **self-call** inside one instance (`this.generateDailyReports()`) never passes
  through the proxy, so the annotation is ignored. The call must arrive from
  outside the instance.
- A job (or any object) built by code Spring does not own — `new ReportJob()`, or a
  custom Quartz `JobFactory` that instantiates the class directly instead of going
  through the bean factory. Then neither injection nor interception happens.

When in doubt, do not reason about it — run the path with no authentication set up
and see whether it throws (see "Verify it" below).

## Pattern 2 — `SystemAuthenticator` for a block

Use `io.jmix.core.security.SystemAuthenticator` when you cannot put the work in
its own proxied method, or when you need a specific user rather than the system
user:

```java
import io.jmix.core.security.SystemAuthenticator;

private final SystemAuthenticator authenticator;

public void onMessage(String payload) {
    authenticator.runWithSystem(() -> {
        // DataManager calls are authenticated here
    });
}
```

- `runWithSystem(Runnable)` / `withSystem(AuthenticatedOperation<T>)` — the system
  user, which bypasses security constraints. The `withSystem` variant returns a
  value; its argument is a lambda, same as `Runnable`.
- `runWithUser(String, Runnable)` / `withUser(String, AuthenticatedOperation<T>)` —
  a named user, so role policies apply as they would for that person.

Prefer `@Authenticated` for a whole entry point; use `SystemAuthenticator` for a
narrow block inside a larger method.

## Verify it — a test that actually enters the path

A green `clean test` proves nothing here unless a test calls the background entry
point. Write one that invokes the method the same way the scheduler does, through
the bean:

```java
@SpringBootTest
class ReportJobTest {

    @Autowired
    ReportService reportService;   // the proxied bean, like the job holds

    @Test
    void generateDailyReportsRunsWithoutAuthentication() {
        // NO @ExtendWith(AuthenticatedAsAdmin.class), NO authentication set up:
        // that is the point — this reproduces the scheduler thread.
        reportService.generateDailyReports();
    }
}
```

Setting up test authentication (`AuthenticatedAsAdmin`, `SystemAuthenticator`)
around this call HIDES the defect — the test then proves nothing about the
scheduler path. If the test needs data written first, wrap only the **setup** in
`systemAuthenticator.runWithSystem(...)` and leave the call under test bare. See
`jmix-create-test`.

## Forbidden

- `DataManager` (or any secured API) on a scheduler / `@Async` / startup /
  message-listener thread with no `@Authenticated` method and no
  `SystemAuthenticator` wrapper.
- `@Authenticated` on a method reached by a self-call, or on a class instantiated
  outside the Spring bean factory — the proxy is not involved, so the annotation
  does nothing.
- Declaring the task done on a green `clean test` when no test enters the
  background path.
- A test that authenticates before calling the background entry point and then
  claims that path is covered.
