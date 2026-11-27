---
name: jmix-add-entity-event-listener
description: Add Jmix entity lifecycle event listeners to perform additional actions with saved or loaded entities.
---

# Add Entity Event Listener

Use this skill when business logic must react to entity save, change, delete, or load events.

## Steps

1. Create a Spring `@Component` listener in `service` or `listener`.
2. Use `org.springframework.context.event.EventListener`.
3. Use exact Jmix imports:
   - `io.jmix.core.event.EntityChangedEvent`
   - `io.jmix.core.event.EntityLoadingEvent`
   - `io.jmix.core.event.EntitySavingEvent`
4. For created entities, load by `event.getEntityId()` when related data is needed.
5. If you specify a custom fetch plan, include every scalar and reference property read later (see `jmix-configure-fetch-plan`).
6. For deleted entities, do not load `event.getEntityId()`; use old values or old reference ids from `event.getChanges()`. If the listener reacts to deletes, first read `references/deleted-events.md` in this skill's folder. If children removed by an `ON DELETE CASCADE` foreign key need per-row cleanup, first read `references/database-cascade.md` in this skill's folder.
7. Use `EntitySavingEvent` for defaults or transformations that must happen before data is saved; a required persistent default still belongs at the entity layer (see `jmix-create-entity`). If an `EntitySavingEvent` listener must read a reference of the entity, first read `references/saving-event-reference.md` in this skill's folder. If a stored value is computed from composition children (an order total from its lines), first read `references/derived-from-children.md` in this skill's folder.
8. Use `EntityLoadingEvent` for initializing non-persistent attributes from already loaded local persistent state.
9. Use a before-commit `@EventListener` path for validation that must reject the current save/remove operation. If the write being validated can also violate a database constraint, read **When the validated write can violate a database constraint** below.
10. Put multi-entity changes in a transactional service method when atomicity matters.
11. Reject unsupported updates/deletes inside the event path before treating work as complete.
12. Search the changed code for `@TransactionalEventListener`; if it sets required defaults, move them to an `EntitySavingEvent` `@EventListener`; if it performs validation or rejects updates/deletes, replace it with an `EntityChangedEvent` `@EventListener` or another before-commit path. Decide by the direction of failure (see **Event timing**).
13. Add tests or at least compile/startup validation for the event listener.

## Listener template

```java
import io.jmix.core.DataManager;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.event.EntityLoadingEvent;
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LedgerEntryEventListener {

    private final DataManager dataManager;
    private final LedgerService ledgerService;

    public LedgerEntryEventListener(DataManager dataManager,
                                    LedgerService ledgerService) {
        this.dataManager = dataManager;
        this.ledgerService = ledgerService;
    }

    @EventListener
    public void onLedgerEntryChanged(EntityChangedEvent<LedgerEntry> event) {
        if (event.getType() == EntityChangedEvent.Type.CREATED) {
            LedgerEntry entry = dataManager.load(event.getEntityId()).one();
            ledgerService.applyEntry(entry);
            return;
        }

        throw new UnsupportedOperationException("This record cannot be updated or deleted");
    }

    @EventListener
    public void onLedgerEntrySaving(EntitySavingEvent<LedgerEntry> event) {
        if (event.getEntity().getCreatedDate() == null) {
            event.getEntity().setCreatedDate(LocalDateTime.now());
        }
    }

    @EventListener
    public void onLedgerEntryLoading(EntityLoadingEvent<LedgerEntry> event) {
        LedgerEntry entry = event.getEntity();
        entry.setDisplayLabel(entry.getNumber() + " / " + entry.getType());
    }
}
```

## Fetch plan safety

For non-deleted events, the loaded entity must contain every property the listener reads. The safest default is loading by event id with the normal plan:

```java
LedgerEntry entry = dataManager.load(event.getEntityId()).one();
```

With a custom fetch plan, add all accessed scalar fields and references:

```java
LedgerEntry entry = dataManager.load(LedgerEntry.class)
        .id(event.getEntityId())
        .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE)
                .add("account", FetchPlan.BASE))
        .one();

ledgerService.apply(entry.getAccount().getId(), entry.getAmount(), entry.getType());
```

After writing the listener, scan the method: every `entry.getX()` used after loading must be in the fetch plan.

## Event timing

Decide by the direction of failure:

- Before commit, with a normal Spring `@EventListener`: logic that must be able to reject or change the current save/remove operation — required default values, rejecting unsupported updates or deletes, synchronous changes to related persistent state, validation whose exception must propagate to `DataManager.save()` or `DataManager.remove()`.
- After commit, with `@TransactionalEventListener`: reactions such as notifications or integration events, and any change to a resource the database transaction cannot roll back (file storage, an external API, an outbound notification), however required it is.

Example: an uploaded file deleted from a before-commit handler as "a required synchronous side effect". `EntityChangedEvent` is published in `beforeCommit`; if anything later aborts the transaction, the row stays and its file is gone.

## EntitySavingEvent and EntityLoadingEvent

`EntitySavingEvent` contains the entity instance before it is written to the data store. Use it for required defaults, value normalization, and transformations that must be persisted with the current save operation. It is published for created and updated instances only: `remove` publishes no `EntitySavingEvent`. To react to removals as well (cache invalidation, audit), listen to `EntityChangedEvent` and check `Type.DELETED`.

`EntityLoadingEvent` contains the loaded entity instance after it is read from the data store. Use it to initialize non-persistent attributes from local persistent fields, for example decrypting a stored value into a transient UI-facing property.

In both events, read and write only local attributes of the event entity. Do not assume referenced entities are loaded, or that loading references inside an `EntityLoadingEvent` will cascade loading events predictably.

## Forbidden

- Wrong event import packages such as `io.jmix.core.entity.EntityChangedEvent`; use `io.jmix.core.event.EntityChangedEvent`.
- Assuming `EntityChangedEvent` directly contains the full entity instance.
- Assuming `EntityLoadingEvent` is a replacement for fetching references or running cross-entity queries.
- Reading an entity property that is omitted from a custom fetch plan.
- Loading a deleted entity by `event.getEntityId()`.
- `@TransactionalEventListener` for validation, for immutable-record enforcement, or for required defaults — anything that must be able to reject or change the operation.
- Setting a required default in an `EntityChangedEvent` listener: it runs after the SQL flush, too late for `@NotNull` (see `jmix-create-entity`).
- A before-commit handler for a side effect the transaction cannot undo (file storage, external APIs, notifications).
- Expecting `EntitySavingEvent` on `remove`.
- Reading referenced entities in `EntitySavingEvent` or `EntityLoadingEvent` as if they were loaded.
- Putting UI code in entity listeners.
- Side effects without an explicit service method when several entities must stay consistent.

## Special cases

### When the validated write can violate a database constraint

`EntityChangedEvent` (before commit) is delivered after the SQL flush. If the write being validated can itself violate a database constraint, the constraint error is raised first and the listener never runs.
