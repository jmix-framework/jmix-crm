# New entity as a child of the selected master row

Part of `jmix-add-dialog-detail-flow`. Read this when the new entity is a child of the selected master row.

```java
@Autowired
private DialogWindows dialogWindows;

@ViewComponent
private DataGrid<Order> ordersDataGrid;

@ViewComponent
private CollectionLoader<Order> ordersDl;

@ViewComponent
private CollectionContainer<OrderLine> orderLinesDc;

@ViewComponent
private CollectionLoader<OrderLine> orderLinesDl;

@Subscribe("createLineButton")
public void onCreateLineButtonClick(final ClickEvent<JmixButton> event) {
    Order order = ordersDataGrid.getSingleSelectedItem();
    if (order == null) {
        return;
    }

    dialogWindows.detail(this, OrderLine.class)
            .newEntity()
            .withInitializer(line -> line.setOrder(order))
            .withAfterCloseListener(closeEvent -> {
                if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                    ordersDl.load();
                    orderLinesDl.load();
                }
            })
            .open();
}
```
