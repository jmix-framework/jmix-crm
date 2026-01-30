package com.company.crm.view.usertask;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.user.User;
import com.company.crm.model.user.task.UserTask;
import com.company.crm.model.user.task.UserTaskRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.LoadContext;
import io.jmix.core.SaveContext;
import io.jmix.core.Sort;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.querycondition.PropertyCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.validation.group.UiCrossFieldChecks;
import io.jmix.flowui.UiComponentProperties;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.UiViewProperties;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.action.SecuredBaseAction;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.genericfilter.GenericFilter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.data.EntityValueSource;
import io.jmix.flowui.data.SupportsValueSource;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
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
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.flowui.view.ViewValidation;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.company.crm.app.util.ui.datacontext.DataContextUtils.addCondition;
import static io.jmix.core.repository.JmixDataRepositoryUtils.buildPageRequest;
import static io.jmix.core.repository.JmixDataRepositoryUtils.buildRepositoryContext;
import static io.jmix.core.repository.JmixDataRepositoryUtils.extractEntityId;
import static io.jmix.flowui.component.delegate.AbstractFieldDelegate.PROPERTY_INVALID;

@Route(value = "user-tasks", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.USER_TASK_LIST)
@ViewDescriptor(path = "user-task-list-view.xml")
@LookupComponent("userTasksDataGrid")
@DialogMode(width = "90%", resizable = true, closeOnOutsideClick = true, closeOnEsc = true)
public class UserTaskListView extends StandardListView<UserTask> {

    @Autowired
    private UserTaskRepository userTaskRepository;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private EntityStates entityStates;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private UiViewProperties uiViewProperties;
    @Autowired
    private UiComponentProperties uiComponentProperties;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private DataContext dataContext;
    @ViewComponent
    private CollectionLoader<UserTask> userTasksDl;
    @ViewComponent
    private InstanceContainer<UserTask> userTaskDc;
    @ViewComponent
    private CollectionContainer<UserTask> userTasksDc;
    @ViewComponent
    private InstanceLoader<UserTask> userTaskDl;

    @ViewComponent
    private FormLayout form;
    @ViewComponent
    private VerticalLayout listLayout;
    @ViewComponent
    private VerticalLayout detailsLayout;
    @ViewComponent
    private TypedTextField<String> titleField;
    @ViewComponent
    private DataGrid<UserTask> userTasksDataGrid;
    @ViewComponent
    private HorizontalLayout detailActions;
    @ViewComponent
    private HorizontalLayout buttonsPanel;
    @ViewComponent
    private JmixFormLayout layoutWrapper;
    @ViewComponent
    private GenericFilter genericFilter;

    private boolean modifiedAfterEdit;

    @Nullable
    private Boolean gridOnly = null;

    public void reloadData() {
        userTasksDl.load();
    }

    public void setPadding(boolean padding) {
        getContent().setPadding(padding);
    }

    public void setMaxHeight(float height, Unit unit) {
        getContent().setMaxHeight(height, unit);
        userTasksDataGrid.setMaxHeight(height, unit);
    }

    public UserTaskListView detailOnly() {
        this.gridOnly = false;
        processGridOnly();
        return this;
    }

    public UserTaskListView gridOnly() {
        this.gridOnly = true;
        processGridOnly();
        return this;
    }

    private void processGridOnly() {
        if (gridOnly == null) {
            return;
        }

        Dialog dialog = UiComponentUtils.findDialog(this);
        if (dialog != null) {
            dialog.setMaxWidth(42, Unit.EM);
        }

        layoutWrapper.setAutoResponsive(true);
        layoutWrapper.setMaxColumns(1);
        layoutWrapper.setColumnWidth(100, Unit.PERCENTAGE);

        listLayout.setPadding(!gridOnly);
        listLayout.setVisible(gridOnly);
        listLayout.setHeight(getContent().getHeight());
        listLayout.setMaxHeight(getContent().getMaxHeight());

        genericFilter.setVisible(false);
        detailsLayout.setVisible(!gridOnly);
        buttonsPanel.setVisible(!gridOnly);

        if (gridOnly) {
            listLayout.setWidthFull();
        } else {
            listLayout.setWidth(null);
        }
    }

    @Subscribe
    private void onInit(final InitEvent event) {
        userTasksDataGrid.addItemDoubleClickListener(e -> {
            if (!isGridOnlyMode() && listLayout.isEnabled()) {
                updateControls(true);
            }
        });
        userTasksDataGrid.getActions().forEach(action -> {
            if (action instanceof SecuredBaseAction secured) {
                secured.addEnabledRule(() -> listLayout.isEnabled());
            }
        });
    }

    @Subscribe
    private void onReady(final ReadyEvent event) {
        setupModifiedTracking();
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        DataGridItems<UserTask> gridItems = userTasksDataGrid.getItems();
        if (gridItems != null) {
            gridItems.getItems().stream()
                    .findFirst()
                    .ifPresent(userTask -> userTasksDataGrid.select(userTask));
        }
        processGridOnly();
        updateControls(false);
        processDetailsMode();
    }

    private void processDetailsMode() {
        if (isDetailsOnlyMode()) {
            createAndEditNewTask();
        }
    }

    @Subscribe
    private void onBeforeClose(final BeforeCloseEvent event) {
        preventUnsavedChanges(event);
    }

    @Subscribe("userTasksDataGrid.createAction")
    private void onUserTasksDataGridCreateAction(final ActionPerformedEvent event) {
        createAndEditNewTask();
    }

    private void createAndEditNewTask() {
        prepareFormForValidation();
        dataContext.clear();
        UserTask task = dataContext.create(UserTask.class);
        task.setIsCompleted(false);
        task.setAuthor(getCurrentUser());
        task.setDueDate(LocalDate.now());
        userTaskDc.setItem(task);
        updateControls(true);
    }

    @Subscribe("userTasksDataGrid.editAction")
    private void onUserTasksDataGridEditAction(final ActionPerformedEvent event) {
        updateControls(true);
    }

    @Subscribe("saveButton")
    private void onSaveButtonClick(final ClickEvent<JmixButton> event) {
        saveEditedEntity().then(() -> closeViewIfDetailsMode(StandardOutcome.SAVE));
    }

    @Subscribe("cancelButton")
    private void onCancelButtonClick(final ClickEvent<JmixButton> event) {
        if (isDetailsOnlyMode()) {
            if (hasUnsavedChanges()) {
                discardEditedEntity();
            }
            closeViewIfDetailsMode(StandardOutcome.CLOSE);
            return;
        }

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

    @Subscribe(id = "userTasksDc", target = Target.DATA_CONTAINER)
    public void onUserTasksDcItemChange(final InstanceContainer.ItemChangeEvent<UserTask> event) {
        prepareFormForValidation();

        UserTask entity = event.getItem();
        dataContext.clear();
        if (entity != null) {
            userTaskDl.setEntityId(EntityValues.getId(entity));
            userTaskDl.load();
        } else {
            userTaskDl.setEntityId(null);
            userTaskDc.setItem(null);
        }
        updateControls(false);
    }

    @Install(to = "userTasksDl", target = Target.DATA_LOADER)
    private List<UserTask> listLoadDelegate(LoadContext<UserTask> context) {
        var repositoryContext = prepareTasksLoaderRepositoryContext(context);
        return userTaskRepository.findAll(buildPageRequest(context), repositoryContext).getContent();
    }

    @Install(to = "userTasksDataGrid.removeAction", subject = "delegate")
    private void userTasksDataGridRemoveDelegate(final Collection<UserTask> collection) {
        userTaskRepository.deleteAll(collection);
    }

    @Supply(to = "userTasksDataGrid.isCompleted", subject = "renderer")
    private Renderer<UserTask> userTasksDataGridIsCompletedRenderer() {
        return new ComponentRenderer<>(userTask -> {
            var editor = uiComponents.create(JmixCheckbox.class);
            editor.setValue(userTask.getIsCompleted());
            editor.addValueChangeListener(e -> {
                userTask.setIsCompleted(e.getValue());
                dataManager.save(userTask);
                reloadData();
            });

            var layout = new HorizontalLayout();
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.add(editor);

            return layout;
        });
    }

    @Install(to = "userTaskDl", target = Target.DATA_LOADER)
    private UserTask detailLoadDelegate(LoadContext<UserTask> context) {
        return userTaskRepository.getById(extractEntityId(context), context.getFetchPlan());
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<UserTask> saveDelegate(SaveContext saveContext) {
        return Set.of(userTaskRepository.save(userTaskDc.getItem()));
    }

    @Supply(to = "userTasksDataGrid.dueDate", subject = "renderer")
    private Renderer<UserTask> userTasksDataGridDueDateRenderer() {
        return crmRenderers.taskDueDateRenderer();
    }

    private JmixDataRepositoryContext prepareTasksLoaderRepositoryContext(LoadContext<UserTask> context) {
        context.getQuery().setSort(Sort.by(Sort.Direction.DESC, "dueDate"));
        JmixDataRepositoryContext repositoryContext = buildRepositoryContext(context);
        repositoryContext = addCondition(repositoryContext, PropertyCondition.equal("author", getCurrentUser()));
        return repositoryContext;
    }

    private User getCurrentUser() {
        return (User) currentAuthentication.getUser();
    }

    private void closeViewIfDetailsMode(StandardOutcome outcome) {
        if (isDetailsOnlyMode()) {
            close(outcome);
        }
    }

    private boolean isDetailsOnlyMode() {
        return gridOnly != null && !gridOnly;
    }

    private boolean isGridOnlyMode() {
        return gridOnly != null && gridOnly;
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
        UserTask item = userTaskDc.getItem();
        ValidationErrors validationErrors = validateView(item);

        if (!validationErrors.isEmpty()) {
            viewValidation.showValidationErrors(validationErrors);
            viewValidation.focusProblemComponent(validationErrors);
            return OperationResult.fail();
        }

        dataContext.save();
        userTasksDc.replaceItem(item);
        updateControls(false);
        return OperationResult.success();
    }

    private void discardEditedEntity() {
        resetFormInvalidState();

        dataContext.clear();
        userTaskDc.setItem(null);
        userTaskDl.load();
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

    private ValidationErrors validateView(UserTask entity) {
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
        userTasksDataGrid.getActions().forEach(Action::refreshState);

        if (!uiComponentProperties.isImmediateRequiredValidationEnabled() && editing) {
            resetFormInvalidState();
        }

        if (editing) {
            titleField.focus();
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
        if (isGridOnlyMode()) {
            return;
        }

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
}