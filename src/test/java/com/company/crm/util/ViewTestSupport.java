package com.company.crm.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.View;
import org.springframework.boot.test.context.TestComponent;

import java.util.function.Consumer;

@TestComponent
public class ViewTestSupport {

    private final ViewNavigators viewNavigators;

    public ViewTestSupport(ViewNavigators viewNavigators) {
        this.viewNavigators = viewNavigators;
    }

    public <V extends View<?>> V currentView() {
        return UiTestUtils.getCurrentView();
    }

    public <V extends View<?>> void withCurrentView(Consumer<V> viewConsumer) {
        viewConsumer.accept(currentView());
    }

    public <V extends View<?>> V navigateTo(Class<V> viewClass) {
        viewNavigators.view(currentView(), viewClass).navigate();
        return currentView();
    }

    public <V extends View<?>> V navigateToAnd(Class<V> viewClass, Consumer<V> viewConsumer) {
        viewNavigators.view(currentView(), viewClass).navigate();
        withCurrentView(viewConsumer);
        return currentView();
    }

    public <E, V extends View<?>> V navigateToDetailView(E entity, Class<V> viewClass) {
        @SuppressWarnings("unchecked")
        Class<E> entityClass = (Class<E>) entity.getClass();
        return navigateToDetailView(entityClass, entity, viewClass);
    }

    public <E, V extends View<?>> V navigateToDetailView(Class<E> entityClass, E entity, Class<V> viewClass) {
        viewNavigators.detailView(currentView(), entityClass)
                .editEntity(entity)
                .withViewClass(viewClass)
                .navigate();
        return currentView();
    }

    public <E, V extends View<?>> V navigateToNewEntityDetail(Class<E> entityClass) {
        viewNavigators.detailView(currentView(), entityClass)
                .newEntity()
                .navigate();
        return currentView();
    }

    public <E, V extends View<?>> V navigateToNewEntityDetail(Class<E> entityClass, Class<V> viewClass) {
        viewNavigators.detailView(currentView(), entityClass)
                .newEntity()
                .withViewClass(viewClass)
                .navigate();
        return currentView();
    }

    public <T extends Component> T getComponent(View<?> view, String componentId) {
        return UiTestUtils.getComponent(view, componentId);
    }

    public <T extends Component> T getComponent(String componentId) {
        return getComponent(currentView(), componentId);
    }

    public <T extends Component> T getComponentAnd(String componentId, Consumer<T> componentConsumer) {
        return getComponentAnd(currentView(), componentId, componentConsumer);
    }

    public <T extends Component> T getComponentAnd(View<?> view, String componentId, Consumer<T> componentConsumer) {
        T component = getComponent(view, componentId);
        componentConsumer.accept(component);
        return component;
    }

    @SuppressWarnings("unchecked")
    public <V> void setComponentValue(String componentId, V value) {
        ((HasValue<?, V>) getComponent(componentId)).setValue(value);
    }

    public boolean isReadOnly(String componentId) {
        return isReadOnly(currentView(), componentId);
    }

    public boolean isReadOnly(View<?> view, String componentId) {
        Component component = UiTestUtils.getComponent(view, componentId);
        if (component instanceof HasValue<?, ?> hasValue) {
            return hasValue.isReadOnly();
        }
        if (component instanceof StandardDetailView<?> detailView) {
            return detailView.isReadOnly();
        }
        throw new IllegalArgumentException(String.format("Component '%s' does not support read-only state", componentId));
    }

    public boolean isEnabled(String componentId) {
        return isEnabled(currentView(), componentId);
    }

    public boolean isEnabled(View<?> view, String componentId) {
        Component component = UiTestUtils.getComponent(view, componentId);
        if (component instanceof HasEnabled hasEnabled) {
            return hasEnabled.isEnabled();
        }
        return component.getElement().isEnabled();
    }

    public boolean isVisible(String componentId) {
        return isVisible(currentView(), componentId);
    }

    public boolean isVisible(View<?> view, String componentId) {
        Component component = UiTestUtils.getComponent(view, componentId);
        return component.isVisible();
    }

    public void click(String componentId) {
        click(currentView(), componentId);
    }

    public void click(View<?> view, String componentId) {
        Component component = UiTestUtils.getComponent(view, componentId);
        if (component instanceof JmixButton button) {
            button.click();
        } else {
            component.getElement().executeJs("this.click()");
        }
    }
}
