# Records that open in read mode

Part of `jmix-create-detail-view`. Read this when the task says records open in read mode.

"The list opens records in read mode" or "the detail view is opened with the `read` action" means read-only open mode, set on the list side. It is not a read-only descriptor.

Set it in the list view: open action `list_read` instead of `list_edit`. Existing rows open as a non-editable form, new entities still open writable through `list_create`, and Jmix makes the fields read-only at runtime. The detail XML stays a normal writable descriptor with `detail_saveClose` and editable fields.

Do not build a read-only descriptor (every field `readOnly="true"`, only `detail_close`): it is always read-only, even for new entities, and cannot create entities or save edits.
