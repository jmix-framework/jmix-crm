package com.company.crm.view.catalog;

import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.AccessManager;
import io.jmix.core.EntityStates;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.UiViewProperties;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.action.SecuredBaseAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.data.EntityValueSource;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.model.InstanceLoader;
import io.jmix.flowui.util.OperationResult;
import io.jmix.flowui.util.UnknownOperationResult;
import io.jmix.flowui.view.ChangeTrackerCloseAction;
import io.jmix.flowui.view.CloseAction;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.NavigateCloseAction;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.flowui.view.ViewValidation;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.jmix.core.repository.JmixDataRepositoryUtils.buildPageRequest;
import static io.jmix.core.repository.JmixDataRepositoryUtils.buildRepositoryContext;
import static io.jmix.core.repository.JmixDataRepositoryUtils.extractEntityId;
import static io.jmix.flowui.component.delegate.AbstractFieldDelegate.PROPERTY_INVALID;

// TODO: charts?
@Route(value = "categories", layout = MainView.class)
@ViewController(id = "Category.list")
@ViewDescriptor(path = "category-list-view.xml")
@LookupComponent("categoriesDataGrid")
@DialogMode(width = "64em")
public class CategoryListView extends StandardListView<Category> {

    @Autowired
    private CategoryRepository repository;

    @ViewComponent
    private DataContext dataContext;

    @ViewComponent
    private CollectionContainer<Category> categoriesDc;

    @ViewComponent
    private InstanceContainer<Category> categoryDc;

    @ViewComponent
    private InstanceLoader<Category> categoryDl;

    @ViewComponent
    private VerticalLayout listLayout;

    @ViewComponent
    private TreeDataGrid<Category> categoriesDataGrid;

    @ViewComponent
    private FormLayout form;

    @ViewComponent
    private HorizontalLayout detailActions;

    @Autowired
    private AccessManager accessManager;

    @Autowired
    private EntityStates entityStates;

    @Autowired
    private UiViewProperties uiViewProperties;

    @Autowired
    private ViewValidation viewValidation;

    @Autowired
    private UiComponentProperties uiComponentProperties;

    private boolean modifiedAfterEdit;

    @Subscribe
    public void onInit(final InitEvent event) {
        categoriesDataGrid.getActions().forEach(action -> {
            if (action instanceof SecuredBaseAction secured) {
                secured.addEnabledRule(() -> listLayout.isEnabled());
            }
        });
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        setupModifiedTracking();
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        updateControls(false);
    }

    @Subscribe
    private void onBeforeClose(final BeforeCloseEvent event) {
        preventUnsavedChanges(event);
    }

    @Subscribe("categoriesDataGrid.createAction")
    public void onCategoriesDataGridCreateAction(final ActionPerformedEvent event) {
        prepareFormForValidation();

        dataContext.clear();
        Category entity = dataContext.create(Category.class);
        categoryDc.setItem(entity);
        updateControls(true);
    }

    @Subscribe("categoriesDataGrid.editAction")
    public void onCategoriesDataGridEditAction(final ActionPerformedEvent event) {
        updateControls(true);
    }

    @Subscribe("saveButton")
    public void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        saveEditedEntity();
    }

    @Subscribe("cancelButton")
    public void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        if (!hasUnsavedChanges()) {
            discardEditedEntity();
            return;
        }

        if (uiViewProperties.isUseSaveConfirmation()) {
            viewValidation.showSaveConfirmationDialog(this)
                    .onSave(this::saveEditedEntity)
                    .onDiscard(this::discardEditedEntity);
        } else {
            viewValidation.showUnsavedChangesDialog(this)
                    .onDiscard(this::discardEditedEntity);
        }
    }

    @Subscribe(id = "categoriesDc", target = Target.DATA_CONTAINER)
    public void onCategoriesDcItemChange(final InstanceContainer.ItemChangeEvent<Category> event) {
        prepareFormForValidation();

        Category entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            categoryDl.setEntityId(EntityValues.getId(entity));
            categoryDl.load();
        } else {
            categoryDl.setEntityId(null);
            categoryDc.setItem(null);
        }
        updateControls(false);
    }

    private void prepareFormForValidation() {
        // all components shouldn't be readonly due to validation passing correctly
        UiComponentUtils.getComponents(form).forEach(component -> {
            if (component instanceof HasValueAndElement<?, ?> field) {
                field.setReadOnly(false);
            }
        });
    }

    private OperationResult saveEditedEntity() {
        Category item = categoryDc.getItem();
        ValidationErrors validationErrors = validateView(item);

        if (!validationErrors.isEmpty()) {
            viewValidation.showValidationErrors(validationErrors);
            viewValidation.focusProblemComponent(validationErrors);
            return OperationResult.fail();
        }

        dataContext.save();
        categoriesDc.replaceItem(item);
        updateControls(false);
        return OperationResult.success();
    }

    private void discardEditedEntity() {
        resetFormInvalidState();

        dataContext.clear();
        categoryDc.setItem(null);
        categoryDl.load();
        updateControls(false);
    }

    private void resetFormInvalidState() {
        UiComponentUtils.getComponents(form).forEach(component -> {
            if (component instanceof HasValidation hasValidation && hasValidation.isInvalid()) {
                component.getElement().setProperty(PROPERTY_INVALID, false);
                component.getElement().executeJs("this.invalid = $0", false);
            }
        });
    }

    private ValidationErrors validateView(Category entity) {
        ValidationErrors validationErrors = viewValidation.validateUiComponents(form);
        if (!validationErrors.isEmpty()) {
            return validationErrors;
        }
        validationErrors.addAll(viewValidation.validateBeanGroup(UiCrossFieldChecks.class, entity));
        return validationErrors;
    }

    private void updateControls(boolean editing) {
        UiComponentUtils.getComponents(form).forEach(component -> {
            if (component instanceof SupportsValueSource<?> valueSourceComponent
                    && valueSourceComponent.getValueSource() instanceof EntityValueSource<?, ?> entityValueSource
                    && component instanceof HasValueAndElement<?, ?> field) {
                field.setReadOnly(!editing || !isUpdatePermitted(entityValueSource));
            }
        });

        modifiedAfterEdit = false;
        detailActions.setVisible(editing);
        listLayout.setEnabled(!editing);
        categoriesDataGrid.getActions().forEach(Action::refreshState);

        if (!uiComponentProperties.isImmediateRequiredValidationEnabled() && editing) {
            resetFormInvalidState();
        }
    }

    private boolean isUpdatePermitted(EntityValueSource<?, ?> valueSource) {
        UiEntityAttributeContext context = new UiEntityAttributeContext(valueSource.getMetaPropertyPath());
        accessManager.applyRegisteredConstraints(context);
        return context.canModify();
    }

    private boolean hasUnsavedChanges() {
        for (Object modified : dataContext.getModified()) {
            if (!entityStates.isNew(modified)) {
                return true;
            }
        }

        return modifiedAfterEdit;
    }

    private void setupModifiedTracking() {
        dataContext.addChangeListener(this::onChangeEvent);
        dataContext.addPostSaveListener(this::onPostSaveEvent);
    }

    private void onChangeEvent(DataContext.ChangeEvent changeEvent) {
        modifiedAfterEdit = true;
    }

    private void onPostSaveEvent(DataContext.PostSaveEvent postSaveEvent) {
        modifiedAfterEdit = false;
    }

    private void preventUnsavedChanges(BeforeCloseEvent event) {
        CloseAction closeAction = event.getCloseAction();

        if (closeAction instanceof ChangeTrackerCloseAction trackerCloseAction
                && trackerCloseAction.isCheckForUnsavedChanges()
                && hasUnsavedChanges()) {
            UnknownOperationResult result = new UnknownOperationResult();

            if (closeAction instanceof NavigateCloseAction navigateCloseAction) {
                BeforeLeaveEvent beforeLeaveEvent = navigateCloseAction.getBeforeLeaveEvent();
                BeforeLeaveEvent.ContinueNavigationAction navigationAction = beforeLeaveEvent.postpone();

                if (uiViewProperties.isUseSaveConfirmation()) {
                    viewValidation.showSaveConfirmationDialog(this)
                            .onSave(() -> result.resume(navigateWithSave(navigationAction)))
                            .onDiscard(() -> result.resume(navigateWithDiscard(navigationAction)))
                            .onCancel(() -> {
                                result.otherwise(() -> cancelNavigation(navigationAction));
                                result.fail();
                            });
                } else {
                    viewValidation.showUnsavedChangesDialog(this)
                            .onDiscard(() -> result.resume(navigateWithDiscard(navigationAction)))
                            .onCancel(() -> {
                                result.otherwise(() -> cancelNavigation(navigationAction));
                                result.fail();
                            });
                }
            } else {
                if (uiViewProperties.isUseSaveConfirmation()) {
                    viewValidation.showSaveConfirmationDialog(this)
                            .onSave(() -> result.resume(closeWithSave()))
                            .onDiscard(() -> result.resume(closeWithDiscard()))
                            .onCancel(result::fail);
                } else {
                    viewValidation.showUnsavedChangesDialog(this)
                            .onDiscard(() -> result.resume(closeWithDiscard()))
                            .onCancel(result::fail);
                }
            }

            event.preventClose(result);
        }
    }

    private OperationResult navigateWithDiscard(BeforeLeaveEvent.ContinueNavigationAction navigationAction) {
        return navigate(navigationAction, StandardOutcome.DISCARD.getCloseAction());
    }

    private OperationResult navigateWithSave(BeforeLeaveEvent.ContinueNavigationAction navigationAction) {
        return saveEditedEntity()
                .compose(() -> navigate(navigationAction, StandardOutcome.SAVE.getCloseAction()));
    }

    private void cancelNavigation(BeforeLeaveEvent.ContinueNavigationAction navigationAction) {
        // Because of using React Router, we need to call
        // 'BeforeLeaveEvent.ContinueNavigationAction.cancel'
        // explicitly, otherwise navigation process hangs
        navigationAction.cancel();
    }

    private OperationResult navigate(BeforeLeaveEvent.ContinueNavigationAction navigationAction,
                                     CloseAction closeAction) {
        navigationAction.proceed();

        AfterCloseEvent afterCloseEvent = new AfterCloseEvent(this, closeAction);
        fireEvent(afterCloseEvent);

        return OperationResult.success();
    }

    private OperationResult closeWithSave() {
        return saveEditedEntity()
                .compose(() -> close(StandardOutcome.SAVE));
    }

    @Install(to = "categoriesDl", target = Target.DATA_LOADER)
    private List<Category> listLoadDelegate(LoadContext<Category> context) {
        return repository.findAll(buildPageRequest(context), buildRepositoryContext(context)).getContent();
    }

    @Install(to = "categoriesDataGrid.removeAction", subject = "delegate")
    private void categoriesDataGridRemoveDelegate(final Collection<Category> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "categoryDl", target = Target.DATA_LOADER)
    private Category detailLoadDelegate(LoadContext<Category> context) {
        return repository.getById(extractEntityId(context), context.getFetchPlan());
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(repository.save(categoryDc.getItem()));
    }
}