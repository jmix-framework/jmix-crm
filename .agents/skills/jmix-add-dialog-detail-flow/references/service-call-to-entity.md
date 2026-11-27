# Service-call button that must create an entity

Part of `jmix-add-dialog-detail-flow`. Read this when a service-call button must create an entity instead.

Remove the input dialog; do not chain it before the detail dialog. A UI test that clicks the button expects a `StandardDetailView` at once, and casting an `InputDialog` to `StandardDetailView` throws `ClassCastException`.

WRONG (input dialog, then detail dialog):

```java
@Subscribe("actionButton")
public void onActionButtonClick(ClickEvent<JmixButton> event) {
    Order order = grid.getSingleSelectedItem();
    dialogs.createInputDialog(this)
            .withParameters(InputParameter.intParameter("quantity"))
            .withCloseListener(e -> {
                if (e.closedWith(DialogOutcome.OK)) {
                    Integer qty = e.getValue("quantity");
                    // then opens the detail dialog — too late, the test
                    // already saw the input dialog
                    openDetailView(order, qty);
                }
            })
            .open();
}
```

RIGHT (direct detail-dialog open):

```java
@Subscribe("actionButton")
public void onActionButtonClick(ClickEvent<JmixButton> event) {
    Order order = grid.getSingleSelectedItem();
    if (order == null) return;

    dialogWindows.detail(this, OrderLine.class)
            .newEntity()
            .withInitializer(line -> line.setOrder(order))
            .withAfterCloseListener(e -> {
                if (e.closedWith(StandardOutcome.SAVE)) {
                    ordersDl.load();   // reload the loader — NOT grid.getDataProvider().refreshAll()
                }
            })
            .open();
}
```

The detail view collects the scalar attributes, the entity is created on save, and a side effect (for example an `EntityChangedEvent` listener) performs any derived update. "Open the X detail view from the button" means no `InputDialog`, no `MessageDialog`, and no service call before the dialog opens.
