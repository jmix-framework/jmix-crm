# Master grid selection listener

Part of `jmix-add-dialog-detail-flow`. Read this when you install a master grid selection listener.

Use a listener signature that the component accepts. A no-argument installed selection listener is invalid.

```java
import com.vaadin.flow.data.selection.SelectionEvent;

@Install(to = "ordersDataGrid", subject = "selectionListener")
private void ordersDataGridSelectionListener(SelectionEvent<DataGrid<Order>, Order> event) {
    Order order = ordersDataGrid.getSingleSelectedItem();
    if (order == null) {
        orderLinesDc.getMutableItems().clear();
        return;
    }

    orderLinesDl.setParameter("order", order);
    orderLinesDl.load();
}
```

If the project uses `@Subscribe("ordersDataGrid")` for selection changes, copy that existing compiled pattern instead of inventing a new listener form.
