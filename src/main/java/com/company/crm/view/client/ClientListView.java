package com.company.crm.view.client;

import com.company.crm.app.feature.queryparameters.filters.FieldValueQueryParameterBinder;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.service.user.UserService;
import com.company.crm.app.ui.component.CrmLoader;
import com.company.crm.app.ui.component.card.CrmCard;
import com.company.crm.app.util.AsyncTasksRegistry;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.CrmUiUtils;
import com.company.crm.app.util.ui.listener.resize.WidthResizeListener;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientRepository;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.user.User;
import com.company.crm.view.main.MainView;
import com.company.crm.view.util.SkeletonStyler;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Unit;
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
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import io.jmix.core.Messages;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.repository.JmixDataRepositoryContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.asynctask.UiAsyncTasks.SupplierConfigurer;
import io.jmix.flowui.component.checkbox.Switch;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.company.crm.app.feature.sortable.SortableFeature.makeSortable;
import static com.company.crm.app.util.demo.DemoUtils.defaultSleepForStatisticsLoading;
import static com.company.crm.app.util.ui.CrmUiUtils.addRowSelectionInMultiSelectMode;
import static com.company.crm.app.util.ui.CrmUiUtils.openLink;
import static com.company.crm.app.util.ui.CrmUiUtils.setSearchHintPopover;
import static com.company.crm.app.util.ui.datacontext.DataContextUtils.addCondition;
import static com.company.crm.app.util.ui.listener.resize.WidthResizeListener.isWidthChanged;
import static io.jmix.core.querycondition.PropertyCondition.contains;
import static io.jmix.core.querycondition.PropertyCondition.equal;
import static io.jmix.core.querycondition.PropertyCondition.isCollectionEmpty;

@Route(value = "clients", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.CLIENT_LIST)
@ViewDescriptor(path = "client-list-view.xml")
@LookupComponent("clientsDataGrid")
@DialogMode(width = "90%", resizable = true)
public class ClientListView extends StandardListView<Client> implements WidthResizeListener {

    @Autowired
    private Messages messages;
    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private ClientService clientService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private UiAsyncTasks uiAsyncTasks;
    @Autowired
    private ClientRepository clientRepository;
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
    @ViewComponent
    private MessageBundle messageBundle;

    private static volatile int lastWidth = -1;
    private static final int widthBreakpoint = 600;

    private final AsyncTasksRegistry asyncTasksRegistry = AsyncTasksRegistry.newInstance();

    private final LogicalCondition filtersCondition = LogicalCondition.and();

    @Override
    public void configureUiForWidth(int width) {
        if (isWidthChanged(width, lastWidth, widthBreakpoint)) {
            lastWidth = width;
            configureFiltersPanel(width);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initialize();
    }

    @Install(to = "clientsDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private List<Client> loadDelegate(Pageable pageable, JmixDataRepositoryContext context) {
        return clientRepository.findAll(pageable, addCondition(context, filtersCondition)).getContent();
    }

    @Install(to = "clientsDataGrid.removeAction", subject = "delegate")
    private void clientsDataGridRemoveDelegate(final Collection<Client> collection) {
        clientRepository.deleteAll(collection);
    }

    @Install(to = "pagination", subject = "totalCountByRepositoryDelegate")
    private Long paginationTotalCountByRepositoryDelegate(final JmixDataRepositoryContext context) {
        return clientRepository.count(addCondition(context, filtersCondition));
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
        return crmRenderers.clientNameLink();
    }

    @Supply(to = "clientsDataGrid.type", subject = "renderer")
    private Renderer<Client> clientsDataGridTypeRenderer() {
        return crmRenderers.clientType();
    }

    @Supply(to = "clientsDataGrid.vatNumber", subject = "renderer")
    private Renderer<Client> clientsDataGridVatNumberRenderer() {
        return crmRenderers.clientVatNumber();
    }

    @Supply(to = "clientsDataGrid.regNumber", subject = "renderer")
    private Renderer<Client> clientsDataGridRegNumberRenderer() {
        return crmRenderers.clientRegNumber();
    }

    @Supply(to = "clientsDataGrid.website", subject = "renderer")
    private Renderer<Client> clientsDataGridWebsiteRenderer() {
        return new ComponentRenderer<>(c -> {
            String website = Objects.toString(c.getWebsite(), "");
            if (website.length() > 30) {
                website = StringUtils.substring(website, 0, 27) + "...";
            }

            Span span = new Span(website);
            CrmUiUtils.setCursorPointer(span);
            span.setTitle(c.getWebsite());
            span.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.TextColor.SECONDARY, TextOverflow.ELLIPSIS);
            span.addClickListener(e -> openLink(c.getWebsite()));
            return span;
        });
    }

    private void initialize() {
        initializeStatsBlock();
        initializeFilterFields();
        addDetachListener(e -> asyncTasksRegistry.cancelAll());
        addRowSelectionInMultiSelectMode(clientsDataGrid, "vatNumber", "regNumber");
    }

    private void initializeStatsBlock() {
        makeSortable(statsBlock);
        configureCardsSize();
        calculateCardsValues();
    }

    private void configureCardsSize() {
        statsBlock.getChildren().forEach(card -> {
            if (card instanceof HasSize hasSize) {
                hasSize.setMaxHeight(12, Unit.EM);
            }
        });
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
        CrmLoader loader = new CrmLoader();
        loader.startLoading();
        card.add(loader);
        SkeletonStyler.apply(card);
    }

    private Client[] getSelectedClients() {
        return clientsDataGrid.getSelectedItems().toArray(new Client[0]);
    }

    private void scheduleOrdersTotalSumCalculating(Client... clients) {
        SupplierConfigurer<?> task = uiAsyncTasks.supplierConfigurer(() -> calculateOrdersTotalSum(clients))
                .withExceptionHandler(e -> SkeletonStyler.remove(ordersTotalSumCard))
                .withResultHandler(ordersTotalSum ->
                        fillStatCard(messages.getMessage("ordersTotal"), ordersTotalSumCard, ordersTotalSum));
        asyncTasksRegistry.placeTask("ordersTotalSumTask", task);
    }

    private void schedulePaymentsTotalSumCalculating(Client... clients) {
        SupplierConfigurer<BigDecimal> taskConfigurer = uiAsyncTasks.supplierConfigurer(() -> calculatePaymentsTotalSum(clients))
                .withExceptionHandler(e -> SkeletonStyler.remove(paymentsTotalSumCard))
                .withResultHandler(paymentsTotalSum ->
                        fillStatCard(messages.getMessage("paymentsTotal"), paymentsTotalSumCard, paymentsTotalSum));
        asyncTasksRegistry.placeTask("paymentsTotalSumTask", taskConfigurer);
    }

    private void scheduleAverageBillCalculating(Client... clients) {
        SupplierConfigurer<?> task = uiAsyncTasks.supplierConfigurer(() -> calculateAverageBill(clients))
                .withExceptionHandler(e -> SkeletonStyler.remove(averageBillCard))
                .withResultHandler(averageBill -> fillStatCard(messages.getMessage("averageBill"), averageBillCard, averageBill));
        asyncTasksRegistry.placeTask("averageBillTask", task);
    }

    private BigDecimal calculateOrdersTotalSum(Client[] selectedClients) {
        defaultSleepForStatisticsLoading();
        BigDecimal ordersTotalSum;
        if (selectedClients.length == 0 && !isFilterConditionEmpty()) {
            selectedClients = loadFilteredClients();
            if (selectedClients.length == 0) {
                return BigDecimal.ZERO;
            }
        }

        if (selectedClients.length > 0) {
            ordersTotalSum = clientService.getOrdersTotalSum(OrderStatus.values(), selectedClients);
        } else {
            ordersTotalSum = orderService.getOrdersTotalSum();
        }

        return ordersTotalSum;
    }

    private BigDecimal calculatePaymentsTotalSum(Client[] selectedClients) {
        defaultSleepForStatisticsLoading();
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
        defaultSleepForStatisticsLoading();
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
        var contentComponent = new H1(PriceDataType.defaultFormat(content));
        contentComponent.setWidthFull();
        contentComponent.setMaxWidth(12, Unit.EM);
        contentComponent.addClassNames(Overflow.HIDDEN, TextOverflow.ELLIPSIS, Whitespace.NOWRAP);

        VerticalLayout component = new VerticalLayout(contentComponent);
        component.setWidthFull();
        component.setPadding(false);
        component.addClassNames(Overflow.HIDDEN);
        component.add(createStatCardFooter());

        card.fillAsStaticCard(title, component);
        SkeletonStyler.remove(card);
    }

    private Client[] loadFilteredClients() {
        return clientRepository.fluentLoader()
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
            mainText = new Span(messageBundle.getMessage("for") + " " + selectedClients[0].getName());
            badge = CrmUiUtils.WARNING_BADGE;
        } else if (selectedClients.length == 0 && isFilterConditionEmpty()) {
            mainText = new Span(messageBundle.getMessage("forAllClients"));
            badge = CrmUiUtils.DEFAULT_BADGE;
        } else if (selectedClients.length > 0) {
            mainText = new Span(messageBundle.formatMessage("mainText", selectedClients.length));
            badge = CrmUiUtils.WARNING_BADGE;
        } else {
            mainText = new Span(messageBundle.getMessage("forFilteredClients"));
            badge = CrmUiUtils.SUCCESS_BADGE;
        }

        CrmUiUtils.setBadge(mainText, badge);
        mainText.addClassNames(FontSize.LARGE, FontWeight.MEDIUM);
        mainText.addClassNames(Overflow.HIDDEN, TextOverflow.ELLIPSIS, Whitespace.NOWRAP);
        mainText.setWidthFull();

        Span hintText = new Span(messageBundle.getMessage("cardHintText"));
        hintText.addClassNames(FontSize.XSMALL, FontWeight.THIN);

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

        setSearchHintPopover(searchField);

        List.<HasValue<?, ?>>of(searchField, typeSelect, accountManagerSelect, categorySelect)
                .forEach(field -> field.addValueChangeListener(e -> applyFilters()));

        //noinspection unchecked
        FieldValueQueryParameterBinder.builder(this)
                .addStringBinding(searchField)
                .addBooleanBinding(showOnlyMyClientsCheckBox)
                .addEnumBinding(ClientType.class, typeSelect)
                .addEnumBinding(ClientCategory.class, categorySelect)
                .addEntitySelectBinding(accountManagerSelect, () -> accountManagers)
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
