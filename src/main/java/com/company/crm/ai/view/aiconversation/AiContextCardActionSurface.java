package com.company.crm.ai.view.aiconversation;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class AiContextCardActionSurface extends VerticalLayout {

    public AiContextCardActionSurface() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        addClassName("ai-card-action-surface");
    }

    public void configure(Icon icon,
                          String titleText,
                          String metaText,
                          String iconClassName,
                          String titleClassName,
                          String metaClassName,
                          Runnable onClick) {
        removeAll();

        if (onClick != null) {
            addClickListener(event -> onClick.run());
        }

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);

        if (iconClassName != null && !iconClassName.isEmpty()) {
            icon.addClassName(iconClassName);
        }

        Span title = new Span(titleText);
        if (titleClassName != null && !titleClassName.isEmpty()) {
            title.addClassNames(titleClassName.split(" "));
        }
        title.getElement().setProperty("title", titleText);

        header.add(icon, title);
        header.expand(title);

        VerticalLayout textLayout = new VerticalLayout();
        textLayout.setPadding(false);
        textLayout.setSpacing(false);
        textLayout.setWidthFull();
        textLayout.addClassName("ai-timeline-attachment-text");

        if (metaText != null && !metaText.isEmpty()) {
            Span meta = new Span(metaText);
            if (metaClassName != null && !metaClassName.isEmpty()) {
                meta.addClassNames(metaClassName.split(" "));
            }
            meta.getElement().setProperty("title", metaText);
            textLayout.add(meta);
        }

        add(header, textLayout);
    }
}
