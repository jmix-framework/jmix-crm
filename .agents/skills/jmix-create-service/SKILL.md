---
name: jmix-create-service
description: Create Jmix service-layer business logic with DataManager, transactions, and no UI coupling.
---

# Create Service Logic

Use this skill when implementing business operations, calculations, or persistence workflows.

## Steps

1. Create a Spring `@Service` in the `service` package.
2. Use constructor injection.
3. Prefer `DataManager` for normal loading and saving.
4. Add `@Transactional` when the operation changes multiple entities or must be atomic.
5. Validate business invariants in the service before saving.
6. Return domain values or saved entities, not UI components.
7. Keep view controllers thin: they should call services, not implement business rules.

## Service Template

```java
import io.jmix.core.DataManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountService {

    private final DataManager dataManager;

    public AccountService(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Transactional
    public Account applyDelta(UUID accountId, int delta) {
        Account account = dataManager.load(Account.class)
                .id(accountId)
                .one();

        account.setBalance(account.getBalance() + delta);
        account.setLastUpdated(LocalDateTime.now());

        return dataManager.save(account);
    }
}
```

Consume the instance RETURNED by `dataManager.save(...)`: the pre-save argument is stale (no generated id/version), while `save(...)` returns the fresh managed copy. A missing transaction boundary and using the stale argument are both compile- and render-clean defects.

## DataManager Loading

```java
Customer customer = dataManager.load(Customer.class)
        .id(customerId)
        .one();

List<Customer> activeCustomers = dataManager.load(Customer.class)
        .query("select e from Customer e where e.active = true")
        .list();
```

### DataManager saving

When the result is unused — `saveWithoutReload()`.

`save()` re-selects the entity from the database after persisting, so the caller
gets a fresh instance. When the caller does NOT use the returned instance, that
reload is wasted work:

```java
// result discarded → the reload is pointless
dataManager.save(repository);

// right call for a fire-and-forget persist
dataManager.saveWithoutReload(repository);
```

Rule: use `save()` when you consume the return value (you need the generated id,
the new version, or the reloaded state); use `saveWithoutReload()` when you do
not. The Jmix IDE inspection reports the discarded-result case as *"Result of
DataManager.save() is not used; use saveWithoutReload() to avoid a redundant
reload"* — it is a true finding, and `compileJava` never reports it, so a
compile-only Gate 1 lets it through (see `jmix-ide-static-analysis`).

## Gotchas

- New vs detached: a null id does not mean "new" (ids can be generated early). Use `io.jmix.core.EntityStates#isNew(entity)`.
- `DataManager` does more than `load`/`save`: `loadValues()` for scalar/aggregate data, the Condition API (`PropertyCondition` / `LogicalCondition`) as a JPQL alternative, pessimistic `lockMode()`, and hard delete by setting the `PersistenceHints.SOFT_DELETION` hint to `false` (e.g. `saveContext.setHint(PersistenceHints.SOFT_DELETION, false)`).

## Called from a scheduler, `@Async`, or a listener?

A service method reached from a thread with no logged-in user needs
`@Authenticated` (or a `SystemAuthenticator` block), or its first `DataManager`
call throws `IllegalStateException: Authentication is not set`. No gate catches
this — see `jmix-run-background-code`.

## Forbidden

- Business logic in view controllers.
- `dataManager.save(...)` with the result discarded — use `saveWithoutReload(...)`.
- UI components, dialogs, or notifications in services.
- Constructor calls for Jmix entities.
- `EntityManager` for regular CRUD.
- Missing transaction boundary for multi-step updates that must be atomic.

## Verify

Verify any unfamiliar Jmix/Vaadin symbol before typing it (`jmix-verify-api-symbol`), then run the gates after writing the service: static checks (`jmix-ide-static-analysis`) and the context-load test (`jmix-verify-bootrun`).
