package com.company.crm.view.order.item;

import com.company.crm.app.service.settings.CrmSettingsService;
import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderItemRepository;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.textfield.JmixBigDecimalField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.InstanceContainer.ItemPropertyChangeEvent;
import io.jmix.flowui.view.DialogMode;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.company.crm.app.util.price.PriceCalculator.calculateGrossPrice;
import static com.company.crm.app.util.price.PriceCalculator.calculateNetPrice;
import static com.company.crm.app.util.price.PriceCalculator.calculateVat;
import static com.company.crm.app.util.price.PriceCalculator.calculateVatPercent;
import static com.company.crm.app.util.price.PriceCalculator.recalculatePricing;

@Route(value = "order-items/:id", layout = MainView.class)
@ViewController(id = CrmConstants.ViewIds.ORDER_ITEM_DETAIL)
@ViewDescriptor(path = "order-item-detail-view.xml")
@EditedEntityContainer("orderItemDc")
@DialogMode(resizable = true)
public class OrderItemDetailView extends StandardDetailView<OrderItem> {

    @Autowired
    private EntityStates entityStates;
    @Autowired
    private OrderItemRepository itemRepository;
    @Autowired
    private OrderItemPreservedState preservedState;
    @Autowired
    private CrmSettingsService crmSettingsService;

    @ViewComponent
    private EntityPicker<Order> orderField;
    @ViewComponent
    private EntityPicker<Client> clientField;
    @ViewComponent
    private TypedTextField<BigDecimal> vatField;

    private boolean preventUnsavedChanges = true;
    @ViewComponent
    private Switch vatIncludedField;
    @ViewComponent
    private JmixBigDecimalField quantityField;
    @ViewComponent
    private TypedTextField<BigDecimal> totalField;
    @ViewComponent
    private TypedTextField<BigDecimal> discountField;

    @Override
    protected void preventUnsavedChanges(BeforeCloseEvent event) {
        if (preventUnsavedChanges) {
            super.preventUnsavedChanges(event);
        }
    }

    @Subscribe
    private void onInitEntity(final InitEntityEvent<OrderItem> event) {
        preventUnsavedChanges = false;

        OrderItem entity = event.getEntity();
        initializeDefaultValues(entity);
        initializePreservedState(entity);
    }

    @Subscribe
    private void onBeforeShow(final BeforeShowEvent event) {
        processPreservedState();

        OrderItem item = getEditedEntity();
        Order order = item.getOrder();

        boolean hasOrder = order != null;
        orderField.setReadOnly(hasOrder);
        clientField.setReadOnly(hasOrder);

        updateFields(item);
    }

    @Subscribe
    private void onAfterClose(final AfterCloseEvent event) {
        if (event.closedWith(StandardOutcome.SAVE)) {
            preservedState.clear();
        }
    }

    @Install(to = "orderItemDl", target = Target.DATA_LOADER, subject = "loadFromRepositoryDelegate")
    private Optional<OrderItem> loadDelegate(UUID id, FetchPlan fetchPlan) {
        return itemRepository.findById(id, fetchPlan);
    }

    @Install(target = Target.DATA_CONTEXT)
    private Set<Object> saveDelegate(SaveContext saveContext) {
        return Set.of(itemRepository.save(getEditedEntity()));
    }

    @Subscribe(id = "orderItemDc", target = Target.DATA_CONTAINER)
    private void onOrderItemDcItemPropertyChange(final ItemPropertyChangeEvent<OrderItem> event) {
        OrderItem item = event.getItem();
        recalculatePricing(item);
        updateFields(item);
    }

    @Subscribe(id = "plusQuantityButton", subject = "clickListener")
    private void onPlusQuantityButtonClick(final ClickEvent<JmixButton> event) {
        changeQuantityFieldValue(BigDecimal.ONE, true);
    }

    @Subscribe(id = "minusQuantityButton", subject = "clickListener")
    private void onMinusQuantityButtonClick(final ClickEvent<JmixButton> event) {
        changeQuantityFieldValue(BigDecimal.ONE, false);
    }

    @Install(to = "discountField", subject = "validator")
    private void discountFieldValidator(final BigDecimal value) {
        OrderItem item = getEditedEntity();

        BigDecimal quantity = item.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new ValidationException("Discount cannot be greater than total price");
        }

        BigDecimal total = calculateGrossPrice(item).multiply(quantity);
        if (value != null && value.compareTo(total) > 0) {
            throw new ValidationException("Discount cannot be greater than total price");
        }
    }

    private void initializeDefaultValues(OrderItem orderItem) {
        orderItem.setQuantity(BigDecimal.ONE);
        orderItem.setNetPrice(calculateNetPrice(orderItem));
        orderItem.setVat(calculateVat(orderItem, getDefaultVatPercent()));
        recalculatePricing(orderItem);
    }

    private BigDecimal getDefaultVatPercent() {
        return crmSettingsService.getDefaultVatPercent();
    }

    private void initializePreservedState(OrderItem entity) {
        if (preservedState.isEmpty()) {
            preservedState.setOrderItem(entity);
        }
    }

    private void processPreservedState() {
        OrderItem editedEntity = getEditedEntity();
        if (!entityStates.isNew(editedEntity)) {
            return;
        }

        OrderItem orderItemFromState = preservedState.getOrderItem();
        if (orderItemFromState == null) {
            return;
        }

        boolean isSameOrderItem = Objects.equals(editedEntity, orderItemFromState);
        boolean hasSameOrder = Objects.equals(editedEntity.getOrder(), preservedState.getOrder());
        if (!isSameOrderItem && hasSameOrder) {
            setEntityToEdit(orderItemFromState);
        }
    }

    private void updateFields(OrderItem item) {
        updateVatFields(item);
        updateDiscountField(item);
        updateTotalField(item);
    }

    private void updateVatFields(OrderItem item) {
        vatIncludedField.setValue(item.getVatIncluded());
        vatField.setHelperText("VAT, % = " + calculateVatPercent(item));
        vatIncludedField.executeValidators();
    }

    private void updateDiscountField(OrderItem item) {
        discountField.setTypedValue(item.getDiscount());
        discountField.executeValidators();
    }

    private void updateTotalField(OrderItem item) {
        totalField.setTypedValue(item.getTotal());
    }

    private void changeQuantityFieldValue(BigDecimal step, boolean increase) {
        BigDecimal currentValue = quantityField.getOptionalValue().orElse(BigDecimal.ONE);
        if (increase) {
            quantityField.setValue(currentValue.add(step));
        } else if (currentValue.compareTo(BigDecimal.ONE) > 0) {
            quantityField.setValue(currentValue.subtract(step));
        }
    }
}