---
name: jmix-create-test
description: Create or update Jmix unit, integration, UI integration, or end-to-end tests for services, entity listeners, security behavior, views, fragments, and persistence workflows.
---

# Create Test

## Steps

1. Choose the smallest test type that proves the behavior.
2. Use a plain JUnit test for pure Java logic without Spring/Jmix services.
3. Use `@SpringBootTest` for services, DataManager persistence, entity listeners, security, and transactions.
4. Use `@UiTest` with `FlowuiTestAssistConfiguration` for Flow UI controller/component behavior without a browser. This includes the minimal test that opens each created or changed view for Gate 3 of `jmix-verify-bootrun`; write it even when the view is also walked in a browser. First read `references/ui-test.md` in this skill's folder. If the test checks grid columns or form fields hidden by security, also read `references/ui-test-hidden-by-security.md` in this skill's folder.
5. Use end-to-end browser tests only for real browser behavior, routing, login, theme, or Vaadin client-side interactions. Read **When the behavior needs a real browser** below.
6. Create test data through `DataManager.create()` and `DataManager.save()`.
7. Set authentication with the project's `AuthenticatedAsAdmin` extension or `SystemAuthenticator`. If the code under test runs on a scheduler, `@Async`, or application event listener path, first read `references/outside-user-session.md` in this skill's folder.
8. Clean up created persistent data in `@AfterEach`. If an entity has `@DeletedDate`/`@DeletedBy`, read **When the test removes a soft-deletable entity** below.
9. Mock external systems at the boundary; prefer `@MockitoBean` on Spring Boot 3.4+ projects and follow the project's existing compiled pattern otherwise.
10. Run the smallest relevant Gradle test command.
11. Before finishing, go through **Final review** below.

Before typing any Jmix/Vaadin symbol you didn't copy from this project's `src` — a class, enum constant, action id, component, or event inner-class — verify it with `jmix-verify-api-symbol`.

## Unit test pattern

```java
class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void appliesDiscount() {
        assertThat(calculator.applyDiscount(100, 10)).isEqualTo(90);
    }
}
```

Do not create an unregistered anonymous or local subclass of a Jmix entity for an in-memory fixture. The class is still on the test classpath: Jmix metadata discovery in another test can find it and fail with a missing metaclass, even when the fixture's own unit test passes.

- To test a missing-handler branch, vary the injected registry or collaborator list and use an existing entity type.
- If a new subtype is really needed, register it through the supported test configuration and run the full suite with a fresh context. Running only the fixture's unit test does not check metamodel discovery.

## Integration test pattern

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

Cleanup reloads by id and then removes. The code under test (a service method, an entity event listener) may save the entity again, so the instance in the cleanup list can have an old version, and removing it throws an optimistic-lock exception.

## Security test pattern

```java
Optional<Customer> result = systemAuthenticator.withUser(
        username,
        () -> customerService.findByEmail("customer@test.com")
);
```

Use this when the expected result depends on Jmix security policies.

Test row-level rules by loading data as the user and asserting which rows come back. A permission API such as `SecureOperations` proves only that a policy exists.

If the test checks grid columns or form fields hidden by attribute permissions, first read `references/ui-test-hidden-by-security.md` in this skill's folder.

## Final review

Before finishing, check:

- Every created persistent record is removed in `@AfterEach`, using the authentication level needed for deletion.
- Cleanup reloads by id and then removes, instead of removing an instance that can be stale.
- `@AfterEach` deletes child rows (with a `@ManyToOne`/`@JoinColumn` to another cleaned-up entity) before the parents they reference. Check the order again when a new FK is added to an entity that existing cleanup already covers. A wrong order throws a constraint violation in teardown, and JUnit reports it against the test, so it looks like a test bug.
- If an entity has `@DeletedDate`/`@DeletedBy`, read **When the test removes a soft-deletable entity** below.
- No `@Transactional` on the test class or methods: it rolls back the writes that `@AfterEach` cleanup and the second back-to-back run depend on.
- An in-memory datasource with a fixed name (`jdbc:hsqldb:mem:<fixed>`) is shared by every `@SpringBootTest` context in the JVM that uses it. It is not reset per class or per method (HSQLDB registers a `mem:` catalog by name for the JVM's lifetime). So:
  - `@AfterEach` cleanup is required even for a test that only reads, or a later test in the same run sees its rows.
  - A test that calls a method twice to prove idempotency must not expect a fresh precondition between the calls. Set up explicitly the precondition that the code under test checks.
- Test data has unique values to avoid collisions.
- Assertions verify persisted or visible behavior, not only the absence of exceptions.
- A test that compares a stored date-time with its in-memory original (equality, "same event", `isEqual`) creates the value at the column precision, for example `OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS)` for a PostgreSQL `timestamp`. `now()` has nanoseconds on Linux JDKs and microseconds on macOS, and the driver rounds the value to the column on write, so the reloaded value differs about half the time: green on a developer Mac, red on Linux CI.
- The test command can run one class or method without running the full suite.
- Run the single class twice back-to-back. A second green run shows there are no leftover rows and no dependence on data from other tests.
- For a UI test: it runs as a real database user when the behavior depends on the current user, and it selects the `tabSheet` tab before `click()` on a component in that tab (see `references/ui-test.md` in this skill's folder).

## Forbidden

- `@Transactional` on test classes/methods — it rolls back writes, so `@AfterEach` cleanup of committed entities and running the test twice back-to-back both stop working.
- `@MockBean` (deprecated since Spring Boot 3.4) in a project where `@MockitoBean` is available.
- `new Entity()` or constructor-created Jmix entities in persistence tests.
- An unregistered anonymous or local subclass of a Jmix entity as a test fixture.
- Removing the saved instance in cleanup instead of reloading it by id.
- Tests that depend on data left by previous tests.
- Cleanup only at the end of the test method.
- Deleting parents before the child rows that reference them in `@AfterEach`.
- `dataManager.remove` as the only cleanup of a soft-deletable entity.
- Test data without unique values.
- A test that asserts only that nothing was thrown.
- An idempotency test that expects a fresh precondition between its two calls on a shared fixed-name HSQLDB `mem:` datasource.
- Comparing a reloaded date-time with an original from `now()` that is not truncated to the column precision.
- Checking row-level rules only through a permission API such as `SecureOperations`.
- `spring.main.allow-bean-definition-overriding=true` to replace a bean for one test — use a distinct `@Primary` bean instead.
- `@WithUserDetails` for Jmix security tests when `SystemAuthenticator` or the project auth extension is available.
- Authenticating before calling a scheduler/`@Async` entry point and then claiming that path is covered.
- UI tests that assert only that navigation did not throw.
- Finding UI components by localized text, or comparing a caption with `messages.getMessage(key)` without resolving it in the component's locale or setting the UI locale in the test.
- Leaving `@UiTest` as the system user when the behavior depends on resolving the current user from the database.
- A nested `@TestConfiguration` left out of `@SpringBootTest(classes = {...})` — its beans are silently never defined.
- Looking for dialogs or notifications by walking `UI.getCurrent().getChildren()`.
- `UiTestUtils.getComponent(view, id)` for a component inside a fragment or a `dropdownButton` item.
- Calling `click()` on a component in a non-selected `tabSheet` tab and treating the silent no-op as evidence about browser behavior.
- Asserting `isVisible()` on a grid column hidden by security.
- Browser tests for behavior that `@UiTest` or service tests can prove.
- Hardcoded sleeps when framework waits or component assertions are available.

## Special cases

### When the test removes a soft-deletable entity

An entity with `@DeletedDate`/`@DeletedBy` is not removed by `dataManager.remove`: the row is only marked, stays in the store for every later run, and is visible to any query with soft deletion off. Teardown that must leave the store clean removes it hard:

```java
dataManager.save(new SaveContext()
        .setHint(PersistenceHints.SOFT_DELETION, false)
        .removing(entity));
```

### When the behavior needs a real browser

Use Masquerade/Selenide or the project's browser-test stack. Enable test ids only in a test profile, and prefer stable component ids over text selectors.
