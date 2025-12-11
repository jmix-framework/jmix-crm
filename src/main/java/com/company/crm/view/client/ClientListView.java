package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.service.user.UserService;
import com.company.crm.app.util.ui.component.card.CrmCard;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.user.User;
import com.company.crm.view.main.MainView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.grid.DataGrid;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
    private ClientRepository repository;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    // cards
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

    private final List<CompletableFuture<?>> calculatingTasks = new ArrayList<>();

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
        initializeStatsBlock();
        initializeFilterFields();
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
        calculateCardsValues();
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
        calculateCardsValues();
    }

    private void calculateCardsValues() {
        cancelPreviousCalculating();
        calculatingTasks.addAll(
                List.of(
                        updateOrdersTotalSumCard(),
                        updatePaymentsTotalSumCard(),
                        updateAverageBillCard()
                )
        );
    }

    private void cancelPreviousCalculating() {
        calculatingTasks.forEach(task -> task.cancel(true));
        calculatingTasks.clear();
    }

    private CompletableFuture<?> updateOrdersTotalSumCard() {
        installCardLoader(ordersTotalSumCard);
        Client[] selectedClients = getSelectedClients();
        return uiAsyncTasks.supplierConfigurer(() -> {
                    BigDecimal ordersTotalSum;
                    if (selectedClients.length > 0) {
                        ordersTotalSum = clientService.getOrdersTotalSum(selectedClients);
                    } else {
                        ordersTotalSum = orderService.getOrdersTotalSum();
                    }
                    return ordersTotalSum;
                })
                .withExceptionHandler(e ->
                        SkeletonStyler.remove(ordersTotalSumCard))
                .withResultHandler(ordersTotalSum ->
                        fillStatCard("Orders Total", ordersTotalSumCard, ordersTotalSum, selectedClients))
                .supplyAsync();
    }

    private CompletableFuture<?> updatePaymentsTotalSumCard() {
        installCardLoader(paymentsTotalSumCard);
        Client[] selectedClients = getSelectedClients();
        return uiAsyncTasks.supplierConfigurer(() -> {
                    BigDecimal paymentsTotalSum;
                    if (selectedClients.length > 0) {
                        paymentsTotalSum = clientService.getPaymentsTotalSum(selectedClients);
                    } else {
                        paymentsTotalSum = paymentService.getPaymentsTotalSum();
                    }
                    return paymentsTotalSum;
                })
                .withExceptionHandler(e ->
                        SkeletonStyler.remove(paymentsTotalSumCard))
                .withResultHandler(paymentsTotalSum ->
                        fillStatCard("Payments Total", paymentsTotalSumCard, paymentsTotalSum, selectedClients))
                .supplyAsync();
    }

    private CompletableFuture<?> updateAverageBillCard() {
        installCardLoader(averageBillCard);
        Client[] selectedClients = getSelectedClients();
        return uiAsyncTasks.supplierConfigurer(() -> {
                    BigDecimal averageBill;
                    try {
                        Thread.sleep(3_000);
                    } catch (InterruptedException e) {

                    }
                    if (selectedClients.length > 0) {
                        averageBill = clientService.getAverageBill(selectedClients);
                    } else {
                        averageBill = orderService.getOrdersAverageBill();
                    }
                    return averageBill;
                })
                .withExceptionHandler(e ->
                        SkeletonStyler.remove(averageBillCard))
                .withResultHandler(averageBill ->
                        fillStatCard("Average Bill", averageBillCard, averageBill, selectedClients))
                .supplyAsync();
    }

    private void installCardLoader(Card card) {
        card.removeAll();
        card.add("Loading...");
        SkeletonStyler.apply(card);
    }

    private Client[] getSelectedClients() {
        return clientsDataGrid.getSelectedItems().toArray(new Client[0]);
    }

    private void fillStatCard(String title, CrmCard card, BigDecimal content, Client[] selectedClients) {
        Div component = new Div(new H1(PriceDataType.formatValue(content)));
        component.add(createClientsSuffix(selectedClients));
        card.fillAsStaticCard(title, component);
        SkeletonStyler.remove(card);
    }

    private Component createClientsSuffix(Client[] selectedClients) {
        if (selectedClients.length > 0) {
            if (selectedClients.length == 1) {
                return new Span("for " + selectedClients[0].getName());
            } else {
                return new Span("for %d selected clients".formatted(selectedClients.length));
            }
        }
        return new Span("for all clients");
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