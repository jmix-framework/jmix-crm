package com.company.crm.app.ui.component;

import com.company.crm.app.util.ui.CrmUiUtils;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.jspecify.annotations.Nullable;

public class CrmLoader extends VerticalLayout {

    private static final String DEFAULT_MESSAGE = "Loading...";

    private final Span loadingMessage = new Span();

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

    public void setLoadingMessage(String message) {
        setLoadingMessage(message, null);
    }

    public void setLoadingMessage(String message, @Nullable String badge) {
        String messageToSet = message == null || message.isBlank() ? DEFAULT_MESSAGE : message;
        loadingMessage.setText(messageToSet);
        CrmUiUtils.setBadge(loadingMessage, badge);
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
        var logo = CrmUiUtils.appLogo();
        logo.addClassName("loader-animation");
        logo.setSize("6em");
        add(logo);
    }

    private void addLoadingMessage() {
        setLoadingMessage(loadingMessage.getText(), CrmUiUtils.DEFAULT_BADGE);
        loadingMessage.addClassNames(LumoUtility.FontWeight.THIN, LumoUtility.FontSize.SMALL);
        loadingMessage.addClassName("crm-loader-message");
        add(loadingMessage);
    }
}
