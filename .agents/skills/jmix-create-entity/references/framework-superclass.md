# Entity that extends a framework `@MappedSuperclass`

Part of `jmix-create-entity`. Read this when the entity extends a framework `@MappedSuperclass`.

When the entity extends a base class the framework ships (the application-settings base,
and other add-on base classes), the key is declared there already. Write no `@Id`, no
`@JmixGeneratedValue` and no `@Version` on the subclass. Read that superclass before writing
the changelog: it decides the id column's name and type (an `int` or `bigint` key is common
there, so `${uuid.type}` is the wrong default), and it often adds further columns (a
version, a tenant column) that the table must still create even though no field in the
subclass mentions them.
