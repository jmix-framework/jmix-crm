package com.company.crm.view.catalog;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Views;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "catalog", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CATALOG)
@ViewDescriptor(path = "catalog-view.xml")
public class CatalogView extends StandardView {
    @Autowired
    private Views views;
    @ViewComponent
    private JmixTabSheet tabSheet;
    @ViewComponent
    private MessageBundle messageBundle;

    @Subscribe
    public void onInit(final InitEvent event) {
        var categoriesView = views.create(CategoryListView.class);
        var categoryItemsView = views.create(CategoryItemListView.class);

        tabSheet.add(messageBundle.getMessage("categoryItems"), categoryItemsView);
        tabSheet.add(messageBundle.getMessage("categories"), categoriesView);

        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
    }
}