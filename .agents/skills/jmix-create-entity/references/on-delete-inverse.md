# `@OnDeleteInverse` on a target without soft delete

Part of `jmix-create-entity`. Read this when a reference has `@OnDeleteInverse` and its target has no soft delete.

Jmix runs the delete-policy processor for `@OnDeleteInverse(DeletePolicy.DENY)` / `UNLINK` /
`CASCADE` only when the entity being deleted supports soft delete. The trait that matters is
on the reference's target (`Product` when the annotation sits on `OrderLine.product`), not on
the entity that carries the annotation. On a hard-deleted target the annotation only tells
Studio to generate an FK constraint with `ON DELETE`; DENY then appears as a foreign-key
violation from the database — a persistence exception, never `DeletePolicyException`.

Keep the annotation and put the policy on the foreign key in the changelog (see
`jmix-create-liquibase-changelog`, **When two referential actions can act on one row**):

- UNLINK → `onDelete="SET NULL"` on the FK; the column must be nullable.
- DENY → a plain FK with no `onDelete`; the database rejects the delete. Tests and error
  handling must expect the persistence exception, not `DeletePolicyException`.
- CASCADE → `onDelete="CASCADE"` on the FK.
