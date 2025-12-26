package com.company.crm.view.order;

import com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder;
import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.ui.component.OrderStatusPipeline;
import com.company.crm.app.ui.component.OrderStatusPipeline.OrderStatusComponent;
import com.company.crm.app.util.AsyncTasksRegistry;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.CrmUiUtils;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.PrimaryListView;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.company.crm.app.util.ui.CrmUiUtils.addColumnHeaderCurrencySuffix;
import static com.company.crm.app.util.ui.datacontext.DataContextUtils.wrapCondition;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.greaterOrEqual;
import static io.jmix.core.querycondition.PropertyCondition.lessOrEqual;

@Route(value = "orders", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.ORDER_LIST)
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "64em")
@PrimaryListView(Order.class)
public class OrderListView extends StandardListView<Order> {

    @Autowired
    private Messages messages;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private OrderRepository orderRepository;
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
    private OrderStatusPipeline pipeLineFilter;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;

    private final LogicalCondition filtersCondition = LogicalCondition.and();
    private final AsyncTasksRegistry asyncTasksRegistry = AsyncTasksRegistry.newInstance();

    private Optional<OrderStatus> selectedStatus = Optional.empty();
    private SimpleUrlQueryParametersBinder selectedStatusUrlParameterBinder;

    @Subscribe
    private void onInit(final InitEvent event) {
        addColumnHeaderCurrencySuffix(ordersDataGrid, "total", "paid", "leftOver");
        clientsDl.load();
        registerUrlQueryParametersBinders();
        addDetachListener(e -> asyncTasksRegistry.cancelAll());
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        initializeFilterFields();
        applyFilters();
    }

    @Install(to = "ordersDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Order> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return orderRepository.findAll(pageable, wrapCondition(context, filtersCondition)).getContent();
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return orderRepository.count(wrapCondition(context, filtersCondition));
    }

    @Install(to = "ordersDataGrid.removeAction", subject = "delegate")
    private void ordersDataGridRemoveDelegate(final Collection<Order> collection) {
        orderRepository.deleteAll(collection);
    }

    @Supply(to = "ordersDataGrid.client", subject = "renderer")
    private Renderer<Order> ordersDataGridClientRenderer() {
        return crmRenderers.orderClientLink();
    }

    @Supply(to = "ordersDataGrid.status", subject = "renderer")
    private Renderer<Order> ordersDataGridStatusRenderer() {
        return crmRenderers.orderStatus();
    }

    @Supply(to = "ordersDataGrid.total", subject = "renderer")
    private Renderer<Order> ordersDataGridTotalRenderer() {
        return new TextRenderer<>(order ->
                PriceDataType.formatWithoutCurrency(order.getTotal()));
    }

    @Supply(to = "ordersDataGrid.paid", subject = "renderer")
    private Renderer<Order> ordersDataGridPaidRenderer() {
        return new TextRenderer<>(order ->
                PriceDataType.formatWithoutCurrency(orderService.getOrderPaymentsSum(order)));
    }

    @Supply(to = "ordersDataGrid.leftOver", subject = "renderer")
    private Renderer<Order> ordersDataGridLeftOverRenderer() {
        return new ComponentRenderer<>(order -> {
            BigDecimal leftOverSum = orderService.getOrderLeftOverSum(order);
            Span span = new Span(PriceDataType.formatWithoutCurrency(leftOverSum));

            if (leftOverSum.compareTo(BigDecimal.valueOf(10_000)) > 0) {
                CrmUiUtils.setBadge(span, CrmUiUtils.ERROR_BADGE);
            } else if (leftOverSum.compareTo(BigDecimal.ZERO) > 0) {
                CrmUiUtils.setBadge(span, CrmUiUtils.WARNING_BADGE);
            } else {
                CrmUiUtils.setBadge(span, CrmUiUtils.SUCCESS_BADGE);
                span.setText(messages.getMessage("paid"));
            }

            return span;
        });
    }

    private void initializeFilterFields() {
        initializePipelineFilter();
        List.<HasValue<?, ?>>of(searchField, clientComboBox, fromDatePicker, toDatePicker)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));
    }

    private void registerUrlQueryParametersBinders() {
        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(searchField)
                .addComboboxBinding(clientComboBox, clientsDc.getItems())
                .addDatePickerBinding(fromDatePicker)
                .addDatePickerBinding(toDatePicker)
                .build();

        selectedStatusUrlParameterBinder = SimpleUrlQueryParametersBinder.registerBinder(this,
                () -> QueryParameters.of("selected_status",
                        selectedStatus.map(s -> s.getId().toString()).orElse("")),
                qp -> qp.getSingleParameter("selected_status").ifPresent(id ->
                        selectedStatus = Optional.ofNullable(OrderStatus.fromStringId(id))));
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

    private void addDateRangeConditions() {
        addSearchByFromDateCondition();
        addSearchByToDateCondition();
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
        pipeLineFilter.selectStatus(selectedStatus.orElse(null));
        pipeLineFilter.addStatusClickListener(this::onStatusFilterClick);
        calculateAmountForEachOrderStatus();
    }

    private void onStatusFilterClick(OrderStatusComponent component) {
        Optional<OrderStatus> statusOpt = Optional.of(component.getStatus());

        if (selectedStatus.equals(statusOpt)) {
            selectedStatus = Optional.empty();
        } else {
            selectedStatus = statusOpt;
        }

        selectedStatusUrlParameterBinder.fireQueryParametersChanged();

        pipeLineFilter.deselectAllStatuses();
        selectedStatus.ifPresent(s -> pipeLineFilter.selectStatus(s));

        applyFilters();
    }

    private void calculateAmountForEachOrderStatus() {
        asyncTasksRegistry.placeTask("orderByStatusTask", uiAsyncTasks
                .supplierConfigurer(() -> orderService.getOrdersAmountByStatus())
                .withResultHandler(this::updateAmountForEachOrderStatus));
    }

    private void updateAmountForEachOrderStatus(Map<OrderStatus, BigDecimal> result) {
        result.forEach((status, amount) ->
                pipeLineFilter.getStatusComponents().forEach(comp -> {
                    if (comp.getStatus().equals(status)) {
                        comp.setTitle(messages.getMessage(status) + " (" + amount + ")");
                    }
                }));
    }
}