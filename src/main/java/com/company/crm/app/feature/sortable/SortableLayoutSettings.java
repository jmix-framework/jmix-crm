package com.company.crm.app.feature.sortable;

import com.vaadin.flow.component.Component;
import io.jmix.flowui.facet.settings.Settings;

import java.util.ArrayList;
import java.util.List;

public class SortableLayoutSettings implements Settings {

    private String id;

    private List<String> componentIdChain = new ArrayList<>();

    public void setComponentIdChainFrom(List<Component> components) {
        List<String> chain = new ArrayList<>();
        for (Component c : components) {
            c.getId().ifPresent(chain::add);
        }
        setComponentIdChain(chain);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<String> getComponentIdChain() {
        return componentIdChain;
    }

    public void setComponentIdChain(List<String> componentIdChain) {
        this.componentIdChain = componentIdChain;
    }
}
