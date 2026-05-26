package com.company.crm.ai.view.component.support;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class AiContextPendingCardLayout extends HorizontalLayout {

    public AiContextPendingCardLayout(Component actionSurface, Component removeButton) {
        setPadding(false);
        setSpacing(true);
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        add(actionSurface, removeButton);
        expand(actionSurface);
    }
}
