# Confirmation before save

Part of `jmix-create-detail-view`. Read this when the save must ask the user for confirmation.

`ValidationEvent` can only reject a save; it cannot ask a question and continue. The only hook that suspends a save and resumes it afterwards is `StandardDetailView.BeforeSaveEvent` with `preventSave()` / `resume()`:

```java
@Autowired
private Dialogs dialogs;

@Subscribe
public void onBeforeSave(final BeforeSaveEvent event) {
    if (!reissuesRelatedCodes()) {
        return;
    }
    event.preventSave();
    dialogs.createOptionDialog()
            .withHeader(messageBundle.getMessage("confirmDialog.header"))
            .withText(messageBundle.getMessage("confirmDialog.text"))
            .withActions(
                    new DialogAction(DialogAction.Type.YES)
                            .withHandler(e -> event.resume()),
                    new DialogAction(DialogAction.Type.NO))
            .open();
}
```

`Dialogs.createOptionDialog()` takes no view argument, unlike `createInputDialog(View)`. Without `preventSave()` the save runs regardless of what a dialog shows, so a home-made confirmation dialog does not stop it.
