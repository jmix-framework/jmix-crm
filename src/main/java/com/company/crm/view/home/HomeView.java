package com.company.crm.view.home;

import com.company.crm.app.feature.sortable.SortableFeature;
import com.company.crm.app.service.user.UserService;
import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.service.finance.InvoiceService;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.service.ui.CrmUiComponents;
import com.company.crm.app.util.ui.component.model.card.CardModel;
import com.company.crm.app.util.ui.component.model.card.CardPeriod;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import com.company.crm.model.user.UserActivity;
import com.company.crm.app.util.ui.component.model.card.CardModel.AbstractCardContentModel;
import com.company.crm.app.util.ui.component.model.card.CardModel.CardContentModel;
import com.company.crm.app.util.ui.component.model.card.CardModel.ComponentCardContentModel;
import com.company.crm.app.util.ui.component.model.card.CardModel.DefaultCardContentModel;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.Grid;
import io.jmix.chartsflowui.kit.component.model.Title;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.PieSeries;
import io.jmix.chartsflowui.kit.component.model.shared.FontStyle;
import io.jmix.chartsflowui.kit.component.model.shared.Orientation;
import io.jmix.chartsflowui.kit.component.model.toolbox.SaveAsImageFeature;
import io.jmix.chartsflowui.kit.component.model.toolbox.Toolbox;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.splitlayout.JmixSplitLayout;
import io.jmix.flowui.facet.SettingsFacet;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

@Route(value = "home", layout = MainView.class)
@ViewController(id = "HomeView")
@ViewDescriptor(path = "home-view.xml")
public class HomeView extends StandardView implements WidthResizeListener {

    private static final DateTimeFormatter DATE_WITHOUT_YEAR =
            DateTimeFormatter.ofPattern("dd MMM");

    private static final DateTimeFormatter DATE_WITH_YEAR =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final DateTimeFormatter DATE_WITH_YEAR_AND_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private DateTimeService dateTimeService;

    @Autowired
    private Messages messages;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CrmUiComponents crmUiComponents;

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

    private volatile int lastProcessedWidth = -1;

    @Override
    public void configureUiForWidth(int width) {
        if (needToReconfigureUi(width)) {
            lastProcessedWidth = width;

            var breakpoint = 1200;
            if (width < breakpoint) {
                configureUiForSmallWidth();
            } else {
                configureUiForNormalWidth();
            }
        }
    }

    private boolean needToReconfigureUi(int width) {
        return width != lastProcessedWidth
                || (width >= 1000 && lastProcessedWidth <= 0 && lastProcessedWidth >= 1000)
                || (width <= 1000 && lastProcessedWidth <= 0 && lastProcessedWidth <= 1000);
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
        doCreateCards(getLeftCardsModels(), leftContent);
        SortableFeature.makeSortable(leftContent);
    }

    private void createRightComponents() {
        doCreateCards(getRightCardsModels(), rightContent);
        SortableFeature.makeSortable(rightContent);
    }

    private List<CardModel> getLeftCardsModels() {
        return List.of(
                new CardModel("totalOrdersValue", 1, "Total Orders Value", this::createTotalOrdersValueComponent),
                new CardModel("payments", 1, "Payments", this::createPaymentsComponent),
                new CardModel("overdueInvoices", 2, "Overdue Invoices", this::createOverdueInvoicesComponent),
                new CardModel("myTasks", 2, myTasksTitleComponent(), this::createMyTasksComponent)
        );
    }

    private static Component myTasksTitleComponent() {
        var hbox = new HorizontalLayout();
        hbox.setWidthFull();
        hbox.setAlignItems(FlexComponent.Alignment.CENTER);
        hbox.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        var title = new H4("My Tasks");
        hbox.add(title);

        var newTaskButton = new Button("New Task");
        newTaskButton.setIcon(VaadinIcon.PLUS.create());
        newTaskButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        hbox.add(newTaskButton);

        return hbox;
    }

    private List<CardModel> getRightCardsModels() {
        return List.of(
                new CardModel("salesChart", 2, "Sales Chart", this::createSalesFunnelComponent),
                new CardModel("recentActivities", 2, "Recent Activities", this::createRecentActivitiesComponent)
        );
    }

    private void doCreateCards(List<CardModel> cardModels, JmixFormLayout form) {
        for (CardModel cardModel : cardModels) {
            JmixCard card = crmUiComponents.createCrmComponent(cardModel);
            card.addClassName(LumoUtility.Margin.Top.MEDIUM);
            form.add(card);
        }
    }

    private CardContentModel createTotalOrdersValueComponent(CardPeriod period) {
        return contentModelForPriceSum(period,
                (startDate, endDate) ->
                        orderService.loadOrders(startDate, endDate).stream().map(Order::getTotal),
                sum -> "↑20.82%"); // TODO
    }

    private CardContentModel createPaymentsComponent(CardPeriod period) {
        return contentModelForPriceSum(period,
                (startDate, endDate) ->
                        paymentService.loadPayments(startDate, endDate).stream().map(Payment::getAmount),
                sum -> "↓-200,000$"); // TODO
    }

    private CardContentModel contentModelForPriceSum(CardPeriod period,
                                                     BiFunction<LocalDate, LocalDate, Stream<BigDecimal>> pricesProvider,
                                                     Function<BigDecimal, String> statInfoProvider) {
        Pair<LocalDate, LocalDate> dateRange = getDateRangeBy(period);
        LocalDate startDate = dateRange.getFirst();
        LocalDate endDate = dateRange.getSecond();

        var sum = BigDecimal.ZERO;
        for (BigDecimal it : pricesProvider.apply(startDate, endDate).toList()) {
            sum = sum.add(it);
        }

        var statInfo = statInfoProvider.apply(sum);

        DefaultCardContentModel contentModel = new DefaultCardContentModel(
                PriceDataType.formatValue(sum),
                "%s - %s".formatted(
                        DATE_WITHOUT_YEAR.format(startDate),
                        DATE_WITH_YEAR.format(endDate)),
                statInfo
        );

        return withDefaultBackgroundCallback(contentModel, statInfo);
    }

    private CardContentModel withDefaultBackgroundCallback(CardContentModel contentModel) {
        return withDefaultBackgroundCallback(contentModel, "");
    }

    private CardContentModel withDefaultBackgroundCallback(CardContentModel contentModel, String statInfo) {
        if (contentModel instanceof AbstractCardContentModel<?> abstractCardContentModel) {
            return abstractCardContentModel.withCreationCallback(card -> {
                if (statInfo.startsWith("↑")) {
                    setLinearGradient(card, "var(--lumo-primary-color-10pct)");
                } else if (statInfo.startsWith("↓")) {
                    setLinearGradient(card, "var(--lumo-error-color-10pct)");
                } else {
                    setLinearGradient(card, "var(--lumo-contrast-10pct)");
                }
            });
        } else {
            return contentModel;
        }
    }

    private void setLinearGradient(Component component, int deg, String color1, String color2) {
        component.getStyle().set("background", "linear-gradient(%ddeg, %s, %s)".formatted(deg, color1, color2));
    }

    private void setLinearGradient(Component component, String color) {
        setLinearGradient(component, 45, "var(--lumo-base-color)", color);
    }

    private CardContentModel createOverdueInvoicesComponent(CardPeriod period) {
        DataGrid<Invoice> grid = uiComponents.create(DataGrid.class);

        grid.addColumn(new ComponentRenderer<>(r -> new Span(r.getClient().getFullName())))
                .setHeader("Client");
        grid.addColumn(new ComponentRenderer<>(r -> new Span(DATE_WITH_YEAR.format(r.getDueDate()))))
                .setHeader("Due Date");

        grid.setItems(invoiceService.getOverdueInvoices());

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setMinHeight(10, Unit.EM);
        grid.setMaxHeight(15, Unit.EM);
        grid.setEmptyStateText("No overdue invoices");

        return withDefaultBackgroundCallback(new ComponentCardContentModel(grid));
    }

    private CardContentModel createMyTasksComponent(CardPeriod period) {
        return withDefaultBackgroundCallback(
                new ComponentCardContentModel(new Span("My Tasks Content"))
        );
    }

    private CardContentModel createSalesFunnelComponent(CardPeriod period) {
        Pair<LocalDate, LocalDate> dateRange = getDateRangeBy(period);
        LocalDate startDate = dateRange.getFirst();
        LocalDate endDate = dateRange.getSecond();

        List<Order> orders = orderService.loadOrders(startDate, endDate);

        String statInfo = "↑20.82%"; // TODO

        CardContentModel mainContent = new DefaultCardContentModel(
                orders.size() + " orders",
                "%s - %s".formatted(
                        DATE_WITHOUT_YEAR.format(startDate),
                        DATE_WITH_YEAR.format(endDate)),
                statInfo
        );

        CardContentModel chartContent = createSalesFunnelChartContent(orders);

        return withDefaultBackgroundCallback(mainContent.composeWith(chartContent), statInfo);
    }

    private CardContentModel createSalesFunnelChartContent(List<Order> orders) {
        Chart chart = uiComponents.create(Chart.class)
                .withDataSet(createSalesChartDataSet(orders))
                .withSeries(new PieSeries()
                        .withAnimation(true))
                .withToolbox(new Toolbox()
                        .withShow(true)
                        .withFeatures(new SaveAsImageFeature().withType(SaveAsImageFeature.SaveType.PNG)))
                .withTitle(new Title()
                        .withText("STATUS")
                        .withTextStyle(new Title.TextStyle()
                                .withFontSize(12)
                                .withFontStyle(FontStyle.NORMAL)))
                .withLegend(new Legend()
                        .withTop("20")
                        .withLeft("0")
                        .withOrientation(Orientation.VERTICAL))
                .withGrid(new Grid()
                        .withShow(false)
                        .withBottom("0")
                        .withRight("0"));

        chart.setHeight(30, Unit.EM);

        Div container = new Div(chart);
        container.getStyle().setMarginTop("1em");

        return new ComponentCardContentModel(container);
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
    
    private CardContentModel createRecentActivitiesComponent(CardPeriod period) {
        Div container = new Div();

        container.add(new H5("Today"));
        LocalDate todayStart = dateTimeService.getDayStart().toLocalDate();
        for (UserActivity todayActivity : userService.loadActivities(todayStart, 3)) {
            container.add(createActivityRow(todayActivity));
        }

        container.add(new H5("Yesterday"));
        LocalDate yesterdayStart = todayStart.minusDays(1);
        for (UserActivity yesterdayActivity : userService.loadActivities(yesterdayStart, 3)) {
            container.add(createActivityRow(yesterdayActivity));
        }

        return withDefaultBackgroundCallback(
                new ComponentCardContentModel(container)
                        .withHasPeriodFilter(false)
                        .withHasEllipsisButton(false)
        );
    }
    private Component createActivityRow(UserActivity activity) {
        User user = activity.getUser();
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);

        Avatar avatar = new Avatar(user.getFirstName().substring(0, 1));
        row.add(avatar);

        Span userNameSpan = new Span(user.getFullNameName());
        userNameSpan.addClassNames(LumoUtility.TextColor.BODY);

        Span activityDescriptionSpan = new Span(activity.getActionDescription());
        activityDescriptionSpan.addClassNames(LumoUtility.TextColor.TERTIARY);

        Span dateSpan = new Span(DATE_WITH_YEAR_AND_TIME.format(activity.getCreatedDate()));
        dateSpan.addClassNames(LumoUtility.TextColor.TERTIARY);

        Div activityInfoBlock = new Div(new HorizontalLayout(userNameSpan, activityDescriptionSpan), dateSpan);
        row.add(activityInfoBlock);

        return row;
    }

    private Pair<LocalDate, LocalDate> getDateRangeBy(CardPeriod period) {
        switch (period) {
            case WEEK -> {
                return new Pair<>(
                        dateTimeService.getWeekStart().toLocalDate(),
                        dateTimeService.getWeekEnd().toLocalDate()
                );
            }
            case MONTH -> {
                return new Pair<>(
                        dateTimeService.getMonthStart().toLocalDate(),
                        dateTimeService.getMonthEnd().toLocalDate()
                );
            }
            case YEAR -> {
                return new Pair<>(
                        dateTimeService.getYearStart().toLocalDate(),
                        dateTimeService.getYearEnd().toLocalDate()
                );
            }
            default -> throw new IllegalStateException("Unexpected value: " + period);
        }
    }
}