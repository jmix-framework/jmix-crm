# `EntitySavingEvent` listener that needs a reference

Part of `jmix-add-entity-event-listener`. Read this when an `EntitySavingEvent` listener needs a reference.

Inside `EntitySavingEvent` an unloaded reference does not lazy-load: the getter throws. This differs from the ordinary rule (outside the save path a reference missing from the fetch plan is lazy-loaded and only local attributes throw), so code written from the general rule fails here.

This also rules out `dataManager.load(Category.class).id(order.getCategory().getId())`: `order.getCategory()` throws first. `entity.getRef().getId()` is not a safe way to learn a reference's id; it needs the reference loaded.

```java
@EventListener
public void onOrderSaving(EntitySavingEvent<Order> event) {
    Order order = event.getEntity();
    Category category = entityStates.isLoaded(order, "category")
            ? order.getCategory()
            : dataManager.load(Order.class).id(order.getId())
                    .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                            .add("category", FetchPlan.BASE))
                    .optional().map(Order::getCategory).orElse(null);
    // ... use category, keep working with `order`
}
```

Read the reference out of the separately loaded instance and keep using the event's entity. Do not continue with the reloaded copy and do not merge it back: it carries what is in the database and overwrites values set in memory but not yet saved.
