package com.company.crm.view.catalog;

import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

@Route(value = "category-items", layout = MainView.class)
@ViewController(id = "CategoryItem.list")
@ViewDescriptor(path = "category-item-list-view.xml")
@LookupComponent("categoryItemsDataGrid")
@DialogMode(width = "64em")
public class CategoryItemListView extends StandardListView<CategoryItem> {

    @Autowired
    private CategoryItemRepository repository;

    @Install(to = "categoryItemsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<CategoryItem> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, context).getContent();
    }

    @Install(to = "categoryItemsDataGrid.removeAction", subject = "delegate")
    private void categoryItemsDataGridRemoveDelegate(final Collection<CategoryItem> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(context);
    }
}