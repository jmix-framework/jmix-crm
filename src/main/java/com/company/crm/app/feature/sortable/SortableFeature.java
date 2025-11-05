package com.company.crm.app.feature.sortable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.dom.Element;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.facet.SettingsFacet;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import org.vaadin.jchristophe.SortableConfig;
import org.vaadin.jchristophe.SortableLayout;

import java.util.List;
import java.util.Optional;

public final class SortableFeature {

    public static void makeSortable(Component component) {
        makeSortable(component, new SortableConfig());
    }

    public static SortableLayout makeSortable(Component component, SortableConfig config) {
        SortableLayout sortableLayout = null;

        Optional<String> sortableLayoutId = component.getId().map(id -> id + "Sortable");
        Optional<Component> parentOpt = component.getParent();

        if (parentOpt.isPresent()) {
            Component parent = parentOpt.get();

            View<?> view = UiComponentUtils.findView(parent);
            if (view != null) {
                SettingsFacet settingsFacet = ViewControllerUtils.getViewFacet(view, SettingsFacet.class);
                if (settingsFacet == null) {
                    throw new IllegalStateException("View %s doesn't have %s"
                            .formatted(view.getClass().getSimpleName(), SettingsFacet.class.getSimpleName()));
                }
            }

            if (parent instanceof HasComponents hasComponents) {
                Element parentElement = hasComponents.getElement();
                int index = parentElement.indexOfChild(component.getElement());
                sortableLayout = new SortableLayout(component, config);
                parentElement.insertChild(index, sortableLayout.getElement());
            } else {
                throw new IllegalStateException(("Component must have parent " +
                        "that implements %s").formatted(HasComponents.class.getSimpleName()));
            }
        }

        if (sortableLayout == null) {
            sortableLayout = new SortableLayout(component, config);
        }

        if (sortableLayoutId.isPresent()) {
            sortableLayout.setId(sortableLayoutId.get());
        }

        if (component instanceof HasSize hasSize) {
            sortableLayout.setMaxHeight(hasSize.getMaxHeight());
            sortableLayout.setMinHeight(hasSize.getMinHeight());
            sortableLayout.setMaxWidth(hasSize.getMaxWidth());
            sortableLayout.setMinWidth(hasSize.getMinWidth());
            sortableLayout.setWidth(hasSize.getWidth());
            sortableLayout.setHeight(hasSize.getHeight());
        }

        return sortableLayout;
    }

    public static void reorder(SortableLayout sortableLayout, List<Component> newOrder) {
        Component layout = sortableLayout.getChildren().findFirst().orElse(null);
        if (layout instanceof HasComponents hasComponents) {
            for (int i = 0; i < newOrder.size(); i++) {
                Component component = newOrder.get(i);
                hasComponents.remove(component);
                hasComponents.addComponentAtIndex(i, component);
            }
        }
    }

    private SortableFeature() {

    }
}
