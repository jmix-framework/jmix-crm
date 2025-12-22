package com.company.crm.app.feature.sortable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.spring.annotation.SpringComponent;
import io.jmix.core.JmixOrder;
import io.jmix.flowui.facet.settings.Settings;
import io.jmix.flowui.facet.settings.component.binder.ComponentSettingsBinder;
import org.springframework.core.annotation.Order;
import org.vaadin.jchristophe.SortableLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringComponent
@Order(JmixOrder.LOWEST_PRECEDENCE)
public class SortableLayoutSettingsBinder implements ComponentSettingsBinder<SortableLayout, SortableLayoutSettings> {

    @Override
    public Class<? extends Component> getComponentClass() {
        return SortableLayout.class;
    }

    @Override
    public Class<? extends Settings> getSettingsClass() {
        return SortableLayoutSettings.class;
    }

    @Override
    public void applySettings(SortableLayout sortableLayout, SortableLayoutSettings settings) {
        if (isOrderChanged(sortableLayout, settings)) {
            List<String> expectedChain = settings.getComponentIdChain();
            List<Component> expectedOrder = sortComponents(sortableLayout.getComponents(), expectedChain);
            SortableFeature.reorder(sortableLayout, expectedOrder);
        }
    }

    private List<Component> sortComponents(List<Component> components, List<String> expectedChain) {
        components.sort((c1, c2) -> {
            Optional<String> c1Id = c1.getId();
            Optional<String> c2Id = c2.getId();
            if (c1Id.isEmpty() && c2Id.isPresent()) {
                return -1;
            } else if (c1Id.isPresent() && c2Id.isEmpty()) {
                return 1;
            } else {
                return c1Id.map(s -> Integer.compare(expectedChain.indexOf(s), expectedChain.indexOf(c2Id.get()))).orElse(0);
            }
        });
        return components;
    }

    @Override
    public boolean saveSettings(SortableLayout sortableLayout, SortableLayoutSettings settings) {
        if (isOrderChanged(sortableLayout, settings)) {
            settings.setComponentIdChainFrom(sortableLayout.getComponents());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public SortableLayoutSettings getSettings(SortableLayout sortableLayout) {
        SortableLayoutSettings settings = new SortableLayoutSettings();
        settings.setId(sortableLayout.getId().orElse(null));
        settings.setComponentIdChainFrom(sortableLayout.getComponents());
        return settings;
    }

    private boolean isOrderChanged(SortableLayout component, SortableLayoutSettings settings) {
        List<String> savedChain = settings.getComponentIdChain();
        List<String> actualChain = getChain(component);

        if (savedChain.size() != actualChain.size()) {
            return true;
        } else if (savedChain.isEmpty()) {
            return false;
        }

        for (int i = 0; i < actualChain.size(); i++) {
            if (!savedChain.get(i).equals(actualChain.get(i))) {
                return true;
            }
        }

        return false;
    }

    private List<String> getChain(SortableLayout component) {
        List<String> actualChain = new ArrayList<>();
        for (Component c : component.getComponents()) {
            c.getId().ifPresent(actualChain::add);
        }
        return actualChain;
    }
}
