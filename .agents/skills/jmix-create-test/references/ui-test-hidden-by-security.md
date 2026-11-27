# UI test of columns or fields hidden by security

Part of `jmix-create-test`. Read this when a UI test checks columns or fields hidden by security.

A grid column whose attribute the current role may not view is removed from the Vaadin grid, but `DataGrid` still keeps it by key: `getColumnByKey("discount")` returns the column and its `isVisible()` is `true`, so a visibility assertion reports a hidden column as shown. Assert the removal through the column's element:

```java
Grid.Column<Order> column = grid.getColumnByKey("discount");
assertThat(column == null || column.getElement().getParent() == null).isTrue();
```

A form field bound to such an attribute is hidden with `setVisible(false)`, so `assertThat(field.isVisible()).isFalse()` is correct for fields.
