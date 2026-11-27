# Widening an existing entity

Part of `jmix-create-entity`. Read this when widening an existing entity.

Adding fields to an entity that already exists is a different task from creating one.
Change only what the requirement asks for:

- Do not touch the `@Id` field or its strategy, `@Table(name = ...)`, `@Version`, or the
  existing columns' types and constraints. An entity with an assigned `Long` id stays that
  way — do not "normalize" it to a generated UUID.
- Add the new fields with their exact constraints, plus new `@Index` entries in
  `@Table(indexes = {...})` if the new columns need indexing ("Index parity").
- Pair it with an `addColumn` changelog, never a second `createTable` — see
  `jmix-create-liquibase-changelog` ("Changing an existing table").
- Follow the project's precedent: read the entity's own changelog before writing the new
  one, and match its naming and column-type style.
