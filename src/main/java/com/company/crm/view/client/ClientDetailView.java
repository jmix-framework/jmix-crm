package com.company.crm.view.client;

import com.company.crm.ai.view.context.AiChatAboutThisSupport;
import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.client.CompletedOrdersByDateRangeInfo;
import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.ui.component.CrmCard;
import com.company.crm.app.ui.component.CrmLoader;
import com.company.crm.app.ui.component.RecentActivitiesBlock;
import com.company.crm.app.util.AsyncTasksRegistry;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.app.util.ui.chart.ChartsUtils;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.address.Address;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import com.company.crm.view.address.AddressFragment;
import com.company.crm.view.main.MainView;
import com.company.crm.view.payment.PaymentDetailView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.SimpleDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.SplitLine;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.AccessManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.action.view.DetailSaveCloseAction;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.asynctask.UiAsyncTasks.SupplierConfigurer;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.data.EntityValueSource;
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
import io.jmix.reportsflowui.runner.UiReportRunner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.company.crm.app.feature.sortable.SortableFeature.makeSortable;
import static com.company.crm.app.util.demo.DemoUtils.defaultSleepForStatisticsLoading;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Route(value = "clients/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CLIENT_DETAIL)
@ViewDescriptor(path = "client-detail-view.xml")
@EditedEntityContainer("clientDc")
@DialogMode(width = "90%", height = "90%", resizable = true, closeOnEsc = true, closeOnOutsideClick = true)
public class ClientDetailView extends StandardDetailView<Client> {

    @Autowired
    private Messages messages;
    @Autowired
    private Fragments fragments;
    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ClientService clientService;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UiReportRunner uiReportRunner;
    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private AiChatAboutThisSupport aiChatAboutThisSupport;

    @ViewComponent
    private JmixTabSheet tabSheet;
    @ViewComponent
    private JmixFormLayout summaryBlock;
    @ViewComponent
    private RecentActivitiesBlock recentActivities;
    @ViewComponent
    private JmixFormLayout analyticChartsBlock;
    @ViewComponent
    private EntityComboBox<User> accountManagerField;
    @ViewComponent
    private CrmCard ordersTotalSumCard;
    @ViewComponent
    private CrmCard paymentsTotalSumCard;
    @ViewComponent
    private CrmCard averageBillCard;
    @ViewComponent
    private H3 outstandingBalanceValue;
    @ViewComponent
    private H2 clientName;
    @ViewComponent
    private InstancePropertyContainer<Address> addressDc;
    @ViewComponent
    private JmixTextArea addressField;
    @ViewComponent
    private DetailSaveCloseAction<Object> saveCloseAction;

    @SuppressWarnings("FieldCanBeLocal")
    private final int loadStatsForLastYearsAmount = 3;
    private final AsyncTasksRegistry asyncTasksRegistry = AsyncTasksRegistry.newInstance();
    private final Map<Integer, List<CompletedOrdersByDateRangeInfo>> ordersInfoForLastYears = new HashMap<>();

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
        clientName.setText(getEditedEntity().getInstanceName(messages));
        initializeRecentActivities();
        initializeSummaryBlock();
        initializeOutstandingBalance();
        initializeAnalyticsBlock();
        updateAccountManagerFieldAccessibility();
        updateAddressField();
    }

    @Subscribe("downloadProfileButton")
    public void onDownloadProfileButtonClick(ClickEvent<JmixButton> event) {
        LocalDate now = LocalDate.now();
        LocalDate oneYearAgo = now.minusYears(1);

        Date fromDate = Date.from(oneYearAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(now.atStartOfDay(ZoneId.systemDefault()).toInstant());

        uiReportRunner.byReportCode("client-360-report")
                .addParam("client", getEditedEntity())
                .addParam("fromDate", fromDate)
                .addParam("toDate", toDate)
                .runAndShow();
    }

    @Subscribe("chatAboutThis")
    private void onChatAboutThis(final ActionPerformedEvent event) {
        aiChatAboutThisSupport.openChatAbout(getEditedEntity());
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
        updateAddressField();
    }

    @Subscribe
    private void onValidation(final ValidationEvent event) {
        if (isAddressIncomplete(addressDc.getItemOrNull())) {
            String errorMessage = messages.getMessage("com.company.crm.view.client/addressRequiredError");
            addressField.setInvalid(true);
            addressField.setErrorMessage(errorMessage);
            event.getErrors().add(errorMessage);
        } else {
            addressField.setInvalid(false);
            addressField.setErrorMessage(null);
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


    @Subscribe(id = "addressEditBtn", subject = "clickListener")
    private void onAddressEditBtnClick(final ClickEvent<JmixButton> event) {
        openAddressEditFormDialog();
    }

    private void updateAccountManagerFieldAccessibility() {
        if (accountManagerField.getValueSource() instanceof EntityValueSource<?, ?> entityValueSource
                && entityValueSource.isDataModelSecurityEnabled()) {
            UiEntityAttributeContext context = new UiEntityAttributeContext(entityValueSource.getMetaPropertyPath());
            accessManager.applyRegisteredConstraints(context);
            accountManagerField.setReadOnly(!context.canModify());
        }
    }

    private void updateAddressField() {
        Address address = addressDc.getItemOrNull();
        if (address != null) {
            String presentableText = address.getInstanceName();
            if (StringUtils.isNotBlank(presentableText)) {
                addressField.setValue(presentableText);
            } else {
                addressField.setValue(addressField.getPlaceholder());
            }
        } else {
            addressField.setValue(addressField.getPlaceholder());
        }
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

    private void initializeRecentActivities() {
        recentActivities.setMaxWidth(27.5f, Unit.EM);
        recentActivities.setClient(getEditedEntity());
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
                .withResultHandler(total -> outstandingBalanceValue.setText(PriceDataType.defaultFormat(total, datatypeFormatter)));
        asyncTasksRegistry.placeTask("outstandingBalanceTask", taskConfigurer);
    }

    private void initializeAnalyticsBlock() {
        asyncTasksRegistry.placeTask("loadOrdersInfoForLastYears",
                uiAsyncTasks.runnableConfigurer(this::loadOrdersInfoForLastYears)
                        .withResultHandler(this::initializeCharts));
    }

    private void installCardLoader(CrmCard card) {
        card.removeAll();
        card.setHeader(null);
        SkeletonStyler.apply(card);

        CrmLoader loader = new CrmLoader(card);
        loader.setLogoSize("3em");
        loader.startLoading();
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
        return clientService.getOutstandingBalance(client);
    }

    private void fillSummaryCard(String title, CrmCard card, BigDecimal value) {
        var valueContent = new H1(PriceDataType.defaultFormat(value, datatypeFormatter));
        valueContent.addClassNames(Overflow.HIDDEN, TextOverflow.ELLIPSIS, Whitespace.NOWRAP);

        var content = new VerticalLayout(valueContent);
        content.setPadding(false);
        content.setSpacing(false);

        content.addClassNames(Overflow.HIDDEN, TextOverflow.ELLIPSIS, Whitespace.NOWRAP);
        card.fillAsStaticCard(title, content);
        SkeletonStyler.remove(card);
    }

    private void initializeCharts() {
        createOrdersByLastYearsChart();
        createAverageOrderValueChart();
        createSalesCycleLengthChart();
    }

    private void createOrdersByLastYearsChart() {
        Chart chart = chartsUtils.createViewStatChart(messages.getMessage("com.company.crm.view.client/analytics.purchaseFrequency"), SeriesType.BAR)
                .withDataSet(createOrdersByLastYearsChartDataSet())
                .withLegend(new Legend().withShow(false));
        configureAxes(chart);
        analyticChartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
    }

    private void createAverageOrderValueChart() {
        Chart chart = chartsUtils.createViewStatChart(messages.getMessage("com.company.crm.view.client/analytics.averageOrderValue"), SeriesType.BAR)
                .withDataSet(createAverageOrderValueChartDataSet())
                .withLegend(new Legend().withShow(false));
        configureAxes(chart);
        analyticChartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
    }

    private void createSalesCycleLengthChart() {
        Chart chart = chartsUtils.createViewStatChart(messages.getMessage("com.company.crm.view.client/analytics.salesCycleLength"), SeriesType.BAR)
                .withDataSet(createSalesCycleLengthChartDataSet())
                .withLegend(new Legend().withShow(false));
        configureAxes(chart);
        analyticChartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
    }

    private static void configureAxes(Chart chart) {
        chart.getYAxes().getFirst()
                .withInterval(0)
                .withSplitLine(new SplitLine().withShow(false));
    }

    private DataSet createOrdersByLastYearsChartDataSet() {
        var dataItems = new ArrayList<SimpleDataItem>();
        for (var entry : ordersInfoForLastYears.entrySet()) {
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

    private void loadOrdersInfoForLastYears() {
        ordersInfoForLastYears.clear();
        Client client = getEditedEntity();
        LocalDate currentYearStart = dateTimeService.getCurrentYearStart().toLocalDate();
        for (int i = 0; i < loadStatsForLastYearsAmount; i++) {
            currentYearStart = currentYearStart.minusYears(i > 0 ? 1 : 0);
            var currentYearStartOffset = dateTimeService.toOffsetDateTime(currentYearStart);
            var currentYearEnd = dateTimeService.getEndOfYear(currentYearStartOffset).toLocalDate();
            var dateRange = LocalDateRange.from(currentYearStart, currentYearEnd);
            var completedOrdersInfo = clientService.getCompletedOrdersInfo(dateRange, client);
            ordersInfoForLastYears.put(currentYearStart.getYear(), completedOrdersInfo);
        }
    }

    private boolean isAddressIncomplete(Address address) {
        return address == null
                || isBlank(address.getCountry())
                || isBlank(address.getCity())
                || isBlank(address.getStreet())
                || isBlank(address.getBuilding());
    }
}
