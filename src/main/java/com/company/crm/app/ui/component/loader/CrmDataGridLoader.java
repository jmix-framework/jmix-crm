package com.company.crm.app.ui.component.loader;

import io.jmix.flowui.xml.layout.loader.component.DataGridLoader;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.company.crm.app.util.ui.CrmUiUtils.setDefaultEmptyStateComponent;

@Primary
@Component
public class CrmDataGridLoader extends DataGridLoader {
    @Override
    public void loadComponent() {
        super.loadComponent();
        setDefaultEmptyStateComponent(resultComponent);
    }
}
