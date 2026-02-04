package com.company.crm.view.catalog;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.grid.dnd.GridDragEndEvent;
import com.vaadin.flow.component.grid.dnd.GridDragStartEvent;
import com.vaadin.flow.component.grid.dnd.GridDropEvent;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.editor.EditorCloseEvent;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.Messages;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.list.EditAction;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.grid.editor.DataGridEditor;
import io.jmix.flowui.component.grid.editor.EditComponentGenerationContext;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Route(value = "categories", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CATEGORY_LIST)
@ViewDescriptor(path = "category-list-view.xml")
@LookupComponent("categoriesDataGrid")
@DialogMode(width = "64em", resizable = true)
public class CategoryListView extends StandardListView<Category> {

    @Autowired
    private DataManager dataManager;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private CategoryRepository categoryRepository;

    @ViewComponent
    private CollectionContainer<Category> categoriesDc;
    @ViewComponent
    private TreeDataGrid<Category> categoriesDataGrid;
    @Autowired
    private Messages messages;
    @ViewComponent("categoriesDataGrid.editAction")
    private EditAction<Category> editAction;
    @ViewComponent
    private CollectionLoader<Category> categoriesDl;

    private Category draggedCategory;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        categoriesDl.load();
    }

    @Subscribe
    public void onInit(InitEvent event) {
        configureInlineEdit();
    }

    @Install(to = "categoriesDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Category> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return categoryRepository.findAll(pageable, context).getContent();
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return categoryRepository.count(context);
    }

    @Subscribe("categoriesDataGrid.editAction")
    private void onCategoriesDataGridEditAction(final ActionPerformedEvent event) {
        startOrStopEditSelectedItem();
    }

    @Install(to = "categoriesDataGrid.removeAction", subject = "delegate")
    private void categoriesDataGridRemoveDelegate(final Collection<Category> collection) {
        categoryRepository.deleteAll(collection);
    }

    @Install(to = "categoriesDataGrid.@editor", subject = "closeListener")
    private void categoriesDataGridEditorCloseListener(final EditorCloseEvent<Category> event) {
        categoriesDc.replaceItem(dataManager.save(event.getItem()));
    }

    @Supply(to = "categoriesDataGrid.code", subject = "renderer")
    private Renderer<Category> categoriesDataGridCodeRenderer() {
        return crmRenderers.categoryCode();
    }

    @Install(to = "categoriesDataGrid.createAction", subject = "newEntitySupplier")
    private Category categoriesDataGridCreateActionNewEntitySupplier() {
        Category category = dataManager.create(Category.class);
        Category parent = categoriesDataGrid.getSingleSelectedItem();
        if (parent != null) {
            category.setParent(parent);
        }
        return category;
    }

    @Install(to = "categoriesDataGrid.createAction", subject = "afterSaveHandler")
    private void categoriesDataGridCreateActionAfterSaveHandler(final Category category) {
        Category parent = category.getParent();
        if (parent != null) {
            categoriesDataGrid.expand(parent);
        }
        categoriesDataGrid.select(category);
    }

    private void configureInlineEdit() {
        addGridDoubleClickListener();
        installDefaultStringEditorComponent("code", "name", "description");
        addEditorListeners();
        addDragAndDropSupport();
    }

    private void addGridDoubleClickListener() {
        categoriesDataGrid.addItemDoubleClickListener(e -> editItem(e.getItem()));
    }

    private void addEditorListeners() {
        DataGridEditor<Category> editor = categoriesDataGrid.getEditor();
        editor.addOpenListener(e -> {
            editAction.setText(messages.getMessage("actions.Cancel"));
            editAction.setIcon(VaadinIcon.BAN.create());
        });
        editor.addCloseListener(e -> {
            editAction.setText(messages.getMessage("actions.Edit"));
            editAction.setIcon(VaadinIcon.PENCIL.create());
        });
    }

    private void addDragAndDropSupport() {
        categoriesDataGrid.addDragStartListener(this::onDragStart);
        categoriesDataGrid.addDropListener(this::onDrop);
        categoriesDataGrid.addDragEndListener(this::onDragEnd);
    }

    private void onDragStart(GridDragStartEvent<Category> event) {
        draggedCategory = event.getDraggedItems().stream().findFirst().orElse(null);
    }

    private void onDrop(GridDropEvent<Category> event) {
        if (draggedCategory == null) {
            return;
        }

        if (categoriesDataGrid.getEditor().isOpen()) {
            cancelEditSelectedItem();
        }

        Category target = event.getDropTargetItem().orElse(null);
        GridDropLocation location = event.getDropLocation();

        if (target != null && Objects.equals(target, draggedCategory)) {
            return;
        }

        Category newParent = resolveDropParent(target, location);
        if (isInvalidParent(draggedCategory, newParent)) {
            return;
        }

        if (Objects.equals(draggedCategory.getParent(), newParent)) {
            return;
        }

        draggedCategory.setParent(newParent);
        Category saved = dataManager.save(draggedCategory);

        if (newParent != null) {
            categoriesDataGrid.expand(newParent);
        }
        categoriesDataGrid.select(saved);

        categoriesDl.load();
    }

    private void onDragEnd(GridDragEndEvent<Category> event) {
        draggedCategory = null;
    }

    private Category resolveDropParent(Category target, GridDropLocation location) {
        if (target == null) {
            return null;
        }
        return location == GridDropLocation.ON_TOP ? target : target.getParent();
    }

    private boolean isInvalidParent(Category category, Category newParent) {
        if (newParent == null) {
            return false;
        }
        if (Objects.equals(category, newParent)) {
            return true;
        }
        Category cursor = newParent.getParent();
        while (cursor != null) {
            if (Objects.equals(cursor, category)) {
                return true;
            }
            cursor = cursor.getParent();
        }
        return false;
    }

    private void installDefaultStringEditorComponent(String... columns) {
        for (String column : columns) {
            categoriesDataGrid.getEditor()
                    .setColumnEditorComponent(column, ctx -> getDefaultStringEditor(column, ctx));
        }
    }

    private void startOrStopEditSelectedItem() {
        Category selectedItem = categoriesDataGrid.getSingleSelectedItem();
        DataGridEditor<Category> editor = categoriesDataGrid.getEditor();

        if (editor.isOpen()) {
            editor.save();
            editor.closeEditor();
            categoriesDataGrid.deselectAll();
        } else {
            if (selectedItem != null) {
                editItem(selectedItem);
            }
        }
    }

    private void cancelEditSelectedItem() {
        DataGridEditor<Category> editor = categoriesDataGrid.getEditor();
        if (editor.isOpen()) {
            editor.cancel();
            editor.closeEditor();
        }
    }

    private void editItem(Category selectedItem) {
        categoriesDataGrid.select(selectedItem);
        categoriesDataGrid.getEditor().editItem(selectedItem);
    }

    private Component getDefaultStringEditor(
            String column, EditComponentGenerationContext<Category> ctx) {
        @SuppressWarnings("unchecked")
        TypedTextField<String> component = uiComponents.create(TypedTextField.class);
        component.setWidthFull();
        component.setValueSource(ctx.getValueSourceProvider().getValueSource(column));
        component.addKeyDownListener(Key.ENTER, e -> startOrStopEditSelectedItem());
        component.addKeyDownListener(Key.ESCAPE, e -> cancelEditSelectedItem());
        if ("name".equals(column)) {
            component.focus();
        }
        return component;
    }
}
