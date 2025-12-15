package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.service.user.UserService;
import com.company.crm.app.ui.component.card.CrmCard;
import com.company.crm.app.util.AsyncTasksRegistry;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.user.User;
import com.company.crm.view.main.MainView;
import com.company.crm.view.user.UserDetailView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.asynctask.UiAsyncTasks.SupplierConfigurer;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.formlayout.JmixFormLayout;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.select.JmixSelect;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.LookupComponent;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.company.crm.app.feature.sortable.SortableFeature.makeSortable;
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
    private OrderService orderService;
    @Autowired
    private ClientService clientService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private ClientRepository repository;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    // stats
    @ViewComponent
    private JmixFormLayout statsBlock;
    @ViewComponent
    private CrmCard ordersTotalSumCard;
    @ViewComponent
    private CrmCard averageBillCard;
    @ViewComponent
    private CrmCard paymentsTotalSumCard;

    // filters
    @ViewComponent
    private TypedTextField<String> searchField;
    @ViewComponent
    private JmixSelect<User> accountManagerSelect;
    @ViewComponent
    private JmixSelect<ClientType> typeSelect;
    @ViewComponent
    private Switch showOnlyMyClientsCheckBox;
    @ViewComponent
    private JmixSelect<ClientCategory> categorySelect;

    @ViewComponent
    private CollectionLoader<Client> clientsDl;
    @ViewComponent
    private DataGrid<Client> clientsDataGrid;

    private static volatile int lastWidth = -1;
    private static final int widthBreakpoint = 600;

    private final AsyncTasksRegistry asyncTasksRegistry = AsyncTasksRegistry.newInstance();

    private final LogicalCondition filtersCondition = LogicalCondition.and();
    @Autowired
    private CrmRenderers crmRenderers;

    @Override
    public void configureUiForWidth(int width) {
        if (isWidthChanged(width, lastWidth, widthBreakpoint)) {
            lastWidth = width;
            configureFiltersPanel(width);
        }
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        initializeStatsBlock();
        initializeFilterFields();
        addDetachListener(e -> asyncTasksRegistry.cancelAll());
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

    @Subscribe("clientsDataGrid")
    private void onClientsDataGridSelection(final SelectionEvent<DataGrid<Client>, Client> event) {
        calculateCardsValues(event.getAllSelectedItems().toArray(new Client[0]));
    }

    @Supply(to = "clientsDataGrid.accountManager", subject = "renderer")
    private Renderer<Client> clientsDataGridAccountManagerRenderer() {
        return crmRenderers.accountManagerLink();
    }

    @Supply(to = "clientsDataGrid.name", subject = "renderer")
    private Renderer<Client> clientsDataGridNameRenderer() {
        return crmRenderers.clientLink();
    }

    @Supply(to = "clientsDataGrid.type", subject = "renderer")
    private Renderer<Client> clientsDataGridTypeRenderer() {
        return crmRenderers.clientType();
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

    private void initializeStatsBlock() {
        makeSortable(statsBlock);
        calculateCardsValues();
    }

    private void calculateCardsValues() {
        calculateCardsValues(getSelectedClients());
    }

    private void calculateCardsValues(Client... selectedClients) {
        updateOrdersTotalSumCard(selectedClients);
        updatePaymentsTotalSumCard(selectedClients);
        updateAverageBillCard(selectedClients);
    }

    private void updateOrdersTotalSumCard(Client... clients) {
        installCardLoader(ordersTotalSumCard);
        scheduleOrdersTotalSumCalculating(clients);
    }

    private void updatePaymentsTotalSumCard(Client... clients) {
        installCardLoader(paymentsTotalSumCard);
        schedulePaymentsTotalSumCalculating(clients);
    }

    private void updateAverageBillCard(Client... clients) {
        installCardLoader(averageBillCard);
        scheduleAverageBillCalculating(clients);
    }

    private void installCardLoader(Card card) {
        card.removeAll();
        card.add("Loading...");
        SkeletonStyler.apply(card);
    }

    private Client[] getSelectedClients() {
        return clientsDataGrid.getSelectedItems().toArray(new Client[0]);
    }

    private void scheduleOrdersTotalSumCalculating(Client... clients) {
        SupplierConfigurer<?> task =
                uiAsyncTasks.supplierConfigurer(() -> calculateOrdersTotalSum(clients))
                        .withExceptionHandler(e -> SkeletonStyler.remove(ordersTotalSumCard))
                        .withResultHandler(ordersTotalSum ->
                                fillStatCard("Orders Total", ordersTotalSumCard, ordersTotalSum));
        asyncTasksRegistry.placeTask("ordersTotalSumTask", task);
    }

    private void schedulePaymentsTotalSumCalculating(Client... clients) {
        SupplierConfigurer<BigDecimal> taskConfigurer =
                uiAsyncTasks.supplierConfigurer(() -> calculatePaymentsTotalSum(clients))
                        .withExceptionHandler(e -> SkeletonStyler.remove(paymentsTotalSumCard))
                        .withResultHandler(paymentsTotalSum ->
                                fillStatCard("Payments Total", paymentsTotalSumCard, paymentsTotalSum));
        asyncTasksRegistry.placeTask("paymentsTotalSumTask", taskConfigurer);
    }

    private void scheduleAverageBillCalculating(Client... clients) {
        SupplierConfigurer<?> task = uiAsyncTasks.supplierConfigurer(() -> calculateAverageBill(clients))
                .withExceptionHandler(e -> SkeletonStyler.remove(averageBillCard))
                .withResultHandler(averageBill ->
                        fillStatCard("Average Bill", averageBillCard, averageBill));
        asyncTasksRegistry.placeTask("averageBillTask", task);
    }

    private BigDecimal calculateOrdersTotalSum(Client[] selectedClients) {
        BigDecimal ordersTotalSum;
        if (selectedClients.length == 0 && !isFilterConditionEmpty()) {
            selectedClients = loadFilteredClients();
            if (selectedClients.length == 0) {
                return BigDecimal.ZERO;
            }
        }

        if (selectedClients.length > 0) {
            ordersTotalSum = clientService.getOrdersTotalSum(selectedClients);
        } else {
            ordersTotalSum = orderService.getOrdersTotalSum();
        }

        return ordersTotalSum;
    }

    private BigDecimal calculatePaymentsTotalSum(Client[] selectedClients) {
        BigDecimal paymentsTotalSum;
        if (selectedClients.length == 0 && !isFilterConditionEmpty()) {
            selectedClients = loadFilteredClients();
            if (selectedClients.length == 0) {
                return BigDecimal.ZERO;
            }
        }

        if (selectedClients.length > 0) {
            paymentsTotalSum = clientService.getPaymentsTotalSum(selectedClients);
        } else {
            paymentsTotalSum = paymentService.getPaymentsTotalSum();
        }

        return paymentsTotalSum;
    }

    private BigDecimal calculateAverageBill(Client[] selectedClients) {
        BigDecimal averageBill;
        if (selectedClients.length == 0 && !isFilterConditionEmpty()) {
            selectedClients = loadFilteredClients();
            if (selectedClients.length == 0) {
                return BigDecimal.ZERO;
            }
        }

        if (selectedClients.length > 0) {
            averageBill = clientService.getAverageBill(selectedClients);
        } else {
            averageBill = orderService.getOrdersAverageBill();
        }
        return averageBill;
    }

    private void fillStatCard(String title, CrmCard card, BigDecimal content) {
        VerticalLayout component = new VerticalLayout(new H1(PriceDataType.formatValue(content)));
        component.setPadding(false);
        component.add(createStatCardFooter());
        card.fillAsStaticCard(title, component);
        SkeletonStyler.remove(card);
    }

    private Client[] loadFilteredClients() {
        return repository.fluentLoader()
                .condition(filtersCondition)
                .list().toArray(new Client[0]);
    }

    private boolean isFilterConditionEmpty() {
        return filtersCondition.getConditions().isEmpty();
    }

    private Component createStatCardFooter() {
        Span mainText;
        String badge;
        Client[] selectedClients = getSelectedClients();

        if (selectedClients.length == 1) {
            mainText = new Span("for " + selectedClients[0].getName());
            badge = "warning";
        } else if (selectedClients.length == 0 && isFilterConditionEmpty()) {
            mainText = new Span("for all clients");
            badge = "default";
        } else if (selectedClients.length > 0) {
            mainText = new Span("for %d selected clients".formatted(selectedClients.length));
            badge = "warning";
        } else {
            mainText = new Span("for filtered clients");
            badge = "success";
        }

        mainText.getElement().getThemeList().addAll(List.of("badge", badge));
        mainText.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM);

        Span hintText = new Span("Select clients in the table to show their statistics");
        hintText.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.THIN);

        VerticalLayout layout = new VerticalLayout(mainText, hintText);
        layout.setWidthFull();
        layout.setPadding(false);
        layout.setHorizontalComponentAlignment(FlexComponent.Alignment.CENTER, hintText);

        return layout;
    }

    private void initializeFilterFields() {
        List<User> accountManagers = new ArrayList<>(userService.loadAccountManagers());
        accountManagers.addFirst(getCurrentUser());
        accountManagerSelect.setItems(accountManagers);

        List.<HasValue<?, ?>>of(searchField, typeSelect, accountManagerSelect, categorySelect)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(searchField)
                .addBooleanBinding(showOnlyMyClientsCheckBox)
                .addEnumBinding(ClientType.class, typeSelect)
                .addEnumBinding(ClientCategory.class, categorySelect)
                .addEntitySelectBinding(accountManagerSelect, accountManagers)
                .build();
    }

    private void applyFilters() {
        updateFiltersCondition();
        calculateCardsValues();
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

    private User getCurrentUser() {
        return ((User) currentAuthentication.getUser());
    }
}