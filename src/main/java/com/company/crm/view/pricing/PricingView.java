package com.company.crm.view.pricing;

import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Function;

@Route(value = "pricing", layout = MainView.class)
@ViewController(id = "PricingView")
@ViewDescriptor(path = "pricing-view.xml")
public class PricingView extends StandardView {

    @Autowired
    private CrmRenderers crmRenderers;

    @Supply(to = "categoryItemsDataGrid.name", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridNameRenderer() {
        return crmRenderers.entityLink(Function.identity());
    }

    @Supply(to = "categoryItemsDataGrid.code", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridCodeRenderer() {
        return crmRenderers.categoryItemCode();
    }
}