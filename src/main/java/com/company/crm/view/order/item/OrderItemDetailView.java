package com.company.crm.view.order.item;

import com.company.crm.app.util.constant.CrmConstants;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderItemRepository;
import com.company.crm.model.settings.CrmSettings;
import com.company.crm.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.router.Route;
import io.jmix.appsettings.AppSettings;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.SaveContext;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.JmixBigDecimalField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
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

import static com.company.crm.app.util.price.PriceCalculator.calculateNetPrice;
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
    private AppSettings appSettings;

    @ViewComponent
    private EntityPicker<Order> orderField;
    @ViewComponent
    private EntityPicker<Client> clientField;
    @ViewComponent
    private JmixBigDecimalField vatAmountField;

    private boolean preventUnsavedChanges = true;

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
        Order order = getEditedEntity().getOrder();
        boolean hasOrder = order != null;
        orderField.setReadOnly(hasOrder);
        clientField.setReadOnly(hasOrder);
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

    @Subscribe("vatIncludedField")
    private void onVatIncludedFieldComponentValueChange(final ComponentValueChangeEvent<Switch, Boolean> event) {
        vatAmountField.setEnabled(event.getValue());
    }

    @Subscribe("categoryItemField")
    private void onCategoryItemFieldComponentValueChange(final ComponentValueChangeEvent<EntityComboBox<CategoryItem>, CategoryItem> event) {
        recalculatePricing(getEditedEntity());
    }

    private void initializeDefaultValues(OrderItem orderItem) {
        orderItem.setVatIncluded(true);
        orderItem.setVatAmount(getDefaultVatPercent());
        orderItem.setQuantity(BigDecimal.ONE);
        orderItem.setNetPrice(calculateNetPrice(orderItem));
    }

    private BigDecimal getDefaultVatPercent() {
        return appSettings.load(CrmSettings.class).getDefaultVatPercent();
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
}