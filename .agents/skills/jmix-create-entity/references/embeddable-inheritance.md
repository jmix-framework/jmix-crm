# `@Embeddable` entities and JPA inheritance

Part of `jmix-create-entity`. Read this when the entity is `@Embeddable` or uses JPA inheritance.

- `@Embeddable` value objects (still annotated `@JmixEntity`) are supported, as are JPA
  inheritance strategies (`@Inheritance` with `JOINED`, `SINGLE_TABLE`, or
  `TABLE_PER_CLASS`).
- For `SINGLE_TABLE`, an abstract base class is normal and carries no `@DiscriminatorValue`
  of its own: it is never instantiated, so there is no row to discriminate. Only the
  concrete subclasses need one. (The published examples usually show a concrete base that
  does have one; the difference is plain JPA, not a Jmix rule — see
  `jmix-verify-api-symbol`.)
- Loading through the base class does not fetch subclass attributes. A
  `dataManager.load(BaseType.class)` builds its default fetch plan from the base metaclass's
  own properties, so casting the result to a subclass and reading a subclass-declared
  attribute throws `IllegalStateException: Cannot get unfetched attribute`. See
  `jmix-configure-fetch-plan` for the fix.
