package com.company.crm.app.ui.component;

import com.company.crm.app.util.ui.CrmUiUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.Messages;
import org.jspecify.annotations.Nullable;

public class CrmLoader extends VerticalLayout {

    private final Span loadingMessage = new Span();

    private String logoSize = "6em";

    public CrmLoader() {
        initComponent();
    }

    public CrmLoader(Component forComponent) {
        this();
        forComponent(forComponent);
    }

    private void initComponent() {
        setSizeFull();
        setPadding(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().setTransition("opacity 200ms ease");
        addComponents();
    }

    private void forComponent(Component component) {
        if (component instanceof HasSize hasSize) {
            setWidth(hasSize.getWidth());
            setHeight(hasSize.getHeight());

            setMaxWidth(hasSize.getMaxWidth());
            setMaxHeight(hasSize.getMaxHeight());

            setMinWidth(hasSize.getMinWidth());
            setMinHeight(hasSize.getMinHeight());
        }

        if (component instanceof HasComponents hasComponents) {
            hasComponents.add(this);
        }
    }

    public void startLoading() {
        setVisible(true);
    }

    public void stopLoading() {
        setVisible(false);
    }

    public void setLoadingMessage(String message) {
        setLoadingMessage(message, null);
    }

    public void setLoadingMessage(String message, @Nullable String badge) {
        String messageToSet = message == null || message.isBlank()
                ? Instantiator.get(UI.getCurrent()).getOrCreate(Messages.class).getMessage("loading")
                : message;
        loadingMessage.setText(messageToSet);
        CrmUiUtils.setBadge(loadingMessage, badge);
    }

    public void setLogoSize(String logoSize) {
        this.logoSize = logoSize;
        updateLogoSize();
    }

    private void updateLogoSize() {
        getChildren()
                .filter(SvgIcon.class::isInstance).findFirst()
                .map(SvgIcon.class::cast)
                .ifPresent(icon -> icon.setSize(logoSize));
    }

    private void addComponents() {
        addLogo();
        addLoadingMessage();
    }

    private void addLogo() {
        var logo = CrmUiUtils.appLogo();
        logo.addClassName("loader-animation");
        logo.setSize(logoSize);
        add(logo);
    }

    private void addLoadingMessage() {
        setLoadingMessage(loadingMessage.getText(), CrmUiUtils.DEFAULT_BADGE);
        loadingMessage.addClassNames(LumoUtility.FontWeight.THIN, LumoUtility.FontSize.SMALL);
        loadingMessage.addClassName("crm-loader-message");
        add(loadingMessage);
    }
}
