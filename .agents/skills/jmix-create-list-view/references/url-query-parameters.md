# State that survives a page reload

Part of `jmix-create-list-view`. Read this when the view has a filter, pagination, or other state that should survive a page reload.

Add `<urlQueryParameters>` bindings so that the state of the filter, pagination, or other stateful components survives a page reload and can be shared by URL. This is the recommended pattern for any non-trivial list view; the seed-scaffolded `user-list-view.xml` has it by default.

```xml
<facets>
    <urlQueryParameters>
        <genericFilter component="genericFilter"/>
        <pagination component="pagination"/>
    </urlQueryParameters>
</facets>
...
<genericFilter id="genericFilter" dataLoader="customersDl">...</genericFilter>
<simplePagination id="pagination" dataLoader="customersDl"/>
```

- Both halves are mandatory. Each `<urlQueryParameters>` child binds to a layout component by id; if that component is missing, the view crashes at init with `Component with id '<x>' not found`, and every test that opens the view fails.
- When copying from another view (for example `user-list-view.xml`), copy both the binding and the component, or neither.
- Leave out `<urlQueryParameters>` when the view has no filter, pagination, or other such component (for example a small static reference table).
- Self-check: for every `<urlQueryParameters>` child, grep the same file for a component whose `id` equals the `component="..."` value. If it is absent, add the matching component to the layout (`<simplePagination id="pagination" dataLoader="...Dl"/>` or `<genericFilter id="genericFilter" dataLoader="...Dl">`) or delete that entry.
