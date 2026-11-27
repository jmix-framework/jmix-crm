# Facets in a fragment

Part of `jmix-create-fragment`. Read this when the fragment needs facets.

Since Jmix 2.8, a fragment descriptor supports a `<facets>` element with fragment-specific facet names, not the view ones: `fragmentDataLoadCoordinator`, `fragmentSettings`, `urlQueryParameters`, and `timer`.

```xml
<facets>
    <fragmentDataLoadCoordinator auto="true"/>
</facets>
```

- `fragmentDataLoadCoordinator auto="true"` loads the fragment's own `<data>` loaders; do not also call `getFragmentData().loadAll()` for the same loaders.
- A facet that persists state (`fragmentSettings`) needs a stable `id` on the embedded fragment instance; prefer declarative embedding with an `id`.
- `urlQueryParameters` entries must reference components declared in the fragment.
