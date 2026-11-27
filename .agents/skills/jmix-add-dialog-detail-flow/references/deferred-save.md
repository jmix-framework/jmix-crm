# Save that waits for the opening view, or a passed container or field

Part of `jmix-add-dialog-detail-flow`. Read this when Save must wait for the opening view, or you pass a container or field.

How the dialog was opened decides whether Save writes to the database at once (the default) or merges into the opening view; `@Composition` on the attribute is what switches the default off:

| opened with | parent context | what Save does |
| --- | --- | --- |
| `.withParentDataContext(ctx)` | yes | merges into the parent; reaches the DB when the parent is saved |
| a property-bound `<collection property="x"/>` container where `x` has `@Composition` — passed via `withContainer`, or through `list_create` / `list_edit` on a grid bound to that container | yes | same |
| a field whose value source is a `@Composition` reference (`withField`, one-to-one composition) | yes | same |
| a standalone container with a loader — this is the usual list view, including its standard `list_create` / `list_edit` | **no** | writes to the database immediately; the saved entity is then merged into the opener's `DataContext` for tracking only |
| a property-bound container or a field whose attribute is a plain association (**no** `@Composition`) | **no** | same as above |
| none of the above — plain `DialogWindows.detail(...)` | **no** | writes to the database immediately; Cancel upstream no longer undoes it |

- Passing a container or a field is not enough: without `@Composition` on the attribute, the dialog saves straight to the database.
- A plain `DialogWindows.detail(this, Customer.class).editEntity(customer)` and `list_create` in an ordinary list view are not part of the opener's transaction: Save commits, and a later Cancel on the opening view leaves the commit in place.
- To make Save atomic with the parent, pass `.withParentDataContext(...)` explicitly. Do not rely on a container or a field for it unless the attribute has `@Composition`.

When a parent data context is set up, both views must be able to take part:

- The opened dialog view has no `DataContext` — `DevelopmentException: No DataContext in view '<id>'. Composition editing is impossible.` Add `<data>` with a writable context to the detail view.
- The opening view has a read-only context (`<data readOnly="true">`) — `IllegalArgumentException: Unsupported DataContext type ... Parent DataContext must implement DataContextInternal`. Drop `readOnly` on the opening view.
- The opening view has no `<data>` at all — no error, and no parent context either: the dialog quietly saves to the database.
