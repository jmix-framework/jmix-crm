package com.company.crm.view.order;

import com.company.crm.app.feature.queryparameters.SimpleUrlQueryParametersBinder;
import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.ui.component.OrderStatusPipeline;
import com.company.crm.app.ui.component.OrderStatusPipeline.OrderStatusComponent;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.invoice.InvoiceDetailView;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.company.crm.app.util.ui.CrmUiUtils.addColumnHeaderCurrencySuffix;
import static com.company.crm.app.util.ui.CrmUiUtils.setSearchHintPopover;
import static com.company.crm.app.util.ui.datacontext.DataContextUtils.addCondition;
import static com.company.crm.model.datatype.PriceDataType.formatWithoutCurrency;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.greaterOrEqual;
import static io.jmix.core.querycondition.PropertyCondition.lessOrEqual;

@Route(value = "orders", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.ORDER_LIST)
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "90%", resizable = true)
@PrimaryListView(Order.class)
public class OrderListView extends StandardListView<Order> {

    @Autowired
    private Messages messages;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private OrderRepository orderRepository;

    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private CollectionLoader<Client> clientsDl;
    @ViewComponent
    private CollectionContainer<Client> clientsDc;

    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private EntityComboBox<Client> clientComboBox;
    @ViewComponent
    private TypedDatePicker<LocalDate> fromDatePicker;
    @ViewComponent
    private TypedDatePicker<LocalDate> toDatePicker;
    @ViewComponent
    private OrderStatusPipeline pipeLineFilter;
    @ViewComponent
    private CollectionContainer<Order> ordersDc;
    @ViewComponent
    private DataGrid<Order> ordersDataGrid;

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    private Optional<OrderStatus> selectedStatus = Optional.empty();
    private SimpleUrlQueryParametersBinder selectedStatusUrlParameterBinder;

    @Subscribe
    private void onInit(final InitEvent event) {
        configureGrid();
        clientsDl.load();
        registerUrlQueryParametersBinders();
    }

    private void configureGrid() {
        addColumnHeaderCurrencySuffix(ordersDataGrid, "total", "invoiced", "paid", "leftOver");
        ordersDataGrid.setItemDetailsRenderer(crmRenderers.orderDetails());
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        initializeFilterFields();
        applyFilters();
    }

    @Install(to = "ordersDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Order> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return orderRepository.findAll(pageable, addCondition(context, filtersCondition)).getContent();
    }

    @Subscribe(id = "ordersDl", target = Target.DATA_LOADER)
    private void onOrdersDlPostLoad(final CollectionLoader.PostLoadEvent<Order> event) {
        updatePipeLineFilter();
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return orderRepository.count(addCondition(context, filtersCondition));
    }

    @Install(to = "ordersDataGrid.removeAction", subject = "delegate")
    private void ordersDataGridRemoveDelegate(final Collection<Order> collection) {
        orderRepository.deleteAll(collection);
    }

    @Subscribe("ordersDataGrid.addInvoice")
    private void onOrdersDataGridAddInvoice(final ActionPerformedEvent event) {
        Order order = ordersDataGrid.getSingleSelectedItem();
        if (order == null) {
            return;
        }

        dialogWindows.detail(this, Invoice.class)
                .newEntity()
                .withInitializer(invoice -> invoice.setOrder(order))
                .withViewClass(InvoiceDetailView.class)
                .withViewConfigurer(InvoiceDetailView::forbidChangeOrder)
                .open();
    }

    @Supply(to = "ordersDataGrid.itemDetails", subject = "renderer")
    private Renderer<Order> ordersDataGridItemDetailsRenderer() {
        return crmRenderers.itemDetailsColumnRenderer(ordersDataGrid);
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
        return new TextRenderer<>(order -> formatWithoutCurrency(order.getTotal()));
    }

    @Supply(to = "ordersDataGrid.number", subject = "renderer")
    private Renderer<Order> ordersDataGridNumberRenderer() {
        return crmRenderers.uniqueNumber(Order::getNumber);
    }

    @Supply(to = "ordersDataGrid.invoiced", subject = "renderer")
    private Renderer<Order> ordersDataGridInvoicedRenderer() {
        return new TextRenderer<>(order -> formatWithoutCurrency(order.getInvoiced()));
    }

    @Supply(to = "ordersDataGrid.paid", subject = "renderer")
    private Renderer<Order> ordersDataGridPaidRenderer() {
        return new TextRenderer<>(order -> formatWithoutCurrency(order.getPaid()));
    }

    @Supply(to = "ordersDataGrid.leftOverSum", subject = "renderer")
    private Renderer<Order> ordersDataGridLeftOverRenderer() {
        return crmRenderers.orderLeftOverSumRenderer();
    }

    @Install(to = "ordersDataGrid.addInvoice", subject = "enabledRule")
    private boolean ordersDataGridAddInvoiceEnabledRule() {
        return ordersDataGrid.getSelectedItems().size() == 1;
    }

    private void initializeFilterFields() {
        initializePipelineFilter();
        setSearchHintPopover(searchField);
        List.<HasValue<?, ?>>of(searchField, clientComboBox, fromDatePicker, toDatePicker).forEach(field -> {
            field.addValueChangeListener(e -> applyFilters());
        });
    }

    private void registerUrlQueryParametersBinders() {
        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(searchField)
                .addComboboxBinding(clientComboBox, () -> clientsDc.getItems())
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

    private void updatePipeLineFilter() {
        Map<OrderStatus, BigDecimal> status2Amount = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            status2Amount.put(status, BigDecimal.ZERO);
        }

        ordersDc.getItems().forEach(item -> {
            OrderStatus status = item.getStatus();
            BigDecimal currentAmount = status2Amount.getOrDefault(status, BigDecimal.ZERO);
            status2Amount.put(status, currentAmount.add(BigDecimal.ONE));
        });

        status2Amount.forEach((status, amount) ->
                pipeLineFilter.getStatusComponents().forEach(comp -> {
                    if (comp.getStatus().equals(status)) {
                        comp.setTitle(messages.getMessage(status) + " (" + amount + ")");
                    }
                }));
    }
}