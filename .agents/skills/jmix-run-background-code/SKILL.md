---
name: jmix-run-background-code
description: Use when writing code that runs outside a user request, such as a Quartz or scheduled job, an @Async method, a startup runner, or an integration event listener. Such code has no authenticated user.
---

# Code that runs outside a user session

`DataManager` and every other secured Jmix API need an authentication in the
Spring `SecurityContext`. A UI or REST request has one (the logged-in user).
These entry points have none:

- a Quartz `Job`, a Spring `@Scheduled` method
- an `@Async` method (it runs on another thread, and the security context is
  not propagated by default)
- an `ApplicationRunner` / `CommandLineRunner` / startup `@EventListener`
  (`ApplicationStartedEvent`, `ApplicationReadyEvent`)
- a message or integration listener (JMS, Kafka, an inbound webhook processed
  off the request thread)

Without an authentication, the first `DataManager` call throws:

```
IllegalStateException: Authentication is not set. Use SystemAuthenticator in
non-user requests like schedulers or asynchronous calls.
```

No check catches this. The code compiles, passes the Jmix inspection, and
passes a green `./gradlew test`, because the tests never
fire your scheduler. It fails the first time the job really runs, which in
production may be minutes after you report the task done.

## `@Authenticated` on the entry method (preferred)

`io.jmix.core.security.Authenticated` runs the annotated method under the
system user. Put it on the method that the background thread enters: the
Quartz job's `execute`, the `@Scheduled` method, the `@Async` method, or the
service method they call.

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

When the logic belongs in a service, annotate the service method and keep the
job a thin trigger:

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

`@Authenticated` is applied by a Spring proxy, so it works on any instance that
Spring builds. A Quartz `Job` qualifies: Spring Boot's Quartz auto-configuration
installs `SpringBeanJobFactory`, so the job gets its `@Autowired` fields and is
proxied, and `@Authenticated` on `execute` takes effect.

- If the method is called from the same instance (`this.method()`), or the
  object is created outside Spring (`new ReportJob()`, a custom Quartz
  `JobFactory`), first read `references/no-proxy.md` in this skill's folder.
- If the work cannot have its own proxied method, or must run as a specific
  user, first read `references/block-or-named-user.md` in this skill's folder.

## Verify: a test that enters the path

A green `test` proves nothing here unless a test calls the background
entry point. Write one that calls the method the way the scheduler does,
through the bean:

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

Test authentication (`AuthenticatedAsAdmin`, `SystemAuthenticator`) around this
call hides the defect, and the test then proves nothing about the scheduler
path. If the test needs data written first, wrap only the setup in
`systemAuthenticator.runWithSystem(...)` and leave the call under test bare.
See `jmix-create-test`.

## Forbidden

- `DataManager` (or any secured API) on a scheduler / `@Async` / startup /
  message-listener thread with no `@Authenticated` method and no
  `SystemAuthenticator` wrapper.
- `@Authenticated` on a method reached by a self-call, or on a class
  instantiated outside the Spring bean factory: the proxy is not involved, so
  the annotation does nothing.
- Reporting the task done on a green `test` when no test enters the
  background path.
- A test that authenticates before calling the background entry point and then
  claims that path is covered.
- A test that calls the entry method on an object it created with `new` instead
  of the Spring bean: it skips the proxy and proves nothing about
  `@Authenticated`.
