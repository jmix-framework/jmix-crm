# Single-table inheritance

Part of `jmix-configure-fetch-plan`. Read this when the entity uses single-table inheritance.

Given an abstract `Payment` with concrete subclasses `CardPayment` and `BankTransfer` in one table, `dataManager.load(Payment.class)` builds its default `_base` plan from the own properties of the requested metaclass only. Subclass attributes are never included, even though the returned row is the subclass; the SQL selects base columns. So this throws:

```java
Payment loaded = dataManager.load(Payment.class).id(id).one();
String authCode = ((CardPayment) loaded).getAuthCode();
// IllegalStateException: Cannot get unfetched attribute [authCode] ... [detached]
```

A plain cast-and-read is never enough. Two fixes:

```java
// (a) load through the CONCRETE subclass — its default plan has the subclass's
//     own attributes, so getAuthCode() is fetched
CardPayment payment = dataManager.load(CardPayment.class).id(id).one();
String authCode = payment.getAuthCode();

// (b) still polymorphic, but the follow-up read goes through a plan built
//     against the CONCRETE class — name the subclass attributes explicitly
CardPayment payment = dataManager.load(CardPayment.class)
        .id(baseTyped.getId())
        .fetchPlan(fp -> fp.addFetchPlan(FetchPlan.BASE).add("authCode"))
        .one();
```

The same applies to a reference typed as the base class: reading a subclass-only attribute from it needs a re-load through the concrete subclass, as in (b). Nothing catches this before runtime; a test must run the line that reads the attribute.
