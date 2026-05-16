package com.company.crm.app.icons;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.IconFactory;

import java.util.Locale;

@JsModule("./icons/crm-icons.js")
public enum CrmIcons implements IconFactory {

    SPARKLES;

    @Override
    public Icon create() {
        return new Icon("crm",
                name().toLowerCase(Locale.ENGLISH).replace('_', '-'));
    }
}
