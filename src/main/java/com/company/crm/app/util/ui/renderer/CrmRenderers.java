package com.company.crm.app.util.ui.renderer;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.util.common.ThreadUtils;
import com.company.crm.app.util.ui.CrmUiUtils;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
import com.company.crm.model.client.ClientType;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.invoice.InvoiceStatus;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.user.User;
import com.company.crm.model.user.task.UserTask;
import com.company.crm.view.client.ClientDetailView;
import com.company.crm.view.order.OrderDetailView;
import com.company.crm.view.user.UserDetailView;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.asynctask.UiAsyncTasks;
import io.jmix.flowui.component.UiComponentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.StandardDetailView;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.company.crm.app.util.ui.CrmUiUtils.DEFAULT_BADGE;
import static com.company.crm.app.util.ui.CrmUiUtils.SUCCESS_BADGE;
import static com.company.crm.app.util.ui.color.EnumClassColors.getBadgeVariant;
import static io.jmix.flowui.component.UiComponentUtils.getCurrentView;

@Component
public class CrmRenderers {

    private final Messages messages;
    private final UiComponents uiComponents;
    private final DialogWindows dialogWindows;
    private final DatatypeFormatter datatypeFormatter;
    private final DateTimeService dateTimeService;
    private final UiAsyncTasks uiAsyncTasks;
    private final MetadataTools metadataTools;

    public CrmRenderers(UiComponents uiComponents, DialogWindows dialogWindows, Messages messages, DatatypeFormatter datatypeFormatter, DateTimeService dateTimeService, UiAsyncTasks uiAsyncTasks, MetadataTools metadataTools) {
        this.messages = messages;
        this.uiComponents = uiComponents;
        this.dialogWindows = dialogWindows;
        this.datatypeFormatter = datatypeFormatter;
        this.dateTimeService = dateTimeService;
        this.uiAsyncTasks = uiAsyncTasks;
        this.metadataTools = metadataTools;
    }

    public <SOURCE> ComponentRenderer<Button, SOURCE> longTextRenderer(
            int maxLength, Function<SOURCE, String> textProvider) {
        return longTextRenderer(maxLength, textProvider, null);
    }

    public <SOURCE> ComponentRenderer<Button, SOURCE> longTextRenderer(
            int maxLength, Function<SOURCE, String> textProvider,
            @Nullable Consumer<SOURCE> fullTextClickListener) {
        return new ComponentRenderer<>(source -> {
            String fullMsg = Optional.ofNullable(textProvider.apply(source)).orElse("");
            int fullMsgLength = fullMsg.length();
            boolean isWithinLimit = fullMsgLength <= maxLength;
            String shortMsg = fullMsgLength > maxLength ? fullMsg.substring(0, maxLength) + "..." : fullMsg;

            Button button = uiComponents.create(JmixButton.class);
            button.setText(shortMsg);
            button.getElement().setAttribute("title", fullMsg);
            button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            if (isWithinLimit && fullTextClickListener == null) {
                button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
            }

            button.addClickListener(event -> {
                if (isWithinLimit) {
                    if (fullTextClickListener != null) {
                        fullTextClickListener.accept(source);
                    }
                    return;
                }

                Scroller scroller = new Scroller();
                scroller.setWidthFull();
                scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

                Paragraph text = uiComponents.create(Paragraph.class);
                text.getStyle().set("word-break", "break-all");
                text.setText(fullMsg);
                text.setSizeFull();

                if (fullTextClickListener != null) {
                    CrmUiUtils.setCursorPointer(text);
                    text.addClickListener(__ -> fullTextClickListener.accept(source));
                }

                scroller.setContent(text);

                Popover popover = new Popover();
                popover.add(scroller);
                popover.setTarget(button);
                popover.setCloseOnEsc(true);
                popover.setModal(true);

                int popoverWidth = fullMsgLength > 600 ? 600 : Math.max(fullMsgLength, 300);
                popover.setWidth(popoverWidth + "px");

                popover.open();
                popover.addOpenedChangeListener(e -> {
                    if (!e.isOpened()) {
                        popover.removeFromParent();
                    }
                });
            });

            return button;
        });
    }

    public <E extends UuidEntity, LINK extends UuidEntity> Renderer<E> entityLink(Function<E, LINK> linkGetter) {
        return entityLink(linkGetter, metadataTools::getInstanceName);
    }

    public <E extends UuidEntity, LINK extends UuidEntity> Renderer<E> entityLink(Function<E, LINK> linkGetter,
                                                                                  Function<LINK, String> textProvider) {
        return new ComponentRenderer<>(entity -> {
            LINK link = linkGetter.apply(entity);
            JmixButton button = entityLinkButton(link, textProvider, textProvider);
            button.addClickListener(e -> {
                //noinspection unchecked
                dialogWindows.detail(UiComponentUtils.getCurrentView(), ((Class<LINK>) link.getClass()))
                        .editEntity(link)
                        .withViewConfigurer(v -> {
                            if (v instanceof StandardDetailView<?> detailView) {
                                detailView.setReadOnly(true);
                            }
                        }).open();
            });
            return button;
        });
    }

    public Renderer<Invoice> invoiceClientLink() {
        return new ComponentRenderer<>(invoice -> clientLinkButton(invoice.getClient()));
    }

    public Renderer<Order> orderClientLink() {
        return new ComponentRenderer<>(order -> clientLinkButton(order.getClient()));
    }

    public Renderer<Invoice> invoiceOrderLink() {
        return new ComponentRenderer<>(invoice -> orderLinkButton(invoice.getOrder()));
    }

    public Renderer<Client> accountManagerLink() {
        return new ComponentRenderer<>(this::accountManagerLinkButton);
    }

    public Renderer<Client> clientNameLink() {
        return new ComponentRenderer<>(this::clientLinkButton);
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

    public Renderer<UserTask> taskDueDateRenderer() {
        return new ComponentRenderer<>(task -> {
            LocalDate dueDate = task.getDueDate();
            String dueDateText = datatypeFormatter.formatLocalDate(dueDate);
            Span span = new Span(dueDateText);

            LocalDate currentDate = dateTimeService.getTimeForCurrentUser().toLocalDate();
            Period daysLeft = currentDate.until(dueDate);

            Boolean isCompleted = task.getIsCompleted();
            var badgeVariant = isCompleted ? SUCCESS_BADGE : DEFAULT_BADGE;
            if (!isCompleted) {
                if (daysLeft.isNegative()) {
                    badgeVariant = CrmUiUtils.ERROR_BADGE;
                } else if (daysLeft.getDays() < 3) {
                    badgeVariant = CrmUiUtils.WARNING_BADGE;
                }
            }

            CrmUiUtils.setBadge(span, badgeVariant);
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
        CrmUiUtils.setCursorPointer(badge);
        badge.addClickListener(e -> {
            UiComponentUtils.copyToClipboard(text);
            Popover popover = new Popover(new Text(messages.getMessage("copied")));
            popover.setTarget(badge);
            popover.open();
            uiAsyncTasks.runnableConfigurer(() -> ThreadUtils.trySleep(1_000))
                    .withResultHandler(popover::close)
                    .runAsync();
        });
        return badge;
    }

    private JmixButton clientLinkButton(Client client) {
        JmixButton button =
                entityLinkButton(client, Client::getName, Client::getFullName);
        button.addClickListener(event ->
                openReadOnlyDetailDialog(client, Client.class, ClientDetailView.class));
        return button;
    }

    private JmixButton orderLinkButton(Order order) {
        JmixButton button =
                entityLinkButton(order, Order::getNumber, o -> o.getInstanceName(datatypeFormatter));
        button.addClickListener(event ->
                openReadOnlyDetailDialog(order, Order.class, OrderDetailView.class));
        return button;
    }

    private JmixButton accountManagerLinkButton(Client client) {
        JmixButton button =
                entityLinkButton(client.getAccountManager(), User::getDisplayName, User::getFullName);
        button.addClickListener(event ->
                openReadOnlyDetailDialog(client.getAccountManager(), User.class, UserDetailView.class));
        return button;
    }

    private <E extends UuidEntity> JmixButton entityLinkButton(E entity,
                                                               Function<E, String> textProvider,
                                                               Function<E, String> tooltipProvider) {
        JmixButton button = uiComponents.create(JmixButton.class);
        button.setText(textProvider.apply(entity));
        button.setTooltipText(tooltipProvider.apply(entity));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        return button;
    }

    private <E extends UuidEntity, V extends StandardDetailView<E>> void openReadOnlyDetailDialog(E entity,
                                                                                                  Class<E> entityClass,
                                                                                                  Class<V> detailClass) {
        dialogWindows.detail(getCurrentView(), entityClass)
                .withViewClass(detailClass)
                .editEntity(entity)
                .withViewConfigurer(v -> v.setReadOnly(true))
                .open();
    }
}
