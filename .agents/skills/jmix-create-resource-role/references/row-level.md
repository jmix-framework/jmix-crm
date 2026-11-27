# Limits on which rows a user can see or change

Part of `jmix-create-resource-role`. Read this when the requirement limits which rows a user can see or change.

Wording such as "own records", "only in status X", "own department". A resource
role grants what the user can do; a row-level role restricts which rows. Put row
rules in a separate interface annotated with `@RowLevelRole`.

- `@JpqlRowLevelPolicy(entityClass = ..., where = "...")` filters in the database.
  Use `{E}` as the entity alias and `:current_user_*` parameters (for example
  `:current_user_username`).
- `@PredicateRowLevelPolicy(entityClass = ..., actions = {...})` filters in
  memory; the method returns a `RowLevelPredicate` / `RowLevelBiPredicate`. Use it
  for logic JPQL cannot express and for non-read operations.

```java
@RowLevelRole(name = "Own Orders Only", code = "app_OwnOrdersOnly")
public interface OwnOrdersOnlyRole {
    @JpqlRowLevelPolicy(entityClass = Order.class,
            where = "{E}.createdBy = :current_user_username")
    void orderPolicy();
}
```

**Row-level policies are joined with AND** — JPQL conditions and predicates alike,
across all the user's row-level roles. A user with both "own orders" and
"department orders" sees only their own. Assign only the wider role, or write one
role whose condition contains the `or`; check seed role assignments too.

A JPQL policy affects only the root entity of a loaded graph. If the entity is
also loaded as a collection inside another entity's graph, define both a
`@JpqlRowLevelPolicy` and a `@PredicateRowLevelPolicy` for it.

An UPDATE predicate is checked against two states of the entity.
`StandardDetailView` and the list `EditAction` check the entity as loaded, to
decide whether the view (or the child dialog) opens read-only. The data store
checks the entity as it is being saved, with the user's changes applied. So a
predicate on a field the same user changes in that save judges the new value: an
`Order` predicate `status == DRAFT` lets the owner edit a draft, then denies, with
`AccessDeniedException`, the owner's own save that moves the order to SUBMITTED.

- Put ownership in the predicate (the requester is the current user); it does not
  change during the save.
- Keep status rules for the entity itself in the service or the view.
- A status rule fits child entities that the transition does not touch, for
  example `OrderLine` changes allowed only while the parent `Order` is a draft.
  Then save the user's pending line edits before the status change, in a separate
  save: in one save the changed lines are checked against the parent's new status
  and denied.
