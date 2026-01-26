package com.company.crm.view.payment;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.chart.ChartsUtils;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.payment.PaymentRepository;
import com.company.crm.view.main.MainView;
import com.company.crm.view.payment.charts.ClientTotalPaymentsValueDescription;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.SimpleDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.MessageBundle;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.company.crm.app.util.ui.datacontext.DataContextUtils.wrapCondition;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.greaterOrEqual;
import static io.jmix.core.querycondition.PropertyCondition.lessOrEqual;

@Route(value = "payments", layout = MainView.class)
@ViewDescriptor("payment-list-view.xml")
@ViewController(CrmConstants.ViewIds.PAYMENT_LIST)
@LookupComponent("paymentsDataGrid")
@DialogMode(width = "90%", resizable = true)
public class PaymentListView extends StandardListView<Payment> {

    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;

    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent
    private CollectionLoader<Payment> paymentsDl;
    @ViewComponent
    private CollectionContainer<Invoice> invoicesDc;
    @ViewComponent
    private CollectionLoader<Invoice> invoicesDl;
    @ViewComponent
    private CollectionContainer<Order> ordersDc;
    @ViewComponent
    private CollectionLoader<Order> ordersDl;
    @ViewComponent
    private CollectionContainer<Client> clientsDc;
    @ViewComponent
    private CollectionLoader<Client> clientsDl;

    @ViewComponent
    private JmixFormLayout chartsBlock;
    @ViewComponent
    private DataGrid<Payment> paymentsDataGrid;
    @ViewComponent
    private EntityComboBox<Client> payments_ClientComboBox;
    @ViewComponent
    private EntityComboBox<Order> payments_OrderComboBox;
    @ViewComponent
    private EntityComboBox<Invoice> payments_InvoiceComboBox;
    @ViewComponent
    private TypedDatePicker<LocalDate> payments_FromDatePicker;
    @ViewComponent
    private TypedDatePicker<LocalDate> payments_ToDatePicker;

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initialize();
    }

    @Subscribe
    private void onInit(final InitEvent event) {
        installGridDefaultSorting();
    }

    @Install(to = "paymentsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Payment> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return paymentRepository.findAll(pageable, wrapCondition(context, filtersCondition)).getContent();
    }

    @Install(to = "payments_pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return paymentRepository.count(wrapCondition(context, filtersCondition));
    }

    @Install(to = "paymentsDataGrid.removeAction", subject = "delegate")
    private void paymentsDataGridRemoveDelegate(final Collection<Payment> collection) {
        paymentRepository.deleteAll(collection);
    }

    @Supply(to = "paymentsDataGrid.number", subject = "renderer")
    private Renderer<Payment> paymentsDataGridNumberRenderer() {
        return crmRenderers.uniqueNumber(Payment::getNumber);
    }

    @Supply(to = "paymentsDataGrid.[order.client]", subject = "renderer")
    private Renderer<Payment> paymentsDataGridOrderClientRenderer() {
        return crmRenderers.entityLink(p -> p.getOrder().getClient());
    }

    @Supply(to = "paymentsDataGrid.order", subject = "renderer")
    private Renderer<Payment> paymentsDataGridOrderRenderer() {
        return crmRenderers.entityLink(Payment::getOrder);
    }

    @Supply(to = "paymentsDataGrid.invoice", subject = "renderer")
    private Renderer<Payment> paymentsDataGridInvoiceRenderer() {
        return crmRenderers.entityLink(Payment::getInvoice);
    }

    private void installGridDefaultSorting() {
        if (paymentsDataGrid.getItems() instanceof DataGridItems.Sortable<Payment> sortable) {
            sortable.sort(new String[]{"date"}, new boolean[]{false});
        }
    }

    private void initialize() {
        loadData();
        initializeChartsBlock();
        registerUrlQueryParametersBinders();
        applyFilters();
    }

    private void initializeChartsBlock() {
        chartsUtils.initializeChartsAsync(getChartsLoaders());
    }

    private void loadData() {
        paymentsDl.load();
        clientsDl.load();
        ordersDl.load();
        invoicesDl.load();
    }

    private void registerUrlQueryParametersBinders() {
        List.<HasValue<?, ?>>of(payments_ClientComboBox, payments_OrderComboBox,
                        payments_InvoiceComboBox, payments_FromDatePicker, payments_ToDatePicker)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        FieldValueQueryParameterBinder.builder(UiComponentUtils.getCurrentView())
                .addComboboxBinding(payments_OrderComboBox, () -> ordersDc.getItems())
                .addComboboxBinding(payments_ClientComboBox, () -> clientsDc.getItems())
                .addComboboxBinding(payments_InvoiceComboBox, () -> invoicesDc.getItems())
                .addDatePickerBinding(payments_FromDatePicker)
                .addDatePickerBinding(payments_ToDatePicker)
                .build();
    }

    private void applyFilters() {
        updateFiltersCondition();
        paymentsDl.load();
    }

    private void updateFiltersCondition() {
        filtersCondition.getConditions().clear();
        addSearchConditions();
    }

    private void addSearchConditions() {
        addSearchByOrderCondition();
        addSearchByClientCondition();
        addSearchByInvoiceCondition();
        addDateRangeConditions();
    }

    private void addSearchByClientCondition() {
        payments_ClientComboBox.getOptionalValue().ifPresent(client ->
                filtersCondition.add(equal("invoice.order.client", client)));
    }

    private void addSearchByOrderCondition() {
        payments_OrderComboBox.getOptionalValue().ifPresent(order ->
                filtersCondition.add(equal("invoice.order", order)));
    }

    private void addSearchByInvoiceCondition() {
        payments_InvoiceComboBox.getOptionalValue().ifPresent(invoice ->
                filtersCondition.add(equal("invoice", invoice)));
    }

    private void addDateRangeConditions() {
        addSearchByFromDateCondition();
        addSearchByToDateCondition();
    }

    private void addSearchByFromDateCondition() {
        payments_FromDatePicker.getOptionalValue().ifPresent(fromDate ->
                filtersCondition.add(greaterOrEqual("date", fromDate)));
    }

    private void addSearchByToDateCondition() {
        payments_ToDatePicker.getOptionalValue().ifPresent(fromDate ->
                filtersCondition.add(lessOrEqual("date", fromDate)));
    }

    private Map<Chart, Supplier<DataSet>> getChartsLoaders() {
        var chart2DataSetLoader = new HashMap<Chart, Supplier<DataSet>>();

        new ArrayList<Pair<Chart, Supplier<DataSet>>>() {{
            add(createTotalsByClientChart());
            add(createBiggestPaymentsChart());
        }}.forEach(chart2Initializer -> {
            Chart chart = chart2Initializer.getFirst();
            Supplier<DataSet> dataSetSupplier = chart2Initializer.getSecond();
            chartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
            chart2DataSetLoader.put(chart, dataSetSupplier);
        });
        return chart2DataSetLoader;
    }

    private Pair<Chart, Supplier<DataSet>> createTotalsByClientChart() {
        Chart chart = chartsUtils.createViewStatPieChart(messageBundle.getMessage("totalsByClients"));
        return new Pair<>(chart, this::createTotalsByClientChartDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createBiggestPaymentsChart() {
        Chart chart = chartsUtils.createViewStatPieChart(messageBundle.getMessage("biggestPayments"));
        return new Pair<>(chart, this::createBiggestPaymentsChartDataSet);
    }

    private DataSet createTotalsByClientChartDataSet() {
        Map<Client, BigDecimal> totalsByClients = paymentService.getPaymentsTotalsByClients(3);

        var dataItems = new ArrayList<SimpleDataItem>();
        for (var entry : totalsByClients.entrySet()) {
            dataItems.add(new SimpleDataItem(new ClientTotalPaymentsValueDescription(entry.getKey().getName(), entry.getValue())));
        }

        return new DataSet().withSource(
                new DataSet.Source<SimpleDataItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("client")
                        .withValueField("total")
        );
    }

    private DataSet createBiggestPaymentsChartDataSet() {
        List<Payment> biggestPayments = paymentService.getBiggestPayments(3);
        var dataItems = new ArrayList<SimpleDataItem>();
        for (var entry : biggestPayments) {
            dataItems.add(new SimpleDataItem(entry));
        }

        return new DataSet().withSource(
                new DataSet.Source<SimpleDataItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("client")
                        .withValueField("amount")
        );
    }
}