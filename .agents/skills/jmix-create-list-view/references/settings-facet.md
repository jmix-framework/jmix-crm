# `<settings>` facet

Part of `jmix-create-list-view`. Read this when the view has or gets a `<settings>` facet.

Add a settings facet when the user's grid layout should persist between visits. Existing views often have one, even though this skill's skeleton does not add it. Place it in `<facets>`:

```xml
<facets>
    <dataLoadCoordinator auto="true"/>
    <settings auto="true"/>
</facets>
```

- With `auto="true"` the facet manages only components that have an `id`: `dataGrid` / `treeDataGrid` (column order, width, visibility, sorting), `genericFilter` and `details` (opened state), `simplePagination` (page size, when `itemsPerPageVisible="true"`).
- Storage is the `FLOWUI_USER_SETTINGS` table, provided by `jmix-flowui-data-starter`. Check that the module is a dependency.
- Do not add a resource-role policy for `flowui_UserSettingsItem`: the framework reads and writes settings through `UnconstrainedDataManager`.
- Saved user settings of these components override later descriptor changes. On open, saved values are applied over the descriptor. They have no version marker, and editing the descriptor does not invalidate them.
- Opening the view once is enough: a full snapshot of all columns is saved on leave. So after a descriptor change, only users who never opened the view see the change.
- The symptom looks like "the deployment did not arrive": new value in the source, old value on screen. Until the saved settings are cleared, a browser check proves nothing, and a correct change looks broken; do not "fix" working code a second time.
- After changing column width, order or visibility, clear the stored settings for that view (`FLOWUI_USER_SETTINGS`) before verifying.
