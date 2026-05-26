package com.company.crm.ai.view.conversation.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.jmix.flowui.component.sidedialog.SideDialog;

public class AiSideDialogHeader extends HorizontalLayout {

    public AiSideDialogHeader() {
        setWidthFull();
        setAlignItems(Alignment.CENTER);
    }

    public void setDialog(SideDialog sideDialog) {
        setJustifyContentMode(JustifyContentMode.END);

        addCloseButton(sideDialog);
    }

    public void setDialog(SideDialog sideDialog, String titleText) {
        setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span titleSpan = new Span(titleText);
        titleSpan.addClassNames("font-semibold", "text-l");
        add(titleSpan);

        addCloseButton(sideDialog);
    }

    private void addCloseButton(SideDialog sideDialog) {
        Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> sideDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        add(closeButton);
    }
}
