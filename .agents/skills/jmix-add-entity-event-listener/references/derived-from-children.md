# Stored value derived from composition children

Part of `jmix-add-entity-event-listener`. Read this when a stored value is derived from composition children.

A save writes only its save set: `DataContext` includes only changed instances, and `DataManager.save(x)` only what you pass. Changing the parent from a line's `EntitySavingEvent`, or changing lines and saving only the parent, writes nothing. So compute each stored value when its own entity is saved, and when a line is saved without its parent, reload the parent and save it.

Put the calculation in a Spring bean, not in the entity: it often needs other beans (prices, rates, settings), and the listeners, the view's live total (`jmix-create-composition-detail-view`) and tests then share one implementation.

```java
@EventListener
public void onOrderLineSaving(EntitySavingEvent<OrderLine> event) {
    event.getEntity().setAmount(orderTotals.lineAmount(event.getEntity()));
}

@EventListener
public void onOrderSaving(EntitySavingEvent<Order> event) {
    Order order = event.getEntity();
    if (entityStates.isLoaded(order, "lines")) {
        order.setTotal(orderTotals.total(order));   // sums lineAmount(line), not stored amounts
    }
}

@EventListener   // a line saved or removed without its order
public void onOrderLineChanged(EntityChangedEvent<OrderLine> event) {
    Id<Order> orderId = event.getType() == EntityChangedEvent.Type.DELETED
            ? event.getChanges().getOldReferenceId("order")
            : Id.of(dataManager.load(event.getEntityId()).fetchPlan(fp -> fp.add("order")).one().getOrder());
    dataManager.load(orderId)
            .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("lines", FetchPlan.BASE))
            .optional()
            .ifPresent(order -> {
                BigDecimal total = orderTotals.total(order);
                if (order.getTotal() == null || order.getTotal().compareTo(total) != 0) {
                    order.setTotal(total);
                    dataManager.saveWithoutReload(order);
                }
            });
}
```

Saving the order changes no line, so no further line event follows.
