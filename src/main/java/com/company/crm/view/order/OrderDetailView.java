package com.company.crm.view.order;

import com.company.crm.app.service.datetime.DateTimeService;
import com.company.crm.app.ui.component.OrderStatusPipeline;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.app.util.ui.renderer.CrmRenderers;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderRepository;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.FetchPlan;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Dialogs.InputDialogBuilder.LabelsPosition;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.action.inputdialog.InputDialogAction;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.SupportsTypedValue.TypedValueChangeEvent;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.richtexteditor.RichTextEditor;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.kit.action.ActionVariant;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.PrimaryDetailView;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.gridexportflowui.exporter.ExportMode;
import io.jmix.gridexportflowui.exporter.excel.ExcelExporter;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.company.crm.app.util.price.PriceCalculator.calculateTotal;
import static com.company.crm.model.datatype.PriceDataType.formatWithoutCurrency;

@Route(value = "orders/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.ORDER_DETAIL)
@ViewDescriptor(path = "order-detail-view.xml")
@EditedEntityContainer("orderDc")
@PrimaryDetailView(Order.class)
public class OrderDetailView extends StandardDetailView<Order> {

    @Autowired
    private Dialogs dialogs;
    @Autowired
    private Messages messages;
    @Autowired
    private Downloader downloader;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private CrmRenderers crmRenderers;
    @Autowired
    private ExcelExporter excelExporter;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private DateTimeService dateTimeService;

    @ViewComponent
    private OrderStatusPipeline statusPipeline;
    @ViewComponent
    private TypedTextField<BigDecimal> discountValueField;
    @ViewComponent
    private TypedTextField<BigDecimal> discountPercentField;
    @ViewComponent
    private H3 orderItemsCount;
    @ViewComponent
    private DataGrid<OrderItem> orderItemsGrid;
    @Autowired
    private Notifications notifications;
    @ViewComponent
    private MessageBundle messageBundle;

    @Subscribe
    private void onInitEntity(final InitEntityEvent<Order> event) {
        Order order = event.getEntity();
        order.setStatus(OrderStatus.NEW);
        order.setDate(dateTimeService.now().toLocalDate());
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        initFieldsValidation();
        selectStatusInPipeline();
        addStatusPipelineClickListener();
        updateFooter();
    }

    @Subscribe(id = "orderDc", target = Target.DATA_CONTAINER)
    private void onOrderDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Order> event) {
        selectStatusInPipeline();
        updateFooter();
    }

    @Install(to = "orderDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<Order> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return orderRepository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(orderRepository.save(getEditedEntity()));
    }

    @Subscribe("discountPercentField")
    private void onDiscountPercentFieldTypedValueChange(final TypedValueChangeEvent<TypedTextField<BigDecimal>, BigDecimal> event) {
        recalculateTotalIfNeeded(event);
    }

    @Subscribe("discountValueField")
    private void onDiscountValueFieldTypedValueChange(final TypedValueChangeEvent<TypedTextField<BigDecimal>, BigDecimal> event) {
        recalculateTotalIfNeeded(event);
    }

    @Supply(to = "orderItemsGrid.[categoryItem.code]", subject = "renderer")
    private Renderer<OrderItem> orderItemsGridCategoryItemCodeRenderer() {
        return crmRenderers.orderItemItemCode();
    }

    @Supply(to = "orderItemsGrid.total", subject = "renderer")
    private Renderer<OrderItem> orderItemsGridTotalRenderer() {
        return crmRenderers.badgeRenderer(item ->
                formatWithoutCurrency(item.getTotal()), "default");
    }

    @Supply(to = "statusSelect", subject = "renderer")
    private ComponentRenderer<Span, OrderStatus> statusSelectRenderer() {
        return crmRenderers.orderStatusEnum();
    }

    @Subscribe("emailAction")
    private void onEmailAction(final ActionPerformedEvent event) {
        dialogs.createInputDialog(this)
                .withHeader(messageBundle.formatMessage("sendEmailDialog.header", getClientName()))
                .withLabelsPosition(LabelsPosition.TOP)
                .withParameter(InputParameter.parameter("text")
                        .withLabel(messages.getMessage("email"))
                        .withField(() -> uiComponents.create(RichTextEditor.class)))
                .withActions(
                        InputDialogAction.action("sendEmail")
                                .withText(messages.getMessage("send"))
                                .withIcon(VaadinIcon.MAILBOX)
                                .withVariant(ActionVariant.SUCCESS)
                                .withHandler(this::onSendEmail),
                        InputDialogAction.action("close")
                                .withText(messages.getMessage("actions.Close"))
                                .withIcon(VaadinIcon.CLOSE)
                                .withHandler(this::closeEmailDialog))
                .build()
                .open();
    }

    private void onSendEmail(ActionPerformedEvent e) {
        notifications.show(messageBundle.formatMessage("emailSentNotification", getClientName()));
        closeEmailDialog(e);
    }

    private String getClientName() {
        return getEditedEntity().getClient().getName();
    }

    @SuppressWarnings("DataFlowIssue")
    private void closeEmailDialog(ActionPerformedEvent e) {
        ((InputDialogAction) e.getSource()).getInputDialog().close(StandardOutcome.CLOSE);
    }

    @Subscribe("downloadAction")
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void onDownloadAction(final ActionPerformedEvent event) {
        // TODO: download custom design-time report
        Grid itemsGrid = orderItemsGrid;
        excelExporter.exportDataGrid(downloader, itemsGrid, ExportMode.CURRENT_PAGE);
    }

    private void selectStatusInPipeline() {
        statusPipeline.selectUntil(getEditedEntity().getStatus());
    }

    private void recalculateTotalIfNeeded(TypedValueChangeEvent<TypedTextField<BigDecimal>, BigDecimal> event) {
        if (event.isFromClient() && !event.getSource().isInvalid()) {
            recalculateTotal(event.getSource());
        }
    }

    private void updateFooter() {
        List<OrderItem> orderItems = getEditedEntity().getOrderItems();
        orderItemsCount.setText((orderItems != null ? orderItems.size() : 0) + " pcs");
    }

    private void recalculateTotal(TypedTextField<BigDecimal> changesOwner) {
        Order order = getEditedEntity();
        BigDecimal itemsTotal = order.getItemsTotal();
        if (changesOwner.equals(discountValueField)) {
            BigDecimal discountValue = order.getDiscountValue();
            if (discountValue != null && itemsTotal.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discountPercent = discountValue
                        .multiply(BigDecimal.valueOf(100))
                        .divide(itemsTotal, 2, RoundingMode.HALF_UP);
                discountPercentField.setTypedValue(discountPercent);
            }
        } else if (changesOwner.equals(discountPercentField)) {
            BigDecimal discountPercent = order.getDiscountPercent();
            if (discountPercent != null) {
                BigDecimal discountValue = itemsTotal.multiply(discountPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                discountValueField.setTypedValue(discountValue);
            }
        } else {
            throw new IllegalStateException("Unknown changes owner for total price calculation");
        }

        order.setTotal(calculateTotal(order));
        updateFooter();
    }

    private void addStatusPipelineClickListener() {
        statusPipeline.addStatusClickListener(comp ->
                getEditedEntity().setStatus(comp.getStatus()));
    }

    private void initFieldsValidation() {
        discountValueField.addValidator(value -> {
            if (value != null && value.compareTo(getEditedEntity().getItemsTotal()) > 0) {
                throw new ValidationException("Value should not be greater than total value");
            }
        });
    }
}