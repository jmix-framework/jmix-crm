package com.company.crm.app.ui.component;

import com.company.crm.app.util.ui.CrmUiUtils;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class GridEmptyStateComponent extends VerticalLayout {

    private static final String DEFAULT_TEXT = "There is nothing here yet...";

    public GridEmptyStateComponent() {
        this(DEFAULT_TEXT);
    }

    public GridEmptyStateComponent(String emptyText) {
        initComponent(emptyText);
    }

    private void initComponent(String emptyText) {
        setPadding(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setSizeFull();

        var logo = CrmUiUtils.appLogo();
        logo.setSize("6em");

        var message = new Span(emptyText);
        message.addClassNames(LumoUtility.FontWeight.BOLD);

        add(logo, message);
    }
}
