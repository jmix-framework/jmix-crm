package com.company.crm.view.catalog;

import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.grid.editor.EditorCloseEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.grid.editor.DataGridEditor;
import io.jmix.flowui.component.grid.editor.EditComponentGenerationContext;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

@Route(value = "categories", layout = MainView.class)
@ViewController(id = "Category.list")
@ViewDescriptor(path = "category-list-view.xml")
@LookupComponent("categoriesDataGrid")
@DialogMode(width = "64em")
public class CategoryListView extends StandardListView<Category> {

    @Autowired
    private DataManager dataManager;
    @Autowired
    private CategoryRepository repository;

    @ViewComponent
    private CollectionContainer<Category> categoriesDc;
    @ViewComponent
    private TreeDataGrid<Category> categoriesDataGrid;
    @Autowired
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(InitEvent event) {
        configureInlineEdit();
    }

    @Install(to = "categoriesDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Category> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, context).getContent();
    }

    @Subscribe("categoriesDataGrid.editAction")
    private void onCategoriesDataGridEditAction(final ActionPerformedEvent event) {
        startOrStopEditSelectedItem();
    }

    @Install(to = "categoriesDataGrid.removeAction", subject = "delegate")
    private void categoriesDataGridRemoveDelegate(final Collection<Category> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(context);
    }

    @Install(to = "categoriesDataGrid.@editor", subject = "closeListener")
    private void categoriesDataGridEditorCloseListener(final EditorCloseEvent<Category> event) {
        categoriesDc.replaceItem(dataManager.save(event.getItem()));
    }

    private void configureInlineEdit() {
        addGridDoubleClickListener();
        installDefaultStringEditorComponent("code", "name", "description");
    }

    private void addGridDoubleClickListener() {
        categoriesDataGrid.addItemDoubleClickListener(e -> editItem(e.getItem()));
    }

    private void installDefaultStringEditorComponent(String... columns) {
        for (String column : columns) {
            categoriesDataGrid.getEditor()
                    .setColumnEditorComponent(column, ctx -> getDefaultStringEditor(column, ctx));
        }
    }

    private void startOrStopEditSelectedItem() {
        DataGridEditor<Category> editor = categoriesDataGrid.getEditor();
        if (editor.isOpen()) {
            editor.save();
            editor.closeEditor();
            categoriesDataGrid.deselectAll();
        } else {
            Category selectedItem = categoriesDataGrid.getSingleSelectedItem();
            if (selectedItem != null) {
                editItem(selectedItem);
            }
        }
    }

    private void editItem(Category selectedItem) {
        categoriesDataGrid.getEditor().editItem(selectedItem);
    }

    private Component getDefaultStringEditor(
            String column, EditComponentGenerationContext<Category> ctx) {
        TypedTextField<String> component = uiComponents.create(TypedTextField.class);
        component.setWidthFull();
        component.setValueSource(ctx.getValueSourceProvider().getValueSource(column));
        component.addKeyDownListener(Key.ENTER, e -> startOrStopEditSelectedItem());
        if ("name".equals(column)) {
            component.focus();
        }
        return component;
    }
}