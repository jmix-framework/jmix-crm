# Listener that handles `Type.DELETED`

Part of `jmix-add-entity-event-listener`. Read this when the listener handles `Type.DELETED`.

For `Type.DELETED`, `event.getChanges()` holds every non-read-only property of the entity with its last value before the delete — unlike `Type.UPDATED`, where only changed attributes appear. So `getOldValue(name)` works for scalar (non-reference) attributes too, and references come back as ids:

```java
@EventListener
public void onOrderLineChanged(EntityChangedEvent<OrderLine> event) {
    if (event.getType() == EntityChangedEvent.Type.DELETED) {
        LocalDate postingDate = event.getChanges().getOldValue("postingDate"); // scalar pre-delete value
        Id<Order> orderId = event.getChanges().getOldReferenceId("order");     // reference as id
        // validate against the pre-delete state, e.g. reject the delete
    }
}
```

The change set covers only the deleted entity's own attributes. A reference is stored as `Id.of(value)` only, so the referenced entity's columns cannot be read through it.
