package com.company.crm.view.catalog;

import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Route(value = "category-items/:id", layout = MainView.class)
@ViewController(id = "CategoryItem.detail")
@ViewDescriptor(path = "category-item-detail-view.xml")
@EditedEntityContainer("categoryItemDc")
public class CategoryItemDetailView extends StandardDetailView<CategoryItem> {

    @Autowired
    private CategoryItemRepository repository;

    @Install(to = "categoryItemDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<CategoryItem> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return repository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(repository.save(getEditedEntity()));
    }
}