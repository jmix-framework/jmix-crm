package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.client.CompletedOrdersByDateRangeInfo;
import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.ui.component.CrmLoader;
import com.company.crm.app.ui.component.RecentActivitiesBlock;
import com.company.crm.app.ui.component.card.CrmCard;
import com.company.crm.app.util.AsyncTasksRegistry;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.app.util.ui.chart.ChartsUtils;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.address.Address;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.view.address.AddressFragment;
import com.company.crm.view.main.MainView;
import com.company.crm.view.payment.PaymentDetailView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.SimpleDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.SplitLine;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.SaveContext;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.view.DetailSaveCloseAction;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.asynctask.UiAsyncTasks.SupplierConfigurer;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.splitlayout.JmixSplitLayout;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstancePropertyContainer;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static com.company.crm.app.feature.sortable.SortableFeature.makeSortable;
import static com.company.crm.app.util.demo.DemoUtils.defaultSleepForStatisticsLoading;

@Route(value = "clients/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CLIENT_DETAIL)
@ViewDescriptor(path = "client-detail-view.xml")
@EditedEntityContainer("clientDc")
@DialogMode(width = "90%", height = "90%", resizable = true, closeOnEsc = true, closeOnOutsideClick = true)
public class ClientDetailView extends StandardDetailView<Client> implements WidthResizeListener {

    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;

    @ViewComponent
    private JmixTabSheet tabSheet;
    @ViewComponent
    private RecentActivitiesBlock recentActivities;
    @ViewComponent
    private JmixSplitLayout formSplit;
    @ViewComponent
    private JmixFormLayout analyticChartsBlock;
    @ViewComponent
    private CrmCard ordersTotalSumCard;
    @ViewComponent
    private CrmCard paymentsTotalSumCard;
    @ViewComponent
    private CrmCard averageBillCard;
    @ViewComponent
    private H3 outstandingBalanceValue;

    @SuppressWarnings("FieldCanBeLocal")
    private final int loadStatsForLastYearsAmount = 3;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Fragments fragments;
    @Autowired
    private Messages messages;
    @ViewComponent
    private InstancePropertyContainer<Address> addressDc;
    @ViewComponent
    private JmixTextArea addressField;
    @ViewComponent
    private DetailSaveCloseAction<Object> saveCloseAction;
    @Autowired
    private CrmRenderers crmRenderers;

    private final AsyncTasksRegistry asyncTasksRegistry = AsyncTasksRegistry.newInstance();
    @ViewComponent
    private JmixFormLayout summaryBlock;

    @Override
    public void configureUiForWidth(int width) {
        formSplit.setOrientation(width > 900 ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        saveCloseAction.setVisible(!readOnly);
    }

    @Subscribe
    private void onInit(final InitEvent event) {
        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
        addDetachListener(e -> asyncTasksRegistry.cancelAll());
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        recentActivities.setMaxWidth(27.5f, Unit.EM);
        recentActivities.setClient(getEditedEntity());
        initializeSummaryBlock();
        initializeOutstandingBalance();
        initializeAnalyticsBlock();
    }

    @Install(to = "clientDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Client> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return clientRepository.findByIdWithDynamicAttributes(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(clientRepository.save(getEditedEntity()));
    }

    @Subscribe(target = Target.DATA_CONTEXT)
    private void onDataContextChange(final DataContext.ChangeEvent event) {
        Address address = addressDc.getItemOrNull();
        if (address != null) {
            addressField.setValue(address.getInstanceName());
        }
    }

    @Supply(to = "paymentsDataGrid.number", subject = "renderer")
    private Renderer<Payment> paymentsDataGridNumberRenderer() {
        return crmRenderers.uniqueNumber(Payment::getNumber);
    }

    @Install(to = "ordersDataGrid.createAction", subject = "initializer")
    private void ordersDataGridCreateActionInitializer(final Order order) {
        order.setClient(getEditedEntity());
    }

    @Install(to = "invoicesDataGrid.createAction", subject = "initializer")
    private void invoicesDataGridCreateActionInitializer(final Invoice invoice) {
        invoice.setClient(getEditedEntity());
    }

    @Install(to = "paymentsDataGrid.createAction", subject = "viewConfigurer")
    private void paymentsDataGridCreateActionViewConfigurer(final PaymentDetailView paymentDetail) {
        paymentDetail.setClient(getEditedEntity());
    }

    @Subscribe("downloadProfile")
    private void onDownloadProfile(final ActionPerformedEvent event) {
        // TODO: download custom design-time report
        notifications.create("Company profile download is not available yet")
                .withType(Notifications.Type.WARNING)
                .show();
    }

    @Subscribe(id = "addressEditBtn", subject = "clickListener")
    private void onAddressEditBtnClick(final ClickEvent<JmixButton> event) {
        openAddressEditFormDialog();
    }

    private void openAddressEditFormDialog() {
        AddressFragment addressFragment = fragments.create(this, AddressFragment.class);
        addressFragment.setAddress(metadataTools.copy(addressDc.getItem()));
        Dialog addressDialog = new Dialog(addressFragment);

        addressDialog.setHeaderTitle(messages.getMessage(Address.class, "Address"));
        addressDialog.setMaxWidth(40, Unit.EM);
        addressDialog.setResizable(true);

        Runnable closeDialog = addressDialog::close;

        var closeButton = uiComponents.create(JmixButton.class);
        closeButton.setIcon(VaadinIcon.CLOSE_SMALL.create());
        closeButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> closeDialog.run());
        addressDialog.getHeader().add(closeButton);

        var saveButton = uiComponents.create(JmixButton.class);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setText(messages.getMessage("actions.Save"));
        saveButton.addClickListener(event -> {
            if (addressFragment.validate().isEmpty()) {
                Address updatedAddress = addressFragment.getAddress();
                if (updatedAddress != null) {
                    metadataTools.copy(updatedAddress, addressDc.getItem());
                }
                closeDialog.run();
            }
        });

        var cancelButton = uiComponents.create(JmixButton.class);
        cancelButton.setText(messages.getMessage("actions.Cancel"));
        cancelButton.addClickListener(e -> closeDialog.run());

        addressDialog.getFooter().add(saveButton, cancelButton);

        addressDialog.open();
    }

    private void initializeSummaryBlock() {
        makeSortable(summaryBlock);

        Client client = getEditedEntity();
        installCardLoader(ordersTotalSumCard);
        installCardLoader(paymentsTotalSumCard);
        installCardLoader(averageBillCard);

        scheduleOrdersTotalSumCalculating(client);
        schedulePaymentsTotalSumCalculating(client);
        scheduleAverageBillCalculating(client);
    }

    private void initializeOutstandingBalance() {
        Client client = getEditedEntity();
        outstandingBalanceValue.setText("...");

        SupplierConfigurer<BigDecimal> taskConfigurer = uiAsyncTasks
                .supplierConfigurer(() -> calculateOutstandingBalance(client))
                .withExceptionHandler(e -> outstandingBalanceValue.setText("-"))
                .withResultHandler(total -> outstandingBalanceValue.setText(PriceDataType.defaultFormat(total)));
        asyncTasksRegistry.placeTask("outstandingBalanceTask", taskConfigurer);
    }

    private void initializeAnalyticsBlock() {
        analyticChartsBlock.addAttachListener(e -> {
            if (e.isInitialAttach()) {
                chartsUtils.initializeChartsAsync(getChartsLoaders());
            }
        });
    }

    private void installCardLoader(CrmCard card) {
        card.removeAll();
        CrmLoader loader = new CrmLoader();
        loader.startLoading();
        card.add(loader);
        SkeletonStyler.apply(card);
    }

    private void scheduleOrdersTotalSumCalculating(Client client) {
        SupplierConfigurer<BigDecimal> taskConfigurer = uiAsyncTasks
                .supplierConfigurer(() -> calculateOrdersTotalSum(client))
                .withExceptionHandler(e -> SkeletonStyler.remove(ordersTotalSumCard))
                .withResultHandler(total -> fillSummaryCard(messages.getMessage("ordersTotal"), ordersTotalSumCard, total));
        asyncTasksRegistry.placeTask("ordersTotalSumTask", taskConfigurer);
    }

    private void schedulePaymentsTotalSumCalculating(Client client) {
        SupplierConfigurer<BigDecimal> taskConfigurer = uiAsyncTasks
                .supplierConfigurer(() -> calculatePaymentsTotalSum(client))
                .withExceptionHandler(e -> SkeletonStyler.remove(paymentsTotalSumCard))
                .withResultHandler(total -> fillSummaryCard(messages.getMessage("paymentsTotal"), paymentsTotalSumCard, total));
        asyncTasksRegistry.placeTask("paymentsTotalSumTask", taskConfigurer);
    }

    private void scheduleAverageBillCalculating(Client client) {
        SupplierConfigurer<BigDecimal> taskConfigurer = uiAsyncTasks
                .supplierConfigurer(() -> calculateAverageBill(client))
                .withExceptionHandler(e -> SkeletonStyler.remove(averageBillCard))
                .withResultHandler(average -> fillSummaryCard(messages.getMessage("averageBill"), averageBillCard, average));
        asyncTasksRegistry.placeTask("averageBillTask", taskConfigurer);
    }

    private BigDecimal calculateOrdersTotalSum(Client client) {
        defaultSleepForStatisticsLoading();
        return clientService.getOrdersTotalSum(OrderStatus.values(), client);
    }

    private BigDecimal calculatePaymentsTotalSum(Client client) {
        defaultSleepForStatisticsLoading();
        return clientService.getPaymentsTotalSum(client);
    }

    private BigDecimal calculateAverageBill(Client client) {
        defaultSleepForStatisticsLoading();
        return clientService.getAverageBill(client);
    }

    private BigDecimal calculateOutstandingBalance(Client client) {
        defaultSleepForStatisticsLoading();
        return clientService.getInvoicesTotalSum(client)
                .subtract(clientService.getPaymentsTotalSum(client));
    }

    private void fillSummaryCard(String title, CrmCard card, BigDecimal value) {
        VerticalLayout content = new VerticalLayout(new H1(PriceDataType.defaultFormat(value)));
        content.setPadding(false);
        content.setSpacing(false);
        card.fillAsStaticCard(title, content);
        SkeletonStyler.remove(card);
    }

    private Map<Chart, Supplier<DataSet>> getChartsLoaders() {
        var chart2DataSetLoader = new HashMap<Chart, Supplier<DataSet>>();

        new ArrayList<Pair<Chart, Supplier<DataSet>>>() {{
            add(createOrdersByLastYearsChart());
            add(createAverageOrderValueChart());
            add(createSalesCycleLengthChart());
        }}.forEach(chart2Initializer -> {
            Chart chart = chart2Initializer.getFirst();
            Supplier<DataSet> dataSetSupplier = chart2Initializer.getSecond();
            chart.withLegend(new Legend().withShow(false))
                    .getYAxes().getFirst()
                    .withInterval(0)
                    .withSplitLine(new SplitLine().withShow(false));

            analyticChartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
            chart2DataSetLoader.put(chart, dataSetSupplier);
        });
        return chart2DataSetLoader;
    }

    private Pair<Chart, Supplier<DataSet>> createOrdersByLastYearsChart() {
        Chart chart = chartsUtils.createViewStatChart("Purchase Frequency", SeriesType.BAR);
        return new Pair<>(chart, this::createOrdersByLastYearsChartDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createAverageOrderValueChart() {
        Chart chart = chartsUtils.createViewStatChart("Average Order Value", SeriesType.BAR);
        return new Pair<>(chart, this::createAverageOrderValueChartDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createSalesCycleLengthChart() {
        Chart chart = chartsUtils.createViewStatChart("Sales Cycle Length", SeriesType.BAR);
        return new Pair<>(chart, this::createSalesCycleLengthChartDataSet);
    }

    private DataSet createOrdersByLastYearsChartDataSet() {
        var ordersByFiveLastYears = loadOrdersInfoForLastYears();

        var dataItems = new ArrayList<SimpleDataItem>();
        for (var entry : ordersByFiveLastYears.entrySet()) {
            var stat = entry.getValue().stream().findFirst()
                    .map(CompletedOrdersByDateRangeInfo::getRangeOrders).orElse(0L);
            dataItems.add(new SimpleDataItem(new YearNumberStatisticItemValue(entry.getKey(), stat)));
        }

        return new DataSet().withSource(
                new DataSet.Source<SimpleDataItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("year")
                        .withValueField("statValue")
        );
    }

    private DataSet createAverageOrderValueChartDataSet() {
        var ordersInfoForLastYears = loadOrdersInfoForLastYears();

        var dataItems = new ArrayList<SimpleDataItem>();
        for (var entry : ordersInfoForLastYears.entrySet()) {
            var stat = entry.getValue().stream().findFirst()
                    .map(CompletedOrdersByDateRangeInfo::getRangeAverageBill).orElse(BigDecimal.ZERO);
            dataItems.add(new SimpleDataItem(new YearNumberStatisticItemValue(entry.getKey(), stat)));
        }

        return new DataSet().withSource(
                new DataSet.Source<SimpleDataItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("year")
                        .withValueField("statValue")
        );
    }

    private DataSet createSalesCycleLengthChartDataSet() {
        var ordersInfoForLastYears = loadOrdersInfoForLastYears();

        var dataItems = new ArrayList<SimpleDataItem>();
        for (var entry : ordersInfoForLastYears.entrySet()) {
            var stat = entry.getValue().stream().findFirst()
                    .map(CompletedOrdersByDateRangeInfo::getRangeSalesLifeCycleLength).orElse(0);
            dataItems.add(new SimpleDataItem(new YearNumberStatisticItemValue(entry.getKey(), stat)));
        }

        return new DataSet().withSource(
                new DataSet.Source<SimpleDataItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("year")
                        .withValueField("statValue")
        );
    }

    // FIXME: need to load orders info for years async
    //  for example on init event and cache it
    private Map<Integer, List<CompletedOrdersByDateRangeInfo>> loadOrdersInfoForLastYears() {
        var result = new HashMap<Integer, List<CompletedOrdersByDateRangeInfo>>();
        LocalDate currentYearStart = dateTimeService.getCurrentYearStart().toLocalDate();
        for (int i = 0; i < loadStatsForLastYearsAmount; i++) {
            currentYearStart = currentYearStart.minusYears(i > 0 ? 1 : 0);
            var currentYearEnd = dateTimeService.getEndOfYear(dateTimeService.toOffsetDateTime(currentYearStart)).toLocalDate();
            var dateRange = LocalDateRange.from(currentYearStart, currentYearEnd);
            result.put(currentYearStart.getYear(), clientService.getCompletedOrdersInfo(dateRange, getEditedEntity()));
        }
        return result;
    }
}
