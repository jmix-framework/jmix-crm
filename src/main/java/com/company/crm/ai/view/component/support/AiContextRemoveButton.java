package com.company.crm.ai.view.component.support;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;

public class AiContextRemoveButton extends Button {

    public AiContextRemoveButton(String ariaLabel, Runnable onRemove) {
        super(VaadinIcon.CLOSE.create());
        addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        if (ariaLabel != null) {
            setAriaLabel(ariaLabel);
        }
        if (onRemove != null) {
            addClickListener(event -> onRemove.run());
        }
    }
}
