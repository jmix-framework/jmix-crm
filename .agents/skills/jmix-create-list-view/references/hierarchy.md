# Entity with a reference to its own class

Part of `jmix-create-list-view`. Read this when the entity has a reference to its own class.

Show the list as a tree (here for `Department.parent`):

```xml
<treeDataGrid id="departmentsDataGrid" dataContainer="departmentsDc"
              hierarchyProperty="parent" width="100%">
    <!-- actions and columns as in dataGrid -->
</treeDataGrid>
```

- Actions and buttons are the same as for `dataGrid`.
- `showOrphans="true"` shows as a root a record whose parent is not in the container. With the default `false`, such records disappear when a filter is applied.
- The application must forbid cycles (`A` is its own parent, `A → B → A`): records in a cycle are never shown, together with their subtrees.
- Enforce the ban at the data level (see `jmix-create-entity` / `jmix-add-entity-event-listener`), not in the view: a direct `DataManager.save` bypasses the view. Removing the edited record from its own parent picker is a convenience, not the ban.
