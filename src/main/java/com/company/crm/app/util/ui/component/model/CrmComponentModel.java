package com.company.crm.app.util.ui.component.model;

import com.vaadin.flow.component.Component;
import io.jmix.flowui.UiComponents;
import org.springframework.context.ApplicationContext;

public interface CrmComponentModel<C extends Component> {

    Class<C> getComponentClass();

    void fillComponent(C component, ApplicationContext applicationContext);

    default C createComponent(ApplicationContext applicationContext) {
        UiComponents uiComponents = applicationContext.getBean(UiComponents.class);
        C component = uiComponents.create(getComponentClass());
        fillComponent(component, applicationContext);
        return component;
    }
}
