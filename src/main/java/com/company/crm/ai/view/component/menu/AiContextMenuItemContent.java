package com.company.crm.ai.view.component.menu;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class AiContextMenuItemContent extends HorizontalLayout {

    public AiContextMenuItemContent(VaadinIcon iconName, String labelText) {
        setPadding(false);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);
        addClassName("ai-timeline-add-menu-item");

        Icon icon = iconName.create();
        icon.addClassName("ai-timeline-add-menu-item-icon");

        Span label = new Span(labelText);
        label.addClassName("ai-timeline-add-menu-item-label");

        add(icon, label);
    }
}
