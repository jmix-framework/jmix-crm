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
        cleanup.forEach(dataManager::remove);
    }
}
```

Add the instance **returned** by `dataManager.save()` to the cleanup list — not the pre-save argument — so `remove` targets the persisted entity.

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

## End-To-End Tests

Use Masquerade/Selenide or the project's browser-test stack when browser verification is required. Enable test ids only in a test profile and prefer stable component ids over text selectors.

## Cleanup Audit

Before finishing, check:

- Every created persistent record is removed in `@AfterEach`, using the same authentication level needed for deletion.
- Test data has unique values to avoid collisions.
- Assertions verify persisted or visible behavior, not just absence of exceptions.
- The test command can run one class or method without running the full suite.
- No `@Transactional` on the test class or methods — it rolls back the writes that `@AfterEach` cleanup and the twice-back-to-back run depend on.
- Run the single class twice back-to-back — a second green run proves no leaked rows or cross-test data dependence that one pass hides.

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
