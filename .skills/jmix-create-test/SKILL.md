---
name: jmix-create-test
description: Create or update Jmix unit, integration, UI integration, or end-to-end tests for services, entity listeners, security behavior, views, fragments, and persistence workflows.
---

# Create Test

Use this skill when adding or changing tests for Jmix application behavior.

## Steps

1. Choose the smallest test type that proves the behavior.
2. Use a plain JUnit test for pure Java logic without Spring/Jmix services.
3. Use `@SpringBootTest` for services, DataManager persistence, entity listeners, security, and transactions.
4. Use `@UiTest` with `FlowuiTestAssistConfiguration` for Flow UI controller/component behavior without a browser.
5. Use end-to-end browser tests only for real browser behavior, routing, login, theme, or Vaadin client-side interactions.
6. Create test data through `DataManager.create()` and `DataManager.save()`.
7. Set authentication with the project's `AuthenticatedAsAdmin` extension or `SystemAuthenticator`.
8. Clean up created persistent data in `@AfterEach`.
9. Mock external systems at the boundary; prefer `@MockitoBean` on Spring Boot 3.4+ projects and follow the project's existing compiled pattern otherwise.
10. Run the smallest relevant Gradle test command.

Before typing any Jmix/Vaadin symbol you didn't copy from this project's `src` — a class, enum constant, action id, component, or event inner-class — confirm it. A guessed symbol survives typing and then breaks `compileJava`. Use the Context7 MCP (`/jmix-framework/jmix-context7`) when available, otherwise the official docs plus a working example already in this repo — see `jmix-verify-api-symbol`.

## Unit Test Pattern

```java
class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void appliesDiscount() {
        assertThat(calculator.applyDiscount(100, 10)).isEqualTo(90);
    }
}
```

## Integration Test Pattern

```java
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
class CustomerServiceTest {
    @Autowired
    private DataManager dataManager;

    @Autowired
    private CustomerService customerService;

    private final List<Object> cleanup = new ArrayList<>();

    @Test
    void findsCustomerByEmail() {
        Customer customer = dataManager.create(Customer.class);
        customer.setEmail("customer@test.com");
        cleanup.add(dataManager.save(customer));

        assertThat(customerService.findByEmail("customer@test.com")).isPresent();
    }

    @AfterEach
    void tearDown() {
        cleanup.forEach((entity) -> dataManager.load(Id.of(entity)).optional().ifPresent(dataManager::remove));
    }
}
```

When the code under test (a service method, an entity event listener) may call `dataManager.save()` on the registered entity a second time, the reference in the cleanup list goes stale — its version lags the database — and `remove` in `@AfterEach` throws an optimistic-lock exception. So use the id to reload-then-remove.

## Security Test Pattern

```java
Optional<Customer> result = systemAuthenticator.withUser(
        username,
        () -> customerService.findByEmail("customer@test.com")
);
```

Use this when the expected result depends on Jmix security policies.

## UI Integration Test Pattern

```java
@UiTest
@SpringBootTest(classes = {AppApplication.class, FlowuiTestAssistConfiguration.class})
class CustomerUiTest {
    @Autowired
    private ViewNavigators viewNavigators;

    @Test
    void opensCustomerList() {
        viewNavigators.view(UiTestUtils.getCurrentView(), CustomerListView.class).navigate();
        CustomerListView view = UiTestUtils.getCurrentView();
        DataGrid<Customer> grid = UiTestUtils.getComponent(view, "customersDataGrid");
        assertThat(grid).isNotNull();
    }
}
```

Use the project's helper for component lookup if it exists. Otherwise keep a local typed helper small and explicit. A `@UiTest` that navigates to a view will fail to find it if the view's id or the component id is wrong — the test, not just `compileJava`, is what catches that.

**List a nested `@TestConfiguration` in `classes` explicitly.** Do not rely on the
usual "a static `@TestConfiguration` inside the test class is picked up
automatically" behaviour once `@SpringBootTest(classes = {...})` names an explicit
set, as `@UiTest` requires — in practice the nested class's beans do not appear.
Name it yourself:

```java
@UiTest
@SpringBootTest(classes = {
        AppApplication.class,
        FlowuiTestAssistConfiguration.class,
        CustomerUiTest.MockApiConfig.class})   // ← nested config, listed EXPLICITLY
class CustomerUiTest {

    @TestConfiguration
    static class MockApiConfig { /* @Bean overrides */ }
}
```

Omitting it does not fail loudly: the beans simply never get defined, and the test
runs against the real collaborator (a live HTTP call, or a `NoSuchBeanDefinition`
far from the cause).

## Testing code that runs outside a user session

A scheduler / `@Async` / application event listener path has no authenticated user, and a test that
sets authentication up **hides** exactly the defect worth catching. Call the entry
point bare, and authenticate only the data setup:

```java
@Test
void generateDailyReportsRunsOnTheSchedulerThread() {
    systemAuthenticator.runWithSystem(() -> givenOrder());   // setup only
    // no authentication around the call — reproduces the scheduler thread
    reportService.generateDailyReports();
}
```

See `jmix-run-background-code` for the `@Authenticated` / `SystemAuthenticator`
rules on the production side.

## End-To-End Tests

Use Masquerade/Selenide or the project's browser-test stack when browser verification is required. Enable test ids only in a test profile and prefer stable component ids over text selectors.

## Cleanup Audit

Before finishing, check:

- Every created persistent record is removed in `@AfterEach`, using the same authentication level needed for deletion.
- Cleanup reloads by id then removes instead of removing the instance that can be stale.
- Test data has unique values to avoid collisions.
- Assertions verify persisted or visible behavior, not just absence of exceptions.
- The test command can run one class or method without running the full suite.
- No `@Transactional` on the test class or methods — it rolls back the writes that `@AfterEach` cleanup and the twice-back-to-back run depend on.
- Run the single class twice back-to-back — a second green run proves no leaked rows or cross-test data dependence that one pass hides.
- `@AfterEach` deletions run in FK-safe order: child rows (holding a `@ManyToOne`/`@JoinColumn` to another cleaned-up entity) before the parents they reference. Re-check this whenever a new FK is added to an entity already covered by an existing cleanup block — a previously-safe order can silently become unsafe and throw a constraint violation in teardown, which JUnit reports against the test that triggered it rather than the cleanup, so it first reads as a test bug rather than an ordering one.
- An in-memory datasource with a FIXED name (`jdbc:hsqldb:mem:<fixed>`) is shared for the whole JVM/test run across every `@SpringBootTest` context that resolves to it — it is NOT reset per class or per method (HSQLDB's `mem:` catalog is registered by name for the JVM's lifetime). So `@AfterEach` cleanup is mandatory even for a test that only reads, or a later test in the same run sees its leftover rows; and a test that calls the same method twice to prove idempotency must not assume a fresh precondition between the two calls — set up explicitly the precondition the code under test actually checks.

## Forbidden

- `@Transactional` on test classes/methods — it rolls back writes, so `@AfterEach` cleanup of committed entities and running the test twice back-to-back both stop working.
- `@MockBean` (deprecated) — use `@MockitoBean`.
- `new Entity()` or constructor-created Jmix entities in persistence tests.
- Tests that depend on data left by previous tests.
- Cleanup only at the end of the test method.
- UI tests that assert only that navigation did not throw.
- Browser tests for behavior that `@UiTest` or service tests can prove.
- Hardcoded sleeps when framework waits or component assertions are available.
- `@WithUserDetails` for Jmix security tests when `SystemAuthenticator` or the project auth extension is available.
- A nested `@TestConfiguration` left out of `@SpringBootTest(classes = {...})` — its beans are silently never defined.
- `spring.main.allow-bean-definition-overriding=true` to replace a bean for one test — use a distinct `@Primary` bean instead.
- Authenticating before calling a scheduler/`@Async` entry point and then claiming that path is covered.
