---
name: jmix-create-service
description: Create Jmix service-layer business logic with DataManager, transactions, and no UI coupling.
---

# Create Service Logic

## Steps

1. Create a Spring `@Service` in the `service` package.
2. Use constructor injection.
3. Prefer `DataManager` for normal loading and saving.
4. Add `@Transactional` when the operation changes multiple entities or must be atomic.
5. Validate business invariants in the service before saving.
6. Return domain values or saved entities, not UI components.
7. Keep view controllers thin: they should call services, not implement business rules.
8. If a scheduler, `@Async`, or a listener calls the method, read **When a scheduler, `@Async`, or a listener calls the service** below.
9. If you need aggregates, a lock, or a hard delete, read **When you need aggregates, a lock, or a hard delete** below.
10. Verify any unfamiliar Jmix/Vaadin symbol before typing it (`jmix-verify-api-symbol`). After writing the service, run static checks (`jmix-ide-static-analysis`) and the tests (Gate 2, `jmix-verify-bootrun`).

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

Use the instance returned by `save(...)`: the argument is stale (no generated id or version). A missing transaction boundary and use of the stale argument both compile and render cleanly.

## DataManager Loading

```java
Customer customer = dataManager.load(Customer.class)
        .id(customerId)
        .one();

List<Customer> activeCustomers = dataManager.load(Customer.class)
        .query("select e from Customer e where e.active = true")
        .list();
```

The Condition API (`PropertyCondition` / `LogicalCondition`) is an alternative to JPQL.

## DataManager Saving

`save()` reloads the entity after persisting. Use it when you use the return value (generated id, new version, reloaded state); otherwise use `saveWithoutReload()`:

```java
// result discarded → the reload is pointless
dataManager.save(repository);

// right call for a fire-and-forget persist
dataManager.saveWithoutReload(repository);
```

The Jmix IDE inspection reports the discarded result as *"Result of DataManager.save() is not used; use saveWithoutReload() to avoid a redundant reload"*. It is a true finding that `compileJava` never reports, so a compile-only Gate 1 misses it (see `jmix-ide-static-analysis`).

## Forbidden

- Business logic in view controllers.
- UI components, dialogs, or notifications in services.
- `dataManager.save(...)` with the result discarded — use `saveWithoutReload(...)`.
- Using the argument of `save(...)` instead of the returned instance.
- Missing transaction boundary for multi-step updates that must be atomic.
- Constructor calls for Jmix entities.
- `EntityManager` for regular CRUD.
- Field injection instead of constructor injection.
- Treating a green `compileJava` as Gate 1 without the static checks.
- Treating a null id as "new" (ids can be generated early) — use `io.jmix.core.EntityStates#isNew(entity)`.

## Special cases

### When a scheduler, `@Async`, or a listener calls the service

The thread has no logged-in user, and the first `DataManager` call throws `IllegalStateException: Authentication is not set` — see `jmix-run-background-code`.

### When you need aggregates, a lock, or a hard delete

- Scalar or aggregate data: `dataManager.loadValues()`.
- Pessimistic lock: `lockMode()` on the load.
- Hard delete: set the `PersistenceHints.SOFT_DELETION` hint to `false`, for example `saveContext.setHint(PersistenceHints.SOFT_DELETION, false)`.
