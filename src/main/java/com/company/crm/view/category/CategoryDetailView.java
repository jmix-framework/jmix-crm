package com.company.crm.view.category;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

@Route(value = "categories/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CATEGORY_DETAIL)
@ViewDescriptor(path = "category-detail-view.xml")
@EditedEntityContainer("categoryDc")
public class CategoryDetailView extends StandardDetailView<Category> {

    @Autowired
    private CategoryRepository categoryRepository;

    @Install(to = "categoryDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Category> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return categoryRepository.findById(id, fetchPlan);
    }
}