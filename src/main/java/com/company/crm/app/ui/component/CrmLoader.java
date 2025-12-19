package com.company.crm.app.ui.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class CrmLoader extends VerticalLayout {

    public CrmLoader() {
        initComponent();
    }

    public void startLoading() {
        removeAll();
        addLogo();
        addLoadingMessage();
        setVisible(true);
    }

    public void stopLoading() {
        removeAll();
        setVisible(false);
    }

    public void setLogoSize(String size) {
        getChildren()
                .filter(SvgIcon.class::isInstance).findFirst()
                .map(SvgIcon.class::cast)
                .ifPresent(icon -> icon.setSize(size));
    }

    private void initComponent() {
        setPadding(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().setTransition("opacity 200ms ease");
    }

    private void addLogo() {
        var logo = new SvgIcon("images/logo.svg");
        logo.addClassName("loader-animation");
        logo.setSize("6em");
        add(logo);
    }

    private void addLoadingMessage() {
        Span loadingMessage = new Span("Loading...");
        loadingMessage.addClassNames(LumoUtility.FontWeight.THIN, LumoUtility.FontSize.SMALL);
        add(loadingMessage);
    }
}
