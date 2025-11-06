package com.company.crm.app.feature.queryparameters.tab;

import com.company.crm.app.feature.queryparameters.UrlQueryParametersBinder;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.QueryParameters;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.facet.UrlQueryParametersFacet;
import io.jmix.flowui.facet.urlqueryparameters.AbstractUrlQueryParametersBinder;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Binder to link the selected tab in TabSheet and url parameters.
 */
public class TabIndexUrlQueryParameterBinder extends AbstractUrlQueryParametersBinder {

    public static final String TAB_INDEX_QUERY_PARAMETER = "tab";
    private final UrlQueryParametersBinder delegate;

    public static TabIndexUrlQueryParameterBinder register(View<?> view, JmixTabSheet tabSheet) {
        return register(getUrlQueryParametersFacet(view), tabSheet);
    }

    public static TabIndexUrlQueryParameterBinder register(View<?> view, Tabs tabs) {
        return register(getUrlQueryParametersFacet(view), tabs);
    }

    public static TabIndexUrlQueryParameterBinder register(UrlQueryParametersFacet facet, JmixTabSheet tabSheet) {
        return new TabIndexUrlQueryParameterBinder(facet, tabSheet, 0, tabSheet.getComponents().size());
    }

    public static TabIndexUrlQueryParameterBinder register(UrlQueryParametersFacet facet, Tabs tabs) {
        return new TabIndexUrlQueryParameterBinder(facet, tabs, 0, tabs.getChildren().toList().size());
    }

    @Override
    public void updateState(QueryParameters queryParameters) {
        delegate.updateState(queryParameters);
    }

    @Override
    public Component getComponent() {
        return null;
    }

    private TabIndexUrlQueryParameterBinder(View<?> view, JmixTabSheet tabSheet,
                                            int minTabValue, int maxTabValue) {
        this(getUrlQueryParametersFacet(view), tabSheet, minTabValue, maxTabValue);
    }

    private TabIndexUrlQueryParameterBinder(View<?> view, Tabs tabs,
                                            int minTabValue, int maxTabValue) {
        this(getUrlQueryParametersFacet(view), tabs, minTabValue, maxTabValue);
    }

    private static UrlQueryParametersFacet getUrlQueryParametersFacet(View<?> view) {
        UrlQueryParametersFacet urlQueryParametersFacet =
                ViewControllerUtils.getViewFacet(view, UrlQueryParametersFacet.class);
        if (urlQueryParametersFacet == null) {
            throw new IllegalStateException("View %s doesn't have %s"
                    .formatted(view.getClass().getSimpleName(), UrlQueryParametersFacet.class.getSimpleName()));
        }
        return urlQueryParametersFacet;
    }

    private TabIndexUrlQueryParameterBinder(UrlQueryParametersFacet facet, JmixTabSheet tabSheet,
                                            int minTabValue, int maxTabValue) {
        this.delegate = createDelegate(facet, tabSheet, minTabValue, maxTabValue);
    }

    private TabIndexUrlQueryParameterBinder(UrlQueryParametersFacet facet, Tabs tabs,
                                            int minTabValue, int maxTabValue) {
        this.delegate = createDelegate(facet, tabs, minTabValue, maxTabValue);
    }

    private UrlQueryParametersBinder createDelegate(UrlQueryParametersFacet facet,
                                                    JmixTabSheet tabSheet, int minTabValue, int maxTabValue) {
        UrlQueryParametersBinder delegate = doCreateDelegate(facet, tabSheet, minTabValue, maxTabValue);
        addTabSheetSelectionListener(tabSheet, delegate);
        return delegate;
    }

    private UrlQueryParametersBinder createDelegate(UrlQueryParametersFacet facet,
                                                    Tabs tabs, int minTabValue, int maxTabValue) {
        UrlQueryParametersBinder delegate = doCreateDelegate(facet, tabs, minTabValue, maxTabValue);
        addTabSheetSelectionListener(tabs, delegate);
        return delegate;
    }

    private UrlQueryParametersBinder doCreateDelegate(UrlQueryParametersFacet facet,
                                                      JmixTabSheet tabSheet, int minTabValue, int maxTabValue) {
        return createTabSheetBinder(facet, tabSheet::getSelectedIndex, tabSheet::setSelectedIndex, minTabValue, maxTabValue);
    }

    private UrlQueryParametersBinder doCreateDelegate(UrlQueryParametersFacet facet,
                                                      Tabs tabs, int minTabValue, int maxTabValue) {
        return createTabSheetBinder(facet, tabs::getSelectedIndex, tabs::setSelectedIndex, minTabValue, maxTabValue);
    }

    private static UrlQueryParametersBinder createTabSheetBinder(UrlQueryParametersFacet facet,
                                                                 Supplier<Integer> indexProvider,
                                                                 Consumer<Integer> indexConsumer,
                                                                 int minTabValue, int maxTabValue) {
        return UrlQueryParametersBinder.registerBinder(facet, () -> {
            final String selectedTab = String.valueOf(indexProvider.get());
            return QueryParameters.of(TAB_INDEX_QUERY_PARAMETER, selectedTab);
        }, qp -> {
            List<String> queryParameterValues = qp.getParameters().getOrDefault(TAB_INDEX_QUERY_PARAMETER, List.of());
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

    private void addTabSheetSelectionListener(JmixTabSheet tabSheet, UrlQueryParametersBinder delegate) {
        tabSheet.addSelectedChangeListener(e -> {
            if (e.isInitialSelection()) {
                return;
            }
            delegate.fireQueryParametersChanged();
        });
    }

    private void addTabSheetSelectionListener(Tabs tabs, UrlQueryParametersBinder delegate) {
        tabs.addSelectedChangeListener(e -> {
            if (e.isInitialSelection()) {
                return;
            }
            delegate.fireQueryParametersChanged();
        });
    }
}
