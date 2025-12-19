package com.company.crm.view.catalog;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.catalog.CatalogService;
import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.ui.component.card.CrmCard;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.app.util.ui.chart.ChartsUtils;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.category.CategoryRepository;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.CategoryItemRepository;
import com.company.crm.view.catalog.charts.ItemOrdersAmountItem;
import com.company.crm.view.catalog.charts.ItemOrdersAmountValueDescription;
import com.company.crm.view.main.MainView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.textfield.TypedTextField;
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
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.company.crm.app.util.ui.CrmUiUtils.addRowSelectionInMultiSelectMode;
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
    private CrmRenderers crmRenderers;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CategoryItemRepository repository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private CatalogService catalogService;

    @ViewComponent
    private JmixFormLayout chartsBlock;
    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private JmixSelect<Category> categorySelect;
    @ViewComponent
    private CollectionLoader<CategoryItem> categoryItemsDl;
    @ViewComponent
    private DataGrid<CategoryItem> categoryItemsDataGrid;

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Subscribe
    public void onInit(final InitEvent event) {
        initializeChartsBlock();
        initializeFilterFields();
        addRowSelectionInMultiSelectMode(categoryItemsDataGrid, "code");
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

    @Supply(to = "categoryItemsDataGrid.code", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridCodeRenderer() {
        return crmRenderers.categoryItemCode();
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

    private void initializeChartsBlock() {
        chartsUtils.initializeChartsAsync(getChartsLoaders());
    }

    private Map<Chart, Supplier<DataSet>> getChartsLoaders() {
        var chart2DataSetLoader = new HashMap<Chart, Supplier<DataSet>>();

        new ArrayList<Pair<Chart, Supplier<DataSet>>>() {{
            add(createBestOrderItemsChart());
            add(createOrderedThisMonthChart());
            // add(createAvailableByCategoryChart());
        }}.forEach(chart2Initializer -> {
            Chart chart = chart2Initializer.getFirst();
            Supplier<DataSet> dataSetSupplier = chart2Initializer.getSecond();
            CrmCard card = uiComponents.create(CrmCard.class);
            card.add(chart);
            chartsBlock.add(card);
            SkeletonStyler.apply(chart);
            chart2DataSetLoader.put(chart, dataSetSupplier);
        });
        return chart2DataSetLoader;
    }

    private Pair<Chart, Supplier<DataSet>> createBestOrderItemsChart() {
        Chart chart = chartsUtils.createViewStatPieChart("Best order items");
        return new Pair<>(chart, () -> createBestOrderItemsChartDataSet(null));
    }

    private Pair<Chart, Supplier<DataSet>> createOrderedThisMonthChart() {
        Chart chart = chartsUtils.createViewStatPieChart("Ordered this month");
        return new Pair<>(chart, () -> createBestOrderItemsChartDataSet(dateTimeService.getCurrentMonthRange()));
    }

    private Pair<Chart, Supplier<DataSet>> createAvailableByCategoryChart() {
        Chart chart = chartsUtils.createViewStatPieChart("Available by category");
        return new Pair<>(chart, this::createAvailableByCategoryChartDataSet);
    }

    private DataSet createBestOrderItemsChartDataSet(@Nullable LocalDateRange dateRange) {
        var dataItems = new ArrayList<ItemOrdersAmountItem>();

        for (Map.Entry<CategoryItem, BigDecimal> entry : catalogService.getBestItems(4, dateRange).entrySet()) {
            String key = entry.getKey().getName();
            BigDecimal value = entry.getValue();
            ItemOrdersAmountValueDescription valueDescription = new ItemOrdersAmountValueDescription(key, value);
            dataItems.add(new ItemOrdersAmountItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<ItemOrdersAmountItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("item")
                        .withValueField("amount")
        );
    }

    private DataSet createAvailableByCategoryChartDataSet() {
        return null;
    }
}