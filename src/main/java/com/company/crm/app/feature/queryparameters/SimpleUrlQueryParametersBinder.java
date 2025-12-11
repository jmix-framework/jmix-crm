package com.company.crm.app.feature.queryparameters;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.flowui.facet.UrlQueryParametersFacet;
import io.jmix.flowui.facet.UrlQueryParametersFacet.UrlQueryParametersChangeEvent;
import io.jmix.flowui.facet.urlqueryparameters.AbstractUrlQueryParametersBinder;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.jmix.flowui.component.UiComponentUtils.isComponentAttachedToDialog;

public class SimpleUrlQueryParametersBinder extends AbstractUrlQueryParametersBinder {

    private final UrlQueryParametersFacet facet;
    private final Supplier<QueryParameters> parametersUpdater;
    private final Consumer<QueryParameters> parametersReader;

    public static SimpleUrlQueryParametersBinder registerBinder(UrlQueryParametersFacet facet,
                                                                Supplier<QueryParameters> stateUpdateProvider,
                                                                Consumer<QueryParameters> stateUpdateConsumer) {
        return new SimpleUrlQueryParametersBinder(facet, stateUpdateProvider, stateUpdateConsumer);
    }

    public void fireQueryParametersChanged() {
        View<?> view = facet.getOwner();

        if (view == null || isComponentAttachedToDialog(view)) {
            return;
        }

        // TODO: how to remove empty qp? via RouteSupport?
        view.getUI().ifPresent(ui ->
                fireQueryParametersChanged(new UrlQueryParametersChangeEvent(this, parametersUpdater.get())));
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        parametersReader.accept(queryParameters);
    }

    @Override
    public Component getComponent() {
        return null;
    }

    private SimpleUrlQueryParametersBinder(UrlQueryParametersFacet facet,
                                           Supplier<QueryParameters> parametersUpdater,
                                           Consumer<QueryParameters> parametersReader) {
        this.facet = facet;
        this.parametersUpdater = parametersUpdater;
        this.parametersReader = parametersReader;
        facet.registerBinder(this);
    }

    public static UrlQueryParametersFacet getUrlQueryParametersFacet(View<?> view) {
        UrlQueryParametersFacet urlQueryParametersFacet =
                ViewControllerUtils.getViewFacet(view, UrlQueryParametersFacet.class);
        if (urlQueryParametersFacet == null) {
            throw new IllegalStateException("View %s doesn't have %s"
                    .formatted(view.getClass().getSimpleName(), UrlQueryParametersFacet.class.getSimpleName()));
        }
        return urlQueryParametersFacet;
    }

    public static void validateIds(Iterable<? extends Component> components) {
        for (Component component : components) {
            validateId(component);
        }
    }

    public static void validateId(Component component) {
        if (component.getId().isEmpty()) {
            throw new IllegalArgumentException("Component id cannot be empty");
        }
    }
}