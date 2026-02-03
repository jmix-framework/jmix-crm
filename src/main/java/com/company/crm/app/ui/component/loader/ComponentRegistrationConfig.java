package com.company.crm.app.ui.component.loader;

import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.sys.registration.ComponentRegistration;
import io.jmix.flowui.sys.registration.ComponentRegistrationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ComponentRegistrationConfig {

    @Bean
    @Primary
    public ComponentRegistration dataGrid() {
        return ComponentRegistrationBuilder.create(DataGrid.class)
                .withComponentLoader("dataGrid", CrmDataGridLoader.class)
                .build();
    }

    @Bean
    @Primary
    public ComponentRegistration treeDataGrid() {
        return ComponentRegistrationBuilder.create(TreeDataGrid.class)
                .withComponentLoader("treeDataGrid", CrmTreeDataGridLoader.class)
                .build();
    }
}
