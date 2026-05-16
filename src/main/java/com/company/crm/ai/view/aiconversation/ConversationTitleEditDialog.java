package com.company.crm.ai.view.aiconversation;

import com.company.crm.ai.model.AiConversation;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.View;

class ConversationTitleEditDialog {

    private final Dialogs dialogs;
    private final MessageBundle messageBundle;

    ConversationTitleEditDialog(Dialogs dialogs, MessageBundle messageBundle) {
        this.dialogs = dialogs;
        this.messageBundle = messageBundle;
    }

    void open(View<?> owner, AiConversation conversation, Runnable saveTitle) {
        if (conversation == null) {
            return;
        }

        dialogs.createInputDialog(owner)
                .withHeader(messageBundle.getMessage("editConversationTitleDialog.header"))
                .withLabelsPosition(Dialogs.InputDialogBuilder.LabelsPosition.TOP)
                .withParameters(
                        InputParameter.stringParameter("title")
                                .withLabel(messageBundle.getMessage("editConversationTitleDialog.titleField"))
                                .withRequired(true)
                                .withDefaultValue(conversation.getTitle() == null ? "" : conversation.getTitle())
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (!closeEvent.closedWith(DialogOutcome.OK)) {
                        return;
                    }

                    String updatedTitle = closeEvent.getValue("title");
                    if (updatedTitle == null || updatedTitle.isBlank()) {
                        return;
                    }

                    conversation.setTitle(updatedTitle.trim());
                    saveTitle.run();
                })
                .open();
    }
}
