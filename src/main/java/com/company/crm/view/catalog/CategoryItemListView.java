package com.company.crm.view.catalog;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.catalog.CatalogImportSettings;
import com.company.crm.app.service.catalog.CatalogService;
import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.util.constant.CrmConstants;
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
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.renderer.Renderer;
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
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
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
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.company.crm.app.util.ui.datacontext.DataContextUtils.wrapCondition;
import static io.jmix.core.querycondition.PropertyCondition.contains;
import static io.jmix.core.querycondition.PropertyCondition.equal;

@Route(value = "products", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CATEGORY_ITEM_LIST)
@ViewDescriptor(path = "category-item-list-view.xml")
@LookupComponent("categoryItemsDataGrid")
@DialogMode(width = "64em", resizable = true)
public class CategoryItemListView extends StandardListView<CategoryItem> {

    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private CategoryItemRepository itemRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private DateTimeService dateTimeService;
    @Autowired
    private CatalogService catalogService;

    @ViewComponent
    private MessageBundle messageBundle;
    @ViewComponent
    private JmixFormLayout chartsBlock;
    @ViewComponent
    private TypedTextField<String> items_searchField;
    @ViewComponent
    private JmixSelect<Category> items_categorySelect;
    @ViewComponent
    private CollectionLoader<CategoryItem> categoryItemsDl;
    @ViewComponent
    private FileUploadField importCatalogField;

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Subscribe("importCatalogField")
    public void onImportCatalogFieldFileUploadSucceeded(FileUploadSucceededEvent<FileUploadField> event) {
        byte[] content = importCatalogField.getValue();
        if (content != null) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            CatalogImportSettings importSettings = new CatalogImportSettings(inputStream);
            catalogService.updateCatalog(importSettings);
            categoryItemsDl.load();
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initialize();
    }

    @Install(to = "categoryItemsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<CategoryItem> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return itemRepository.findAll(pageable, wrapCondition(context, filtersCondition)).getContent();
    }

    @Install(to = "items_pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return itemRepository.count(wrapCondition(context, filtersCondition));
    }

    @Install(to = "categoryItemsDataGrid.removeAction", subject = "delegate")
    private void categoryItemsDataGridRemoveDelegate(final Collection<CategoryItem> collection) {
        itemRepository.deleteAll(collection);
    }

    @Supply(to = "categoryItemsDataGrid.name", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridNameRenderer() {
        return crmRenderers.entityLink(Function.identity());
    }

    @Supply(to = "categoryItemsDataGrid.category", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridCategoryRenderer() {
        return crmRenderers.entityLink(CategoryItem::getCategory);
    }

    @Supply(to = "categoryItemsDataGrid.code", subject = "renderer")
    private Renderer<CategoryItem> categoryItemsDataGridCodeRenderer() {
        return crmRenderers.categoryItemCode();
    }

    private void initialize() {
        loadData();
        initializeChartsBlock();
        initializeFilterFields();
    }

    private void loadData() {
        categoryItemsDl.load();
    }

    private void updateFiltersCondition() {
        filtersCondition.getConditions().clear();
        addSearchByNameCondition();
        addSearchByCategoryCondition();
    }

    private void addSearchByNameCondition() {
        items_searchField.getOptionalValue().ifPresent(name ->
                filtersCondition.add(contains("name", name)));
    }

    private void addSearchByCategoryCondition() {
        items_categorySelect.getOptionalValue().ifPresent(category ->
                filtersCondition.add(equal("category", category)));
    }

    private void initializeFilterFields() {
        List<Category> categories = categoryRepository.findAll();
        items_categorySelect.setItems(categories);

        List.<HasValue<?, ?>>of(items_searchField, items_categorySelect)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(items_searchField)
                .addEntitySelectBinding(items_categorySelect, () -> categories)
                .build();
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
        }}.forEach(chart2Initializer -> {
            Chart chart = chart2Initializer.getFirst();
            Supplier<DataSet> dataSetSupplier = chart2Initializer.getSecond();
            chartsBlock.add(chartsUtils.createViewStatChartWrapper(chart));
            chart2DataSetLoader.put(chart, dataSetSupplier);
        });
        return chart2DataSetLoader;
    }

    private Pair<Chart, Supplier<DataSet>> createBestOrderItemsChart() {
        Chart chart = chartsUtils.createViewStatPieChart(messageBundle.getMessage("bestSelling"));
        return new Pair<>(chart, () -> createBestOrderItemsChartDataSet(null));
    }

    private Pair<Chart, Supplier<DataSet>> createOrderedThisMonthChart() {
        Chart chart = chartsUtils.createViewStatPieChart(messageBundle.getMessage("sellingThisMonth"));
        return new Pair<>(chart, () -> createBestOrderItemsChartDataSet(dateTimeService.getCurrentMonthRange()));
    }

    private DataSet createBestOrderItemsChartDataSet(@Nullable LocalDateRange dateRange) {
        var dataItems = new ArrayList<ItemOrdersAmountItem>();

        for (Map.Entry<CategoryItem, BigDecimal> entry : catalogService.getBestOrderedItems(4, dateRange).entrySet()) {
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
}