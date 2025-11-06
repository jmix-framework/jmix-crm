package com.company.crm.app.feature.queryparameters;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.flowui.facet.UrlQueryParametersFacet;
import io.jmix.flowui.facet.UrlQueryParametersFacet.UrlQueryParametersChangeEvent;
import io.jmix.flowui.facet.urlqueryparameters.AbstractUrlQueryParametersBinder;
import org.springframework.lang.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UrlQueryParametersBinder extends AbstractUrlQueryParametersBinder {

    private final Supplier<QueryParameters> stateUpdateProvider;
    private final Consumer<QueryParameters> stateUpdateConsumer;

    public static UrlQueryParametersBinder registerBinder(UrlQueryParametersFacet facet,
                                                          Supplier<QueryParameters> stateUpdateProvider,
                                                          Consumer<QueryParameters> stateUpdateConsumer) {
        return new UrlQueryParametersBinder(facet, stateUpdateProvider, stateUpdateConsumer);
    }

    public static UrlQueryParametersBinder registerBinder(UrlQueryParametersFacet facet,
                                                          Consumer<QueryParameters> stateUpdateConsumer) {
        return new UrlQueryParametersBinder(facet, null, stateUpdateConsumer);
    }

    public void fireQueryParametersChanged() {
        if (stateUpdateProvider == null) {
            throw new IllegalStateException("stateUpdateProvider not specified");
        }
        fireQueryParametersChanged(new UrlQueryParametersChangeEvent(this, stateUpdateProvider.get()));
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        stateUpdateConsumer.accept(queryParameters);
    }

    @Override
    public Component getComponent() {
        return null;
    }

    private UrlQueryParametersBinder(UrlQueryParametersFacet facet,
                                     @Nullable Supplier<QueryParameters> stateUpdateProvider,
                                     Consumer<QueryParameters> stateUpdateConsumer) {
        this.stateUpdateProvider = stateUpdateProvider;
        this.stateUpdateConsumer = stateUpdateConsumer;
        facet.registerBinder(this);
    }
}