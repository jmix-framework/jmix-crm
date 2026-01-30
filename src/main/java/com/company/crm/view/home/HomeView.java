package com.company.crm.view.home;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.service.finance.InvoiceService;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.ui.component.RecentActivitiesBlock;
import com.company.crm.app.ui.component.card.CrmCard;
import com.company.crm.app.ui.component.card.CrmCard.RangeStatCardInfo;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.date.Period;
import com.company.crm.app.util.ui.chart.ChartsUtils;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.view.main.MainView;
import com.company.crm.view.usertask.UserTaskListView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.CardVariant;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.Tooltip;
import io.jmix.chartsflowui.kit.component.model.legend.ScrollableLegend;
import io.jmix.chartsflowui.kit.component.model.series.Label;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.shared.FontStyle;
import io.jmix.chartsflowui.kit.component.model.shared.Orientation;
import io.jmix.chartsflowui.kit.component.model.toolbox.SaveAsImageFeature;
import io.jmix.chartsflowui.kit.component.model.toolbox.Toolbox;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.Views;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.component.formatter.DateFormatter;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.splitlayout.JmixSplitLayout;
import io.jmix.flowui.data.grid.ContainerDataGridItems;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.company.crm.app.feature.sortable.SortableFeature.makeSortable;
import static com.company.crm.app.util.ui.CrmUiUtils.setBackgroundTransparent;
import static com.company.crm.app.util.ui.listener.resize.WidthResizeListener.isWidthChanged;
import static io.jmix.flowui.component.UiComponentUtils.traverseComponents;

@Route(value = "home", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.HOME)
@ViewDescriptor(path = "home-view.xml")
public class HomeView extends StandardView implements WidthResizeListener {

    @Autowired
    private Metadata metadata;
    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private OrderService orderService;
    @Autowired
    private DataComponents dataComponents;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private Views views;
    @Autowired
    private Messages messages;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private DateFormatter<LocalDate> localDateFormatter;

    @ViewComponent
    private JmixSplitLayout split;
    @ViewComponent
    private VerticalLayout leftBox;
    @ViewComponent
    private VerticalLayout rightBox;
    @ViewComponent
    private JmixFormLayout rightContent;
    @ViewComponent
    private JmixFormLayout leftContent;
    @ViewComponent
    private MessageBundle messageBundle;

    private volatile int lastWidth = -1;
    private static final int widthBreakpoint = 1000;

    @Override
    public void configureUiForWidth(int width) {
        if (isWidthChanged(width, lastWidth, widthBreakpoint)) {
            lastWidth = width;

            var breakpoint = 1200;
            if (width < breakpoint) {
                configureUiForSmallWidth();
            } else {
                configureUiForNormalWidth();
            }
        }
    }

    private void configureUiForNormalWidth() {
        split.setOrientation(JmixSplitLayout.Orientation.HORIZONTAL);
        leftBox.setWidth(65, Unit.PERCENTAGE);
        rightBox.setWidth(35, Unit.PERCENTAGE);
    }

    private void configureUiForSmallWidth() {
        split.setOrientation(JmixSplitLayout.Orientation.VERTICAL);
        leftBox.setWidthFull();
        rightBox.setWidthFull();
    }

    @Subscribe
    private void onInit(final InitEvent event) {
        createComponents();
    }

    private void createComponents() {
        createLeftComponents();
        createRightComponents();
    }

    private void createLeftComponents() {
        doCreateCards(getLeftCards(), leftContent);
        makeSortable(leftContent);
    }

    private void createRightComponents() {
        doCreateCards(getRightCards(), rightContent);
        makeSortable(rightContent);
    }

    private List<JmixCard> getLeftCards() {
        return List.of(
                createTotalOrdersCard(),
                createPaymentsCard(),
                createMyTasksCard(),
                createOverdueInvoicesCard());
    }

    private CrmCard createMyTasksCard() {
        return uiComponents.create(CrmCard.class)
                .fillAsPeriodCard("myTasks", 2, myTasksTitleComponent(), this::createMyTasksComponent)
                .withPeriodFilter(false)
                .withoutBackground(true);
    }

    private CrmCard createOverdueInvoicesCard() {
        return uiComponents.create(CrmCard.class)
                .fillAsPeriodCard("overdueInvoicesCard", 2,
                        messageBundle.getMessage("cards.overdueInvoices"), this::createOverdueInvoicesComponent)
                .withPeriodFilter(false)
                .withoutBackground(true);
    }

    private CrmCard createPaymentsCard() {
        return uiComponents.create(CrmCard.class)
                .withId("paymentsCard")
                .defaultRangeStatPeriodCard(
                        messageBundle.getMessage("cards.payments"),
                        this::createPaymentsComponent);
    }

    private CrmCard createTotalOrdersCard() {
        return uiComponents.create(CrmCard.class)
                .withId("totalOrdersCard")
                .defaultRangeStatPeriodCard(
                        messageBundle.getMessage("card.totalOrdersValue"),
                        this::createTotalOrdersValueComponent);
    }

    private Component myTasksTitleComponent() {
        var container = new HorizontalLayout();
        container.setWidthFull();
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        var title = new H4(messageBundle.getMessage("myTasks"));
        container.add(title);

        var newTaskButton = new Button(messageBundle.getMessage("newTask"));
        newTaskButton.setIcon(VaadinIcon.PLUS.create());
        newTaskButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newTaskButton.addClickListener(clickEvent ->
                dialogWindows.view(this, UserTaskListView.class)
                        .withViewConfigurer(UserTaskListView::detailOnly)
                        .withAfterCloseListener(closeEvent -> {
                            if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                                traverseComponents(this, component -> {
                                    if (component instanceof UserTaskListView userTaskListView) {
                                        userTaskListView.reloadData();
                                    }
                                });
                            }
                        }).open());
        container.add(newTaskButton);

        return container;
    }

    private List<JmixCard> getRightCards() {
        return List.of(createSalesCard(), createRecentActivitiesCard());
    }

    private CrmCard createRecentActivitiesCard() {
        return uiComponents.create(CrmCard.class)
                .withId("activitiesCard")
                .withoutBackground(true)
                .fillAsStaticCard("", 2, createRecentActivitiesComponent());
    }

    private CrmCard createSalesCard() {
        return uiComponents.create(CrmCard.class)
                .withId("salesCard")
                .fillAsPeriodCard(
                        messageBundle.getMessage("salesCardTitle"),
                        2, this::createSalesFunnelComponent);
    }

    private void doCreateCards(List<JmixCard> cards, JmixFormLayout form) {
        for (JmixCard card : cards) {
            card.addClassNames(Margin.Top.MEDIUM, Margin.Bottom.MEDIUM);
            form.add(card);
        }
    }

    private RangeStatCardInfo createTotalOrdersValueComponent(Period period) {
        var range = period.getDateRange(dateTimeService);
        var previousRange = period.getPreviousDateRangeFor(range);

        var sum = orderService.getOrders(range).stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var previousSum = orderService.getOrders(previousRange)
                .stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RangeStatCardInfo(
                range,
                PriceDataType.defaultFormat(sum),
                getDeltaString(sum, previousSum));
    }

    private RangeStatCardInfo createPaymentsComponent(Period period) {
        var range = period.getDateRange(dateTimeService);
        var previousRange = period.getPreviousDateRangeFor(range);

        var sum = paymentService.loadPayments(range).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var previousSum = paymentService.loadPayments(previousRange)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new RangeStatCardInfo(
                range,
                PriceDataType.defaultFormat(sum),
                getDeltaString(sum, previousSum));
    }

    private Component createOverdueInvoicesComponent(Period period) {
        CollectionContainer<Invoice> invoicesDc = dataComponents.createCollectionContainer(Invoice.class);
        CollectionLoader<Invoice> invoicesDl = dataComponents.createCollectionLoader();
        invoicesDl.setLoadDelegate(ctx ->
                invoiceService.getOverdueInvoices(period.getDateRange(dateTimeService), 30));
        invoicesDl.setContainer(invoicesDc);
        invoicesDl.load();

        ContainerDataGridItems<Invoice> gridItems = new ContainerDataGridItems<>(invoicesDc);

        DataGrid<Invoice> grid = uiComponents.create(DataGrid.class);
        grid.setDataProvider(gridItems);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setMinHeight(10, Unit.EM);
        grid.setMaxHeight(15, Unit.EM);
        grid.setEmptyStateText(messageBundle.getMessage("noOverdueInvoices"));
        grid.addItemClickListener(e ->
                dialogWindows.detail(this, Invoice.class).editEntity(e.getItem()).open());

        var clientColumn = grid.addColumn("client", metadata.getClass(Invoice.class).getPropertyPath("client"));
        clientColumn.setRenderer(crmRenderers.invoiceClientLink());
        clientColumn.setHeader(messages.getMessage(Client.class, "Client"));
        clientColumn.setFilterable(true);
        clientColumn.setSortable(true);

        localDateFormatter.setUseUserTimezone(true);
        localDateFormatter.setFormat(messages.getMessage("dateFormat"));

        var dueDateColumn = grid.addColumn("dueDate", metadata.getClass(Invoice.class).getPropertyPath("dueDate"));
        dueDateColumn.setRenderer(crmRenderers.invoiceDueDateRenderer());
        dueDateColumn.setHeader(messages.getMessage(Invoice.class, "Invoice.dueDate"));
        dueDateColumn.setFilterable(true);
        dueDateColumn.setSortable(true);

        return grid;
    }

    private Component createMyTasksComponent(Period period) {
        UserTaskListView userTasksView = views.create(UserTaskListView.class).gridOnly();
        userTasksView.setPadding(false);
        userTasksView.setMaxHeight(15, Unit.EM);
        return userTasksView;
    }

    private String getDeltaString(BigDecimal actualValue, BigDecimal previousValue) {
        if (previousValue.compareTo(BigDecimal.ZERO) == 0) {
            return actualValue.compareTo(BigDecimal.ZERO) == 0 ? "0%" : "↑100%";
        }

        var percentChange = actualValue.subtract(previousValue)
                .multiply(new BigDecimal("100"))
                .divide(previousValue, 2, RoundingMode.HALF_UP);

        return (percentChange.compareTo(BigDecimal.ZERO) >= 0 ? "↑" : "↓") + percentChange.abs() + "%";
    }

    private Component createSalesFunnelComponent(Period period) {
        var range = period.getDateRange(dateTimeService);
        var previousRange = period.getPreviousDateRangeFor(range);

        List<Order> orders = orderService.getOrders(range);
        List<Order> previousOrders = orderService.getOrders(previousRange);

        String delta;
        if (previousOrders.isEmpty()) {
            delta = orders.isEmpty() ? "0%" : "↑100%";
        } else {
            var percentChange = (orders.size() - previousOrders.size()) * 100.0 / previousOrders.size();
            delta = (percentChange >= 0 ? "↑" : "↓") + String.format("%.2f", Math.abs(percentChange)) + "%";
        }

        var statContent = new RangeStatCardInfo(range, orders.size() + " orders", delta).createDefaultContent();
        var chartContent = createSalesFunnelChartContent(orders);

        return new Div(statContent, chartContent);
    }

    private Component createSalesFunnelChartContent(List<Order> orders) {
        Chart chart = uiComponents.create(Chart.class)
                .withDataSet(createSalesChartDataSet(orders))
                .withSeries(new PieSeries()
                        .withLabel(new Label().withShow(false))
                        .withLabelLine(new PieSeries.LabelLine().withShow(false))
                        .withAnimation(true))
                .withTooltip(new Tooltip()
                        .withShow(true))
                .withToolbox(new Toolbox()
                        .withShow(true)
                        .withFeatures(new SaveAsImageFeature().withType(SaveAsImageFeature.SaveType.PNG)))
                .withTitle(new Title()
                        .withText(messageBundle.getMessage("salesChartTitle"))
                        .withTextStyle(new Title.TextStyle()
                                .withFontSize(12)
                                .withFontStyle(FontStyle.NORMAL)))
                .withLegend(new ScrollableLegend()
                        .withHeight("100")
                        .withTop("20")
                        .withLeft("0")
                        .withOrientation(Orientation.VERTICAL))
                .withGrid(new Grid()
                        .withShow(false)
                        .withBottom("0")
                        .withRight("0"));

        chart.setHeight(30, Unit.EM);
        setBackgroundTransparent(chart);

        var wrapper = chartsUtils.createViewStatChartWrapper(chart, false);
        wrapper.removeThemeVariants(CardVariant.values());
        wrapper.getStyle().setMarginTop("1em");
        setBackgroundTransparent(wrapper);

        return wrapper;
    }

    private DataSet createSalesChartDataSet(List<Order> orders) {
        var status2ordersAmount = new HashMap<OrderStatus, Integer>();
        orders.forEach(order -> status2ordersAmount.merge(order.getStatus(), 1, Integer::sum));

        var dataItems = new ArrayList<OrderStatusAmountItem>();
        for (Map.Entry<OrderStatus, Integer> entry : status2ordersAmount.entrySet()) {
            OrderStatusAmountValueDescription valueDescription = new OrderStatusAmountValueDescription(
                    messages.getMessage(entry.getKey()), entry.getValue());
            dataItems.add(new OrderStatusAmountItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<OrderStatusAmountItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("status")
                        .withValueField("amount")
        );
    }

    private Component createRecentActivitiesComponent() {
        return uiComponents.create(RecentActivitiesBlock.class);
    }
}