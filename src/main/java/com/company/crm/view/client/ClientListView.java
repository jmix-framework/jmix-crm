package com.company.crm.view.client;

import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.user.UserService;
import com.company.crm.app.util.ui.component.chart.ChartsUtils;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.user.User;
import com.company.crm.view.util.SkeletonStyler;
import com.company.crm.view.client.charts.buyers.ClientPurchasesSumItem;
import com.company.crm.view.client.charts.buyers.ClientPurchasesSumValueDescription;
import com.company.crm.view.client.charts.categories.ClientCategoryInfo;
import com.company.crm.view.client.charts.categories.ClientCategoryItem;
import com.company.crm.view.client.charts.categories.ClientCategoryValueDescription;
import com.company.crm.view.client.charts.type.ClientTypeAmountItem;
import com.company.crm.view.client.charts.type.ClientTypeAmountValueDescription;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.router.Route;
import io.jmix.chartsflowui.component.Chart;
import io.jmix.chartsflowui.kit.component.model.DataSet;
import io.jmix.chartsflowui.kit.data.chart.ListChartItems;
import io.jmix.core.Messages;
import io.jmix.core.common.datastruct.Pair;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.details.JmixDetails;
import io.jmix.flowui.component.genericfilter.GenericFilter;
import io.jmix.flowui.component.pagination.SimplePagination;
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

import static com.company.crm.app.util.ui.listener.resize.WidthResizeListener.isWidthChanged;
import static io.jmix.core.querycondition.PropertyCondition.contains;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.isCollectionEmpty;

@Route(value = "clients", layout = MainView.class)
@ViewController(id = "Client.list")
@ViewDescriptor(path = "client-list-view.xml")
@LookupComponent("clientsDataGrid")
@DialogMode(width = "64em")
public class ClientListView extends StandardListView<Client> implements WidthResizeListener {

    @Autowired
    private UserService userService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private ClientRepository repository;

    @Autowired
    private Messages messages;
    @Autowired
    private ChartsUtils chartsUtils;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private SimplePagination pagination;
    @ViewComponent
    private FormLayout chartsBlock;
    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private JmixSelect<User> accountManagerSelect;
    @ViewComponent
    private CollectionLoader<Client> clientsDl;
    @ViewComponent
    private JmixSelect<ClientType> typeSelect;
    @ViewComponent
    private Switch showOnlyMyClientsCheckBox;
    @ViewComponent
    private JmixSelect<ClientCategory> categorySelect;
    @ViewComponent
    private GenericFilter advancedFilter;

    private volatile int lastWidth = -1;
    private static final int widthBreakpoint = 600;
    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Override
    public void configureUiForWidth(int width) {
        if (isWidthChanged(width, lastWidth, widthBreakpoint)) {
            lastWidth = width;
            configureFiltersPanel(width);
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        initializeFilterFields();
        chartsUtils.initializeChartsAsync(getChartsLoaders());
    }

    @Install(to = "clientsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Client> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return repository.findAll(pageable, wrapContext(context)).getContent();
    }

    @Install(to = "clientsDataGrid.removeAction", subject = "delegate")
    private void clientsDataGridRemoveDelegate(final Collection<Client> collection) {
        repository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return repository.count(wrapContext(context));
    }

    @Subscribe("showOnlyMyClientsCheckBox")
    private void onShowOnlyMyClientsCheckBoxComponentValueChange(final ComponentValueChangeEvent<Switch, Boolean> event) {
        boolean isFromClient = event.isFromClient();
        if (!isFromClient) {
            return;
        }

        if (event.getValue()) {
            accountManagerSelect.setValue(getCurrentUser());
        } else {
            accountManagerSelect.setValue(null);
        }
    }

    @Subscribe("accountManagerSelect")
    private void onAccountManagerSelectComponentValueChange(final ComponentValueChangeEvent<JmixSelect<User>, User> event) {
        User currentSelection = event.getValue();
        showOnlyMyClientsCheckBox.setValue(currentSelection != null && currentSelection.equals(getCurrentUser()));
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

    private void initializeFilterFields() {
        List<User> accountManagers = new ArrayList<>(userService.loadAccountManagers());
        accountManagers.addFirst(getCurrentUser());
        accountManagerSelect.setItems(accountManagers);

        List.<HasValue<?, ?>>of(searchField, typeSelect, accountManagerSelect, categorySelect)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));
    }

    private void applyFilters() {
        updateFiltersCondition();
        clientsDl.load();
    }

    private void updateFiltersCondition() {
        filtersCondition.getConditions().clear();
        addSearchByNameCondition();
        addSearchByTypeCondition();
        addSearchByManagerCondition();
        addSearchByCategoryCondition();
    }

    private void addSearchByNameCondition() {
        searchField.getOptionalValue().ifPresent(name ->
                filtersCondition.add(contains("name", name)));
    }

    private void addSearchByTypeCondition() {
        typeSelect.getOptionalValue().ifPresent(type ->
                filtersCondition.add(equal("type", type)));
    }

    private void addSearchByManagerCondition() {
        accountManagerSelect.getOptionalValue().ifPresent(manager ->
                filtersCondition.add(equal("accountManager", manager)));
    }

    private void addSearchByCategoryCondition() {
        categorySelect.getOptionalValue().ifPresent(value -> {
            switch (value) {
                case WITH_ORDERS -> filtersCondition.add(isCollectionEmpty("orders", false));
                // FIXME: distinct does not work here for some reason
                case WITH_PAYMENTS -> filtersCondition.add(isCollectionEmpty("invoices.payments", false));
            }
        });
    }

    private void configureFiltersPanel(int width) {
        if (width < widthBreakpoint) {
            searchField.setWidthFull();
        } else {
            searchField.setWidth("50%");
        }
    }

    private Map<Chart, Supplier<DataSet>> getChartsLoaders() {
        var chart2DataSetLoader = new HashMap<Chart, Supplier<DataSet>>();

        new ArrayList<Pair<Chart, Supplier<DataSet>>>() {{
            add(createBestBuyersChart());
            add(createClientCategoriesChart());
            add(createClientsTypesChart());
        }}.forEach(chart2Initializer -> {
            Chart chart = chart2Initializer.getFirst();
            Supplier<DataSet> dataSetSupplier = chart2Initializer.getSecond();
            chartsBlock.add(chart);
            SkeletonStyler.apply(chart);
            chart2DataSetLoader.put(chart, dataSetSupplier);
        });
        return chart2DataSetLoader;
    }

    private Pair<Chart, Supplier<DataSet>> createBestBuyersChart() {
        Chart chart = chartsUtils.createDefaulListViewTopChart("Best Buyers");
        return new Pair<>(chart, this::createBestBuyersChartDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createClientCategoriesChart() {
        Chart chart = chartsUtils.createDefaulListViewTopChart("By Category");
        return new Pair<>(chart, this::createClientsCategoriesDataSet);
    }

    private Pair<Chart, Supplier<DataSet>> createClientsTypesChart() {
        Chart chart = chartsUtils.createDefaulListViewTopChart("By Type");
        return new Pair<>(chart, this::createClientsByTypeChartDataSet);
    }

    private DataSet createBestBuyersChartDataSet() {
        var dataItems = new ArrayList<ClientPurchasesSumItem>();

        for (Map.Entry<Client, BigDecimal> entry : clientService.getBestBuyers(3).entrySet()) {
            ClientPurchasesSumValueDescription valueDescription = new ClientPurchasesSumValueDescription(
                    entry.getKey().getName(), entry.getValue());
            dataItems.add(new ClientPurchasesSumItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<ClientPurchasesSumItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("clientName")
                        .withValueField("purchasesSum")
        );
    }

    private DataSet createClientsCategoriesDataSet() {
        var dataItems = new ArrayList<ClientCategoryItem>();

        for (ClientCategoryInfo category : getClientCategories()) {
            ClientCategoryValueDescription valueDescription = new ClientCategoryValueDescription(category);
            dataItems.add(new ClientCategoryItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<ClientCategoryItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("category")
                        .withValueField("value")
        );
    }

    private DataSet createClientsByTypeChartDataSet() {
        var dataItems = new ArrayList<ClientTypeAmountItem>();

        for (Map.Entry<ClientType, List<Client>> entry : clientService.loadClientsByType().entrySet()) {
            ClientTypeAmountValueDescription valueDescription = new ClientTypeAmountValueDescription(
                    messages.getMessage(entry.getKey()), entry.getValue().size());
            dataItems.add(new ClientTypeAmountItem(valueDescription));
        }

        return new DataSet().withSource(
                new DataSet.Source<ClientTypeAmountItem>()
                        .withDataProvider(new ListChartItems<>(dataItems))
                        .withCategoryField("type")
                        .withValueField("amount")
        );
    }

    private List<ClientCategoryInfo> getClientCategories() {
        return new ArrayList<>() {{
            add(new ClientCategoryInfo("Total", pagination.getPaginationLoader().getCount()));
            add(new ClientCategoryInfo("With orders", clientService.loadClientsWithOrders().size()));
            add(new ClientCategoryInfo("With payments", clientService.loadClientsWithPayments().size()));
        }};
    }

    private User getCurrentUser() {
        return ((User) currentAuthentication.getUser());
    }
}