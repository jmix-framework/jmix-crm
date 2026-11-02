package com.company.crm.test.main;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.view.catalog.CategoryListView;
import com.company.crm.view.category.CategoryDetailView;
import com.company.crm.view.client.ClientDetailView;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteParameters;
import io.jmix.flowui.view.StandardDetailView;
import org.junit.jupiter.api.Test;

class MainViewNavigationTest extends AbstractUiTest {

    @Test
    void cancelDirectlyOpenedDetailReturnsToMainView() {
        UI.getCurrent().navigate(CategoryDetailView.class,
                new RouteParameters(StandardDetailView.DEFAULT_ROUTE_PARAM, StandardDetailView.NEW_ENTITY_ID));
        assertCurrentView(CategoryDetailView.class);

        viewTestSupport.click("closeButton");

        assertCurrentView(MainView.class);
    }

    @Test
    void closeDirectlyOpenedClientReturnsToMainView() {
        var client = entities.client("Direct navigation client");
        UI.getCurrent().navigate(ClientDetailView.class,
                new RouteParameters(StandardDetailView.DEFAULT_ROUTE_PARAM, client.getId().toString()));
        assertCurrentView(ClientDetailView.class);

        viewTestSupport.click("closeButton");

        assertCurrentView(MainView.class);
    }

    @Test
    void cancelDetailWithNavigationHistoryReturnsToPreviousView() {
        viewTestSupport.navigateTo(CategoryListView.class);
        viewTestSupport.navigateToNewEntityDetail(Category.class, CategoryDetailView.class);

        viewTestSupport.click("closeButton");

        assertCurrentView(CategoryListView.class);
    }
}
