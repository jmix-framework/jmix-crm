# Baseline changelog

Part of `jmix-create-liquibase-changelog`. Read this when the project keeps a baseline changelog.

Some projects keep a baseline next to the dated migrations: one changelog that
creates every table for a fresh database, and usually a second one that adds
every FK and index. A fresh database builds the schema from the baseline and
marks the dated migrations as `MARK_RAN` through their own preconditions.

Two signs of a baseline in the root `changelog.xml` includes: it creates tables
older than the current release, and its changesets have
`<validCheckSum>ANY</validCheckSum>`. That marker allows the baseline to be
appended to in place; it is the one exception to "never edit an applied
changeset".

Put the schema change in both places, in the same commit:

- the dated migration, for databases that already exist;
- the baseline: put the `createTable` (or a new `<column>` on the table's
  existing `createTable`) inside the existing changeset, next to related tables;
  add the FK and each index as a new changeset in the constraints changelog,
  following that file's own pattern.

Keep every name identical across the dated file, the baseline and the entity's
`@Table(indexes = ...)`.
