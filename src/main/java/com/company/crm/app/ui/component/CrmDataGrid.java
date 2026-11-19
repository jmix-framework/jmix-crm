package com.company.crm.app.ui.component;

import com.company.crm.app.util.ui.GridScrollUtils;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import io.jmix.flowui.component.grid.DataGrid;
import org.jspecify.annotations.Nullable;

public class CrmDataGrid<E> extends DataGrid<E> {

    @Nullable
    private String scrollParameter;

    public void rememberScrollPosition(String ownerId, Component owner) {
        scrollParameter = GridScrollUtils.rememberScrollPosition(this, ownerId, owner);
    }

    @ClientCallable
    @SuppressWarnings("unused")
    /// Invoked by Vaadin through $server.
    public void receiveScrollPosition(Integer index, String path) {
        GridScrollUtils.saveScrollPosition(this, scrollParameter, index, path);
    }
}
