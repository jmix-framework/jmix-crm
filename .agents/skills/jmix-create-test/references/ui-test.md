# UI integration tests (`@UiTest`)

Part of `jmix-create-test`. Read this when you write a UI integration test (`@UiTest`).

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

- Use the project's helper for component lookup if it exists. Otherwise keep a local typed helper small and explicit.
- A wrong view id or component id is caught by the test that navigates to the view, not by `compileJava`.
- `UiTestUtils.getCurrentView()` works as the navigation origin before the test navigates anywhere: `@UiTest` opens the initial view before each test.

**Find components by id, not by localized text.** In a `@UiTest` the UI and session locale is the JVM default (`Locale.getDefault()`). `Messages` takes the locale from `CurrentAuthentication`, which has no client locale there and falls back to the first entry of `jmix.core.available-locales`. Comparing a caption with `messages.getMessage(key)` passes only where the two agree: on a developer machine, not on an `en_US` CI runner. When the test must assert localized text, resolve it with the locale the component used, or set the UI locale in the test setup.

**Authenticate as a database user when behavior depends on the current user.** By default `@UiTest` runs as the system user, which is created at startup and has no row in the application's user table. Code that resolves the current user through `DataManager` or the user repository finds no entity and can silently take a "not the current user" branch. Wrap navigation, interaction, and assertions in `runWithUser`:

```java
systemAuthenticator.runWithUser("admin", () -> {
    viewNavigators.detailView(UiTestUtils.getCurrentView(), Order.class)
            .editEntity(order)
            .navigate();
    OrderDetailView view = UiTestUtils.getCurrentView();
    JmixButton approveButton = UiTestUtils.getComponent(view, "approveButton");
    assertThat(approveButton.isVisible()).isTrue();
});
```

- Use `withUser` instead when the block returns a value.
- This is separate from testing security policies: authenticate whenever the result depends on which application user looks at the data.
- `runWithUser` in the test body does not cover code that runs before the test: `@UiTest` opens the initial/main view first, as the system user. When that view resolves the current user, or every test in the class needs the same real user, implement `UiTestAuthenticator` and pass it in `@UiTest(authenticator = ...)` so the whole class, including the initial view, runs as that user.

**List a nested `@TestConfiguration` in `classes` explicitly.** Once `@SpringBootTest(classes = {...})` names an explicit set, as `@UiTest` requires, a static nested `@TestConfiguration` is in practice not picked up automatically:

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

If you leave it out, nothing fails at once: the beans are never defined, and the test runs against the real collaborator (a live HTTP call, or a `NoSuchBeanDefinition` far from the cause).

**Dialogs and notifications.** `UiTestUtils.getComponent(...)` finds only components in the view. Dialogs and notifications are not children of the `UI`: walking `UI.getCurrent().getChildren()` compiles, runs, and finds nothing (the UI's only child is the main view), and the failure looks like "the dialog never opened", which blames correct view code.

```java
DialogInfo dialog = UiTestUtils.getLastOpenedDialog();
assertThat(dialog.getText()).contains("...");
dialog.getButtons().stream()
        .filter(b -> "YES".equals(b.getText()))
        .findFirst().orElseThrow()
        .click();
```

`UiTestUtils` also has `getOpenedDialogs()`, `getLastOpenedNotification()`, `getOpenedNotifications()` and `validateView(detailView)`. Assert dialogs, notifications and validation errors through these, never by walking the tree.

These helpers read static registries (`OpenedDialogs`, `OpenedNotifications`). `JmixUiTestExtension` empties them after each test, but during a test they still hold what was opened before the action under test, by the initial view or by setup steps. Before asserting "no dialog opened" or on the last notification, clear them right before the action:

```java
openedDialogs.closeOpenedDialogs();             // @Autowired OpenedDialogs
openedNotifications.closeOpenedNotifications(); // @Autowired OpenedNotifications
approveAction.actionPerform(ordersDataGrid);
assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
```

**Data containers.** Get them through `ViewControllerUtils`, because `View.getViewData()` is protected and the compiler error does not point to the cause:

```java
CollectionContainer<Category> dc =
        ViewControllerUtils.getViewData(view).getContainer("categoriesDc");
assertThat(dc.getItems()).allMatch(Category::isApplicable);
```

**Inactive `tabSheet` tabs.** Lookup and `@ViewComponent` injection can find a component on a non-selected tab, but `JmixTabSheet` disables the content of every non-selected tab (and attaches it to the component tree only when the tab is first selected). `Button.click()` checks `isEnabled()` first, so on that content it does nothing and does not call the handler. This applies only to inactive tab content; a component that is only detached (for example built in a controller and not yet added) clicks normally. Select the tab first:

```java
JmixTabSheet tabSheet = UiTestUtils.getComponent(view, "orderTabSheet");
JmixButton approveButton = UiTestUtils.getComponent(view, "approveButton");

tabSheet.setSelectedIndex(1); // enable the tab content that contains approveButton
approveButton.click();
```

If the test targets only the server-side handler, fire the event directly and state why you bypass the disabled component:

```java
// The button is on an inactive tab; invoke the server-side handler directly.
ComponentUtil.fireEvent(approveButton, new ClickEvent<>(approveButton));
```

Firing the event directly does not prove that tab selection and the browser click work. A `click()` that does nothing on disabled content does not prove that the button is broken in a browser either. Use a browser test when that client-side path is the behavior under test.

**Components inside a fragment.** They are not found by id from the view. The fragment loader keeps a component's XML id as fragment-scoped data, not as the element id, so `UiTestUtils.getComponent(view, "ordersGrid")` and any walk comparing `Component.getId()` report "no component" for a correct descriptor. The fragment itself is a view component: get it from the view, then ask the fragment for its content:

```java
OrderListFragment fragment = UiTestUtils.getComponent(view, "orderListFragment");
DataGrid<Order> grid = (DataGrid<Order>) FragmentUtils.getComponent(fragment, "ordersGrid");
```

`FragmentUtils.findComponent(fragment, id)` is the `Optional` form; both are in `io.jmix.flowui.fragment`.

**`dropdownButton` items.** They are not view components, so `UiTestUtils.getComponent(view, itemId)` does not find them. Get the button by its component id and ask it for the item:

```java
DropdownButton createButton = UiTestUtils.getComponent(view, "createButton");
assertThat(createButton.getItem("createFromTemplate")).isNull(); // null, not an exception

ActionItem blankItem = (ActionItem) createButton.getItem("createBlank");
blankItem.getAction().actionPerform(createButton);
```

`getItems()` lists the items and skips separators.
