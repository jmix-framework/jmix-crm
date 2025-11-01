package com.company.crm.app.service.ui;

import com.company.crm.app.util.ui.component.model.CrmComponentModel;
import com.vaadin.flow.component.Component;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class CrmUiComponents {

    private final ApplicationContext applicationContext;

    public CrmUiComponents(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public <C extends Component> C createCrmComponent(CrmComponentModel<C> model) {
        return model.createComponent(applicationContext);
    }
}
