package com.company.crm.view.order;

import com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder;
import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.util.AsyncTasksRegistry;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.Messages;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.textfield.TypedTextField;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.company.crm.app.util.ui.CrmUiUtils.setCursorPointer;
import static com.company.crm.app.util.ui.color.StatusColors.getBackgroundClass;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.greaterOrEqual;
import static io.jmix.core.querycondition.PropertyCondition.lessOrEqual;
import static io.jmix.core.querycondition.PropertyCondition.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

@Route(value = "orders", layout = MainView.class)
@ViewController(id = "Order.list")
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "64em")
public class OrderListView extends StandardListView<Order> {

    @Autowired
    private Messages messages;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private OrderRepository repository;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;

    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private CollectionLoader<Client> clientsDl;
    @ViewComponent
    private CollectionContainer<Client> clientsDc;

    @ViewComponent
    private TypedTextField<Object> searchField;
    @ViewComponent
    private EntityComboBox<Client> clientComboBox;
    @ViewComponent
    private TypedDatePicker<LocalDate> fromDatePicker;
    @ViewComponent
    private TypedDatePicker<LocalDate> toDatePicker;
    @ViewComponent
    private FormLayout pipeLineFilter;

    private static final String STATUS_CONTAINER_COMPONENT_ID_PREFIX = "status_container_";
    private static final String STATUS_TEXT_COMPONENT_ID_PREFIX = "status_text_";

    private final LogicalCondition filtersCondition = LogicalCondition.and();
    private final AsyncTasksRegistry asyncTasksRegistry = AsyncTasksRegistry.newInstance();

    private Optional<OrderStatus> selectedStatus = Optional.empty();

    @Subscribe
    private void onInit(final InitEvent event) {
        loadData();
        initializeFilterFields();
        addDetachListener(e -> asyncTasksRegistry.cancelAll());
    }

    @Install(to = "ordersDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Order> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, wrapContext(context)).getContent();
    }

    @Install(to = "ordersDataGrid.removeAction", subject = "delegate")
    private void ordersDataGridRemoveDelegate(final Collection<Order> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(wrapContext(context));
    }

    @Supply(to = "ordersDataGrid.client", subject = "renderer")
    private Renderer<Order> ordersDataGridClientRenderer() {
        return crmRenderers.orderClientLink();
    }

    @Supply(to = "ordersDataGrid.status", subject = "renderer")
    private Renderer<Order> ordersDataGridStatusRenderer() {
        return crmRenderers.orderStatus();
    }

    private void initializeFilterFields() {
        initializePipelineFilter();

        List.<HasValue<?, ?>>of(searchField, clientComboBox, fromDatePicker, toDatePicker)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(searchField)
                .addComboboxBinding(clientComboBox, clientsDc.getItems())
                .addDatePickerBinding(fromDatePicker)
                .addDatePickerBinding(toDatePicker)
                .build();

        SimpleUrlQueryParametersBinder.registerBinder(this,
                () -> QueryParameters.of("selected_status",
                        selectedStatus.map(s -> s.getId().toString()).orElse("")),
                qp -> qp.getSingleParameter("selected_status").ifPresent(id ->
                        selectedStatus = Optional.ofNullable(OrderStatus.fromStringId(id))));
    }

    private void loadData() {
        ordersDl.load();
        clientsDl.load();
    }

    private JmixDataRepositoryContext wrapContext(JmixDataRepositoryContext context) {
        LogicalCondition resultCondition;
        if (context.condition() != null) {
            resultCondition = LogicalCondition.and(context.condition(), filtersCondition);
        } else {
            resultCondition = filtersCondition;
        }
        return new JmixDataRepositoryContext(context.fetchPlan(), resultCondition, context.hints());
    }

    private void applyFilters() {
        updateFiltersCondition();
        ordersDl.load();
    }

    private void updateFiltersCondition() {
        filtersCondition.getConditions().clear();
        addSearchConditions();
    }

    private void addSearchConditions() {
        addSearchBySelectedStatus();
        addSearchByNumberCondition();
        addSearchByClientCondition();
        addDateRangeConditions();
    }

    private void addDateRangeConditions() {
        addSearchByFromDateCondition();
        addSearchByToDateCondition();
    }

    private void addSearchBySelectedStatus() {
        selectedStatus.ifPresent(status ->
                filtersCondition.add(equal("status", status)));
    }

    private void addSearchByNumberCondition() {
        searchField.getOptionalValue().ifPresent(name ->
                filtersCondition.add(equal("number", name)));
    }

    private void addSearchByClientCondition() {
        clientComboBox.getOptionalValue().ifPresent(client ->
                filtersCondition.add(equal("client", client)));
    }

    private void addSearchByFromDateCondition() {
        fromDatePicker.getOptionalValue().ifPresent(fromDate ->
                filtersCondition.add(greaterOrEqual("date", fromDate)));
    }

    private void addSearchByToDateCondition() {
        toDatePicker.getOptionalValue().ifPresent(fromDate ->
                filtersCondition.add(lessOrEqual("date", fromDate)));
    }

    private void initializePipelineFilter() {
        for (OrderStatus status : OrderStatus.values()) {
            Integer statusId = status.getId();

            String textComponentId = STATUS_TEXT_COMPONENT_ID_PREFIX + statusId;
            H3 statusTextComponent = new H3(messages.getMessage(status));
            statusTextComponent.setId(textComponentId);

            String containerComponentId = STATUS_CONTAINER_COMPONENT_ID_PREFIX + statusId;
            HorizontalLayout statusContainer = new HorizontalLayout(statusTextComponent);
            statusContainer.setId(containerComponentId);
            statusContainer.setWidth(10, Unit.EM);
            statusContainer.setHeight(2, Unit.EM);
            statusContainer.setAlignItems(FlexComponent.Alignment.CENTER);
            statusContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
            statusContainer.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.FULL);
            statusContainer.addClickListener(e -> onStatusFilterClick(status, statusContainer));
            setCursorPointer(statusContainer);

            pipeLineFilter.add(statusContainer);
        }
        calculateAmountForEachOrderStatus();
    }

    private void onStatusFilterClick(OrderStatus status, HorizontalLayout statusContainer) {
        Optional<OrderStatus> statusOpt = Optional.of(status);
        statusContainer.addClassName(getBackgroundClass(status));
        if (selectedStatus.equals(statusOpt)) {
            selectedStatus = Optional.empty();
        } else {
            selectedStatus = statusOpt;
        }
        pipeLineFilter.getChildren().forEach(child -> {
            if (selectedStatus.isEmpty() || !child.equals(statusContainer)) {
                child.getClassNames().clear();
                child.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.FULL);
            }
        });
        applyFilters();
    }

    private void calculateAmountForEachOrderStatus() {
        asyncTasksRegistry.placeTask("orderByStatusTask", uiAsyncTasks
                .supplierConfigurer(() -> orderService.getOrdersAmountByStatus())
                .withResultHandler(this::updateAmountForEachOrderStatus));
    }

    private void updateAmountForEachOrderStatus(Map<OrderStatus, BigDecimal> result) {
        result.forEach((status, amount) -> {
            String statusTextComponentId = STATUS_TEXT_COMPONENT_ID_PREFIX + status.getId();
            UiComponentUtils.findComponent(pipeLineFilter, statusTextComponentId)
                    .ifPresent(statusTextComponent -> {
                        Element element = statusTextComponent.getElement();
                        element.setText(element.getText() + " (" + amount + ")");
                    });
        });
    }
}