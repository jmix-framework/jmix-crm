# Button that collects a scalar value for a service call

Part of `jmix-add-dialog-detail-flow`. Read this when the button collects a scalar value for a service call.

Use the Jmix input dialog API, not a raw Vaadin dialog.

```java
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;

@Autowired
private Dialogs dialogs;

@Subscribe("adjustButton")
public void onAdjustButtonClick(final ClickEvent<JmixButton> event) {
    dialogs.createInputDialog(this)
            .withHeader(messageBundle.getMessage("adjustDialog.header"))
            .withParameters(
                    InputParameter.intParameter("quantity")
                            .withLabel(messageBundle.getMessage("adjustDialog.quantity"))
                            .withDefaultValue(0)
            )
            .withActions(DialogActions.OK_CANCEL)
            .withCloseListener(closeEvent -> {
                if (closeEvent.closedWith(DialogOutcome.OK)) {
                    Integer quantity = closeEvent.getValue("quantity");
                    if (quantity != null) {
                        service.adjust(quantity);
                        affectedDl.load();
                    }
                }
            })
            .open();
}
```

- `messageBundle`, `service` and `affectedDl` are the view's existing injected dependencies (`MessageBundle`, your service bean, the affected `CollectionLoader`).
- Use message keys for dialog headers, labels, and notifications.
- Keep service calls in services; the view only collects input, calls the service, and reloads loaders.
