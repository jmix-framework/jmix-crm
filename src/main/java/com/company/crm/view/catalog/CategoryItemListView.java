package com.company.crm.view.catalog;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.util.ui.component.chart.ChartsUtils;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.model.order.OrderItem;
import com.company.crm.view.catalog.charts.OrderItemOrdersAmountItem;
import com.company.crm.view.catalog.charts.OrderItemOrdersAmountValueDescription;
import com.company.crm.view.main.MainView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.jmix.core.querycondition.PropertyCondition.contains;
import static io.jmix.core.querycondition.PropertyCondition.equal;

@Route(value = "category-items", layout = MainView.class)
@ViewController(id = "CategoryItem.list")
@ViewDescriptor(path = "category-item-list-view.xml")
@LookupComponent("categoryItemsDataGrid")
@DialogMode(width = "64em")
public class CategoryItemListView extends StandardListView<CategoryItem> {

    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CategoryItemRepository repository;
    @Autowired
    private CategoryRepository categoryRepository;

    @ViewComponent
    private JmixFormLayout chartsBlock;
    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private JmixSelect<Category> categorySelect;
    @ViewComponent
    private CollectionLoader<CategoryItem> categoryItemsDl;

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Subscribe
    public void onInit(final InitEvent event) {
        initializeFilterFields();
        chartsUtils.initializeChartsAsync(getChartsLoaders());
    }

    @Install(to = "categoryItemsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<CategoryItem> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, wrapContext(context)).getContent();
    }

    @Install(to = "categoryItemsDataGrid.removeAction", subject = "delegate")
    private void categoryItemsDataGridRemoveDelegate(final Collection<CategoryItem> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(wrapContext(context));
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

    private void updateFiltersCondition() {
        filtersCondition.getConditions().clear();
        addSearchByNameCondition();
        addSearchByCategoryCondition();
    }

    private void addSearchByNameCondition() {
        searchField.getOptionalValue().ifPresent(name ->
                filtersCondition.add(contains("name", name)));
    }

    private void addSearchByCategoryCondition() {
        categorySelect.getOptionalValue().ifPresent(category ->
                filtersCondition.add(equal("category", category)));
    }

    private void initializeFilterFields() {
        List<Category> categories = categoryRepository.findAll();
        categorySelect.setItems(categories);

        List.<HasValue<?, ?>>of(searchField, categorySelect)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(searchField)
                .addEntitySelectBinding(categorySelect, categories);
    }

    private void applyFilters() {
        updateFiltersCondition();
        categoryItemsDl.load();
    }

    private Map<Chart, Supplier<DataSet>> getChartsLoaders() {
        var chart2DataSetLoader = new HashMap<Chart, Supplier<DataSet>>();

        new ArrayList<Pair<Chart, Supplier<DataSet>>>() {{
            add(createBestOrderItemsChart());
            add(createOrderedThisMonthChart());
            add(createAvailableByCategoryChart());
        }}.forEach(chart2Initializer -> {
            Chart chart = chart2Initializer.getFirst();
            Supplier<DataSet> dataSetSupplier = chart2Initializer.getSecond();
            chartsBlock.add(chart);
            SkeletonStyler.apply(chart);
            chart2DataSetLoader.put(chart, dataSetSupplier);
        });
        return chart2DataSetLoader;
    }

    private Pair<Chart, Supplier<DataSet>> createBestOrderItemsChart() {
        Chart chart = chartsUtils.createDefaulListViewTopChart("Best order items");
        return new Pair<>(chart, this::createBestOrderItemsChartDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createOrderedThisMonthChart() {
        Chart chart = chartsUtils.createDefaulListViewTopChart("Ordered this month");
        return new Pair<>(chart, this::createBestOrderItemsChartDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createAvailableByCategoryChart() {
        Chart chart = chartsUtils.createDefaulListViewTopChart("Available by category");
        return new Pair<>(chart, this::createBestOrderItemsChartDataSet);
    }

    private DataSet createBestOrderItemsChartDataSet() {
        var dataItems = new ArrayList<OrderItemOrdersAmountItem>();

        for (Map.Entry<OrderItem, BigDecimal> entry : orderService.getBestOrderItems(3).entrySet()) {
            OrderItemOrdersAmountValueDescription valueDescription = new OrderItemOrdersAmountValueDescription(
                    entry.getKey().getCategoryItem().getName(), entry.getValue());
            dataItems.add(new OrderItemOrdersAmountItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<OrderItemOrdersAmountItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("itemName")
                        .withValueField("amount")
        );
    }
}