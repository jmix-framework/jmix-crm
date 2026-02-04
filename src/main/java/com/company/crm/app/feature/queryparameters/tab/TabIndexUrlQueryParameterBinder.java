package com.company.crm.app.feature.queryparameters.tab;

import com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.facet.UrlQueryParametersFacet;
import io.jmix.flowui.facet.urlqueryparameters.AbstractUrlQueryParametersBinder;
import io.jmix.flowui.view.View;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder.getUrlQueryParametersFacet;
import static com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder.validateId;

/**
 * Binder to link the selected tab in TabSheet and url query parameter.
 */
public class TabIndexUrlQueryParameterBinder extends AbstractUrlQueryParametersBinder {

    private static final String QP_PREFIX = "tab-index-for-";

    private final Component tabsOwner;
    private final SimpleUrlQueryParametersBinder delegate;

    public static TabIndexUrlQueryParameterBinder register(View<?> view, JmixTabSheet tabSheet) {
        return register(getUrlQueryParametersFacet(view), tabSheet);
    }

    public static TabIndexUrlQueryParameterBinder register(View<?> view, Tabs tabs) {
        return register(getUrlQueryParametersFacet(view), tabs);
    }

    public static TabIndexUrlQueryParameterBinder register(UrlQueryParametersFacet facet, JmixTabSheet tabSheet) {
        return new TabIndexUrlQueryParameterBinder(facet, tabSheet, 0, tabSheet.getTabCount());
    }

    public static TabIndexUrlQueryParameterBinder register(UrlQueryParametersFacet facet, Tabs tabs) {
        return new TabIndexUrlQueryParameterBinder(facet, tabs, 0, tabs.getTabCount());
    }

    private TabIndexUrlQueryParameterBinder(View<?> view, JmixTabSheet tabSheet,
                                            int minTabValue, int maxTabValue) {
        this(getUrlQueryParametersFacet(view), tabSheet, minTabValue, maxTabValue);
    }

    private TabIndexUrlQueryParameterBinder(View<?> view, Tabs tabs,
                                            int minTabValue, int maxTabValue) {
        this(getUrlQueryParametersFacet(view), tabs, minTabValue, maxTabValue);
    }

    private TabIndexUrlQueryParameterBinder(UrlQueryParametersFacet facet, JmixTabSheet tabSheet,
                                            int minTabValue, int maxTabValue) {
        this.delegate = createDelegate(facet, tabSheet, minTabValue, maxTabValue);
        this.tabsOwner = tabSheet;
    }

    private TabIndexUrlQueryParameterBinder(UrlQueryParametersFacet facet, Tabs tabs,
                                            int minTabValue, int maxTabValue) {
        this.delegate = createDelegate(facet, tabs, minTabValue, maxTabValue);
        this.tabsOwner = tabs;
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        delegate.updateState(queryParameters);
    }

    @Override
    public Component getComponent() {
        return tabsOwner;
    }

    private SimpleUrlQueryParametersBinder createDelegate(UrlQueryParametersFacet facet,
                                                          JmixTabSheet tabSheet, int minTabValue, int maxTabValue) {
        SimpleUrlQueryParametersBinder delegate = doCreateDelegate(facet, tabSheet, minTabValue, maxTabValue);
        addTabSheetSelectionListener(tabSheet, delegate);
        return delegate;
    }

    private SimpleUrlQueryParametersBinder createDelegate(UrlQueryParametersFacet facet,
                                                          Tabs tabs, int minTabValue, int maxTabValue) {
        SimpleUrlQueryParametersBinder delegate = doCreateDelegate(facet, tabs, minTabValue, maxTabValue);
        addTabSheetSelectionListener(tabs, delegate);
        return delegate;
    }

    private SimpleUrlQueryParametersBinder doCreateDelegate(UrlQueryParametersFacet facet,
                                                            JmixTabSheet tabSheet, int minTabValue, int maxTabValue) {
        validateId(tabSheet);
        return createTabSheetBinder(tabSheet.getId().orElseThrow(), facet, tabSheet::getSelectedIndex, tabSheet::setSelectedIndex, minTabValue, maxTabValue);
    }

    private SimpleUrlQueryParametersBinder doCreateDelegate(UrlQueryParametersFacet facet,
                                                            Tabs tabs, int minTabValue, int maxTabValue) {
        validateId(tabs);
        return createTabSheetBinder(tabs.getId().orElseThrow(), facet, tabs::getSelectedIndex, tabs::setSelectedIndex, minTabValue, maxTabValue);
    }

    private static SimpleUrlQueryParametersBinder createTabSheetBinder(String tabOwnerId, UrlQueryParametersFacet facet,
                                                                       Supplier<Integer> indexProvider,
                                                                       Consumer<Integer> indexConsumer,
                                                                       int minTabValue, int maxTabValue) {
        String parameterKey = QP_PREFIX + tabOwnerId;
        return SimpleUrlQueryParametersBinder.registerBinder(facet,
                () -> QueryParameters.of(parameterKey, String.valueOf(indexProvider.get())),
                qp -> {
                    List<String> queryParameterValues = qp.getParameters().getOrDefault(parameterKey, List.of());
                    queryParameterValues.stream().findFirst().ifPresent(tabIndex -> {
                        try {
                            int parsedIndex = Integer.parseInt(tabIndex);
                            if (parsedIndex >= minTabValue && parsedIndex <= maxTabValue) {
                                indexConsumer.accept(parsedIndex);
                            }
                        } catch (Exception ignored) {
                            // ignore incorrect values and selection errors
                        }
                    });
                });
    }

    private void addTabSheetSelectionListener(JmixTabSheet tabSheet, SimpleUrlQueryParametersBinder delegate) {
        tabSheet.addSelectedChangeListener(e -> {
            if (e.isInitialSelection()) {
                return;
            }
            delegate.fireQueryParametersChanged();
        });
    }

    private void addTabSheetSelectionListener(Tabs tabs, SimpleUrlQueryParametersBinder delegate) {
        tabs.addSelectedChangeListener(e -> {
            if (e.isInitialSelection()) {
                return;
            }
            delegate.fireQueryParametersChanged();
        });
    }
}
