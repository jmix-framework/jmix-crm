package com.company.crm.app.util.ui.renderer;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.util.common.ThreadUtils;
import com.company.crm.app.util.ui.CrmUiUtils;
import com.company.crm.app.util.ui.theme.CrmStyleUtility;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.datatype.PriceDataType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import com.company.crm.model.user.User;
import com.company.crm.model.user.task.UserTask;
import com.company.crm.view.client.ClientDetailView;
import com.company.crm.view.order.OrderDetailView;
import com.company.crm.view.user.UserDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.spring.annotation.SpringComponent;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.renderer.DetailButtonRenderer;
import io.jmix.flowui.view.OpenMode;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.StandardListView;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.flowui.view.builder.DetailWindowBuilder;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.function.Function;

import static com.company.crm.app.util.ui.CrmUiUtils.CONTRAST_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.SUCCESS_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.setDefaultEmptyStateComponent;
import static com.company.crm.app.util.ui.color.EnumClassColors.getBadgeVariant;
import static com.company.crm.model.datatype.PriceDataType.defaultFormat;
import static com.vaadin.flow.component.icon.VaadinIcon.CHEVRON_DOWN_SMALL;
import static com.vaadin.flow.component.icon.VaadinIcon.CHEVRON_RIGHT_SMALL;
import static io.jmix.flowui.component.UiComponentUtils.copyToClipboard;
import static io.jmix.flowui.component.UiComponentUtils.getCurrentView;

@SpringComponent
public class CrmRenderers {

    private final Messages messages;
    private final UiAsyncTasks uiAsyncTasks;
    private final UiComponents uiComponents;
    private final ViewNavigators viewNavigators;
    private final MetadataTools metadataTools;
    private final DialogWindows dialogWindows;
    private final DateTimeService dateTimeService;
    private final DatatypeFormatter datatypeFormatter;

    public CrmRenderers(UiComponents uiComponents, DialogWindows dialogWindows, Messages messages,
                        DatatypeFormatter datatypeFormatter, DateTimeService dateTimeService,
                        UiAsyncTasks uiAsyncTasks, MetadataTools metadataTools, ViewNavigators viewNavigators) {
        this.messages = messages;
        this.uiComponents = uiComponents;
        this.dialogWindows = dialogWindows;
        this.datatypeFormatter = datatypeFormatter;
        this.dateTimeService = dateTimeService;
        this.uiAsyncTasks = uiAsyncTasks;
        this.metadataTools = metadataTools;
        this.viewNavigators = viewNavigators;
    }

    public <T> Renderer<T> itemDetailsColumnRenderer(DataGrid<T> grid) {
        return new ComponentRenderer<>(item -> {
            boolean isDetailsVisible = grid.isDetailsVisible(item);
            Icon icon = isDetailsVisible ? CHEVRON_DOWN_SMALL.create() : CHEVRON_RIGHT_SMALL.create();
            CrmUiUtils.setClickableCursor(icon);
            icon.addClassNames(CrmStyleUtility.IconSize.SMALL);
            icon.addClickListener(e -> {
                if (!grid.isDetailsVisibleOnClick()) {
                    grid.setDetailsVisible(item, !isDetailsVisible);
                }
            });
            return icon;
        });
    }

    public ComponentRenderer<Component, Client> clientDetails() {
        return new ComponentRenderer<>(client -> {
            var container = new VerticalLayout();
            container.add(new H3(messages.getMessage(getClass(), "orders")));

            @SuppressWarnings("unchecked")
            DataGrid<Order> ordersGrid = uiComponents.create(DataGrid.class);
            ordersGrid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
            ordersGrid.setMaxHeight(15, Unit.EM);
            ordersGrid.addColumn(Order::getNumber)
                    .setRenderer(uniqueNumber(Order::getNumber))
                    .setHeader(messages.getMessage(Order.class, "Order.number"));
            ordersGrid.addColumn(Order::getDate)
                    .setHeader(messages.getMessage(Order.class, "Order.date"));
            ordersGrid.addColumn(order -> defaultFormat(order.getTotal(), datatypeFormatter))
                    .setHeader(messages.getMessage(Order.class, "Order.total"));
            ordersGrid.setItems(client.getOrders());
            setDefaultEmptyStateComponent(ordersGrid);
            container.add(ordersGrid);

            return container;
        });
    }

    public ComponentRenderer<Component, Invoice> invoiceDetails() {
        return new ComponentRenderer<>(invoice -> {
            var container = new VerticalLayout();
            container.add(new H3(messages.getMessage(getClass(), "payments")));

            @SuppressWarnings("unchecked")
            DataGrid<Payment> paymentsGrid = uiComponents.create(DataGrid.class);
            paymentsGrid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
            paymentsGrid.setMaxHeight(15, Unit.EM);
            paymentsGrid.addColumn(Payment::getNumber)
                    .setRenderer(uniqueNumber(Payment::getNumber))
                    .setHeader(messages.getMessage(Payment.class, "Payment.number"));
            paymentsGrid.addColumn(Payment::getDate)
                    .setHeader(messages.getMessage(Payment.class, "Payment.date"));
            paymentsGrid.addColumn(payment -> defaultFormat(payment.getAmount(), datatypeFormatter))
                    .setHeader(messages.getMessage(Payment.class, "Payment.amount"));
            paymentsGrid.setItems(invoice.getPayments());
            setDefaultEmptyStateComponent(paymentsGrid);
            container.add(paymentsGrid);

            return container;
        });
    }

    public ComponentRenderer<Component, Order> orderDetails() {
        return new ComponentRenderer<>(order -> {
            var container = new VerticalLayout();
            container.add(new H3(messages.getMessage(getClass(), "invoices")));

            @SuppressWarnings("unchecked")
            DataGrid<Invoice> invoicesGrid = uiComponents.create(DataGrid.class);
            invoicesGrid.addThemeVariants(GridVariant.NO_BORDER, GridVariant.ROW_STRIPES);
            invoicesGrid.setMaxHeight(15, Unit.EM);
            invoicesGrid.addColumn(Invoice::getNumber)
                    .setRenderer(uniqueNumber(Invoice::getNumber))
                    .setHeader(messages.getMessage(Invoice.class, "Invoice.number"));
            invoicesGrid.addColumn(Invoice::getDate)
                    .setHeader(messages.getMessage(Invoice.class, "Invoice.date"));
            invoicesGrid.addColumn(invoice -> defaultFormat(invoice.getTotal(), datatypeFormatter))
                    .setHeader(messages.getMessage(Invoice.class, "Invoice.total"));
            invoicesGrid.setItems(order.getInvoices());
            setDefaultEmptyStateComponent(invoicesGrid);
            container.add(invoicesGrid);

            return container;
        });
    }

    public <E> Renderer<E> detailLink(DataGrid<E> grid) {
        return detailLink(grid, metadataTools::getInstanceName, null);
    }

    public <E> Renderer<E> detailLink(DataGrid<E> grid, ValueProvider<E, String> textProvider,
                                      @Nullable Class<? extends View<?>> detailViewClass) {
        return new DetailButtonRenderer<>(uiComponents, viewNavigators, dialogWindows, grid, textProvider)
                .withOpenMode(OpenMode.DIALOG)
                .withViewClass(detailViewClass)
                .withThemeNames(ButtonVariant.TERTIARY.getVariantName());
    }

    public <E, L extends UuidEntity> Renderer<E> entityLink(DataGrid<E> grid, Function<E, L> linkGetter) {
        return entityLink(grid, linkGetter, metadataTools::getInstanceName);
    }

    public <E, L extends UuidEntity> Renderer<E> entityLink(DataGrid<E> grid, Function<E, L> linkGetter,
                                                            Function<L, String> textProvider) {
        return relatedDetailLink(grid, linkGetter, textProvider, null, false);
    }

    private <E, L extends UuidEntity> Renderer<E> relatedDetailLink(DataGrid<E> grid, Function<E, L> linkGetter,
                                                                    Function<L, String> textProvider,
                                                                    @Nullable Class<? extends View<?>> detailViewClass,
                                                                    boolean readOnly) {
        return new DetailButtonRenderer<>(uiComponents, viewNavigators, dialogWindows, grid,
                item -> {
                    L link = linkGetter.apply(item);
                    return link != null ? textProvider.apply(link) : "";
                })
                .withThemeNames(ButtonVariant.TERTIARY.getVariantName())
                .withClickHandler(item -> {
                    L link = linkGetter.apply(item);
                    if (link != null) {
                        openDetailDialog(link, detailViewClass, readOnly);
                    }
                });
    }

    public Renderer<Invoice> invoiceClientLink(DataGrid<Invoice> grid) {
        return relatedDetailLink(grid, Invoice::getClient, Client::getName, ClientDetailView.class, false);
    }

    public Renderer<Order> orderClientLink(DataGrid<Order> grid) {
        return relatedDetailLink(grid, Order::getClient, Client::getName, ClientDetailView.class, false);
    }

    public Renderer<Invoice> invoiceOrderLink(DataGrid<Invoice> grid) {
        return relatedDetailLink(grid, Invoice::getOrder, metadataTools::getInstanceName, OrderDetailView.class, false);
    }

    public Renderer<Client> accountManagerLink(DataGrid<Client> grid) {
        return relatedDetailLink(grid, Client::getAccountManager, User::getDisplayName, UserDetailView.class, true);
    }

    public Renderer<Client> clientNameLink(DataGrid<Client> grid) {
        return detailLink(grid, Client::getName, ClientDetailView.class);
    }

    public <E extends UuidEntity> Renderer<E> uniqueNumber(Function<E, String> numberProvider) {
        return new ComponentRenderer<>(entity -> {
            var button = new Button(numberProvider.apply(entity));
            button.setTooltipText(messages.getMessage("copy"));
            button.addThemeVariants(ButtonVariant.SMALL, ButtonVariant.TERTIARY);
            button.addClickListener(e -> copyToClipboardWithNotification(numberProvider.apply(entity), button));
            return button;
        });
    }

    public Renderer<Client> clientType() {
        return new ComponentRenderer<>(client -> {
            ClientType type = client.getType();
            return createBadge(messages.getMessage(type), getBadgeVariant(type));
        });
    }

    public Renderer<Category> categoryCode() {
        return badgeWithCopyRenderer(Category::getCode);
    }

    public Renderer<CategoryItem> categoryItemCode() {
        return badgeWithCopyRenderer(CategoryItem::getCode);
    }

    public Renderer<CategoryItem> categoryItemCategoryCode() {
        return badgeWithCopyRenderer(item -> item.getCategory().getCode());
    }

    public Renderer<OrderItem> orderItemItemCode() {
        return badgeWithCopyRenderer(item -> item.getCategoryItem().getCode());
    }

    public Renderer<Client> clientVatNumber() {
        return badgeWithCopyRenderer(Client::getVatNumber);
    }

    public Renderer<Client> clientRegNumber() {
        return badgeWithCopyRenderer(Client::getRegNumber);
    }

    public <T> Renderer<T> badgeRenderer(Function<T, String> textProvider, String badgeVariant) {
        return new ComponentRenderer<>(obj -> createBadge(textProvider.apply(obj), badgeVariant));
    }

    public <T> Renderer<T> badgeWithCopyRenderer(Function<T, String> textProvider) {
        return new ComponentRenderer<>(obj -> createBadgeWithCopy(textProvider.apply(obj)));
    }

    public Renderer<Order> orderStatus() {
        return new ComponentRenderer<>(order -> createOrderStatusBadge(order.getStatus()));
    }

    public Renderer<Invoice> invoiceStatus() {
        return new ComponentRenderer<>(invoice -> createInvoiceStatusBadge(invoice.getStatus()));
    }

    public ComponentRenderer<Span, OrderStatus> orderStatusEnum() {
        return new ComponentRenderer<>(this::createOrderStatusBadge);
    }

    public Span createOrderStatusBadge(OrderStatus status) {
        return createBadge(messages.getMessage(status), getBadgeVariant(status));
    }

    public Span createInvoiceStatusBadge(InvoiceStatus status) {
        return createBadge(messages.getMessage(status), getBadgeVariant(status));
    }

    public Renderer<Invoice> invoiceDueDateRenderer() {
        return new ComponentRenderer<>(invoice -> {
            LocalDate dueDate = invoice.getDueDate();
            String dueDateText = datatypeFormatter.formatLocalDate(dueDate);
            Span span = new Span(dueDateText);

            LocalDate currentDate = dateTimeService.getTimeForCurrentUser().toLocalDate();
            Period daysLeft = currentDate.until(dueDate);

            var badgeVariant = CONTRAST_BADGE;
            if (daysLeft.isNegative()) {
                badgeVariant = CrmUiUtils.ERROR_BADGE;
            }

            CrmUiUtils.setBadge(span, badgeVariant);
            return span;
        });
    }

    public Renderer<UserTask> taskDueDateRenderer() {
        return new ComponentRenderer<>(task -> {
            LocalDate dueDate = task.getDueDate();
            String dueDateText = datatypeFormatter.formatLocalDate(dueDate);
            Span span = new Span(dueDateText);

            LocalDate currentDate = dateTimeService.getTimeForCurrentUser().toLocalDate();
            Period daysLeft = currentDate.until(dueDate);

            Boolean isCompleted = task.getIsCompleted();
            var badgeVariant = isCompleted ? SUCCESS_BADGE : CONTRAST_BADGE;
            if (!isCompleted) {
                if (daysLeft.isNegative()) {
                    badgeVariant = CrmUiUtils.ERROR_BADGE;
                }
            }

            CrmUiUtils.setBadge(span, badgeVariant);
            return span;
        });
    }

    public Renderer<Order> orderLeftOverSumRenderer() {
        return new ComponentRenderer<>(order -> {
            BigDecimal leftOverSum = order.getLeftOverSum();
            Span span = new Span(PriceDataType.formatWithoutCurrency(leftOverSum, datatypeFormatter));

            if (leftOverSum.compareTo(BigDecimal.valueOf(10_000)) > 0) {
                CrmUiUtils.setBadge(span, CrmUiUtils.ERROR_BADGE);
            } else if (leftOverSum.compareTo(BigDecimal.ZERO) > 0) {
                CrmUiUtils.setBadge(span, CrmUiUtils.WARNING_BADGE);
            } else {
                CrmUiUtils.setBadge(span, SUCCESS_BADGE);
                span.setText(messages.getMessage("paid"));
            }

            return span;
        });
    }

    private Span createBadge(String text, String badgeVariant) {
        Span span = new Span(text);
        CrmUiUtils.setBadge(span, badgeVariant);
        return span;
    }

    private Span createBadgeWithCopy(String text) {
        Span badge = createBadge(text, "contrast");
        Tooltip.forComponent(badge).setText(messages.getMessage("copy"));
        CrmUiUtils.setClickableCursor(badge);
        badge.addClickListener(e -> copyToClipboardWithNotification(text, badge));
        return badge;
    }

    private void copyToClipboardWithNotification(String text, Component component) {
        copyToClipboard(text);
        showCopiedNotification(component);
    }

    private void showCopiedNotification(Component component) {
        Popover popover = new Popover(new Text(messages.getMessage("copied")));
        popover.setTarget(component);
        popover.open();

        uiAsyncTasks.runnableConfigurer(() -> ThreadUtils.trySleep(1_000))
                .withResultHandler(popover::close)
                .withOwner(component)
                .runAsync();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <E extends UuidEntity> void openDetailDialog(E entity,
                                                         @Nullable Class<? extends View<?>> detailViewClass,
                                                         boolean readOnly) {
        DetailWindowBuilder<E, View<?>> builder = dialogWindows.detail(getCurrentView(), (Class<E>) entity.getClass());
        if (detailViewClass != null) {
            builder = builder.withViewClass((Class) detailViewClass);
        }
        builder.editEntity(entity)
                .withViewConfigurer(view -> {
                    if (view instanceof StandardDetailView<?> detailView) {
                        detailView.setReadOnly(readOnly);
                    }
                })
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        reloadCurrentListView();
                    }
                })
                .open();
    }

    private void reloadCurrentListView() {
        try {
            // try to refresh data on the list view
            View<?> currentView = getCurrentView();
            if (currentView instanceof StandardListView<?> listView) {
                ViewControllerUtils.getViewData(listView).loadAll();
            }
        } catch (Throwable ignored) {
        }
    }
}
