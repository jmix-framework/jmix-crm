package com.company.crm.ai.view.component.support;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;

public class AiContextCardActionSurface extends VerticalLayout {

    private Registration clickRegistration;

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
        removeClickRegistration();

        if (onClick != null) {
            clickRegistration = addClickListener(event -> onClick.run());
        }

        HorizontalLayout header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(true);
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);

        if (org.springframework.util.StringUtils.hasText(iconClassName)) {
            icon.addClassName(iconClassName);
        }

        Span title = new Span(titleText);
        if (org.springframework.util.StringUtils.hasText(titleClassName)) {
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

        if (org.springframework.util.StringUtils.hasText(metaText)) {
            Span meta = new Span(metaText);
            if (org.springframework.util.StringUtils.hasText(metaClassName)) {
                meta.addClassNames(metaClassName.split(" "));
            }
            meta.getElement().setProperty("title", metaText);
            textLayout.add(meta);
        }

        add(header, textLayout);
    }

    private void removeClickRegistration() {
        if (clickRegistration != null) {
            clickRegistration.remove();
            clickRegistration = null;
        }
    }
}
