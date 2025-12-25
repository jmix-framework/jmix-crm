package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.tab.TabIndexUrlQueryParameterBinder;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.client.CompletedOrdersByDateRangeInfo;
import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.ui.component.RecentActivitiesBlock;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.app.util.ui.chart.ChartsUtils;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.user.activity.UserActivity;
import com.company.crm.model.user.activity.client.ClientUserActivityRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.data.item.SimpleDataItem;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.component.model.axis.SplitLine;
import io.jmix.chartsflowui.kit.component.model.legend.Legend;
import io.jmix.chartsflowui.kit.component.model.series.SeriesType;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.splitlayout.JmixSplitLayout;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

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
    private ClientService clientService;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private ClientUserActivityRepository userActivityRepository;

    @ViewComponent
    private JmixTabSheet tabSheet;
    @ViewComponent
    private RecentActivitiesBlock recentActivities;
    @ViewComponent
    private JmixSplitLayout formSplit;
    @ViewComponent
    private JmixFormLayout chartsBlock;

    @SuppressWarnings("FieldCanBeLocal")
    private final int loadStatsForLastYearsAmount = 3;
    @Autowired
    private Notifications notifications;

    @Override
    public void configureUiForWidth(int width) {
        formSplit.setOrientation(width > 900 ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    @Subscribe
    private void onInit(final InitEvent event) {
        TabIndexUrlQueryParameterBinder.register(this, tabSheet);
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        recentActivities.setMaxWidth(27.5f, Unit.EM);
        recentActivities.setClient(getEditedEntity());
        chartsUtils.initializeChartsAsync(getChartsLoaders());
    }

    @Install(to = "clientDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Client> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return clientRepository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(clientRepository.save(getEditedEntity()));
    }

    @Install(to = "activitiesDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<? extends UserActivity> activitiesDlLoadFromRepositoryDelegate(final Pageable pageable, final JmixDataRepositoryContext ctx) {
        return userActivityRepository.findAll(pageable, ctx).getContent();
    }

    @Subscribe("downloadProfile")
    private void onDownloadProfile(final ActionPerformedEvent event) {
        // TODO: download custom design-time report
        notifications.create("Client profile downloaded successfully")
                .withType(Notifications.Type.SUCCESS)
                .show();
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

            chartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
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