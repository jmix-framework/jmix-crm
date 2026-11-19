package com.company.crm.app.ui.component.loader;

import com.company.crm.app.ui.component.CrmDataGrid;
import io.jmix.flowui.xml.layout.loader.component.DataGridLoader;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import static com.company.crm.app.util.ui.CrmUiUtils.setDefaultEmptyStateComponent;

@Primary
@Component
public class CrmDataGridLoader extends DataGridLoader {
    @Override
    protected CrmDataGrid<?> createComponent() {
        return factory.create(CrmDataGrid.class);
    }

    @Override
    public void loadComponent() {
        super.loadComponent();
        setDefaultEmptyStateComponent(resultComponent);
        ((CrmDataGrid<?>) resultComponent).rememberScrollPosition(getContext().getFullOriginId(), getContext().getOrigin());
    }
}
