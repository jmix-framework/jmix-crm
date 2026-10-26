package com.company.crm.test.order;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.util.UniqueValues;
import com.company.crm.view.order.OrderDetailView;
import com.company.crm.view.order.item.OrderItemDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import io.jmix.flowui.OpenedDialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.JmixBigDecimalField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.testassist.UiTestUtils;
import io.jmix.flowui.view.ViewControllerUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSummaryViewTest extends AbstractUiTest {

    @Test
    void subtotalAndVatIncludeItemsAddedAfterTheFirstOne() {
        CategoryItem first = product("First summary product", "100");
        CategoryItem second = product("Second summary product", "50");
        OrderDetailView view = viewTestSupport.navigateToNewEntityDetail(Order.class, OrderDetailView.class);
        view.getEditedEntity().setClient(entities.client());

        addItem(view, first, "20");
        assertField(view, "subtotalField", "100");
        assertField(view, "vatField", "20");

        addItem(view, second, "5");

        assertThat(view.getEditedEntity().getOrderItems()).hasSize(2);
        assertThat(view.getEditedEntity().getSubTotal()).isEqualByComparingTo("150");
        assertThat(view.getEditedEntity().getVat()).isEqualByComparingTo("25");
        assertField(view, "subtotalField", "150");
        assertField(view, "vatField", "25");
    }

    @Test
    void quantitiesAreAppliedOnceWhenAddingMultipleItems() {
        OrderDetailView view = viewTestSupport.navigateToNewEntityDetail(Order.class, OrderDetailView.class);
        view.getEditedEntity().setClient(entities.client());

        addItem(view, product("Two summary products", "100"), "2", "0");
        addItem(view, product("Three summary products", "50"), "3", "0");

        assertThat(view.getEditedEntity().getOrderItems()).hasSize(2);
        assertField(view, "subtotalField", "350");
        assertField(view, "vatField", "0");
        assertField(view, "totalField", "350");
    }

    @Test
    void discountCannotExceedTheGrossAmountOfTheLine() {
        OrderDetailView view = viewTestSupport.navigateToNewEntityDetail(Order.class, OrderDetailView.class);
        view.getEditedEntity().setClient(entities.client());
        OrderItemDetailView itemView = openItem(view, product("Discount limit product", "100"), "2", "0");
        TypedTextField<BigDecimal> discount = viewTestSupport.getComponent(itemView, "discountField");

        try {
            discount.setTypedValue(new BigDecimal("201"));
        } catch (ValidationException ignored) {
            // The bound field also validates eagerly when its value changes.
        }

        assertThat(itemView.getEditedEntity().getGrossPrice()).isEqualByComparingTo("200");
        assertThat(UiTestUtils.validateView(itemView).getAll())
                .extracting(ValidationErrors.Item::getComponent)
                .contains(discount);

        discount.setTypedValue(new BigDecimal("200"));

        assertThat(UiTestUtils.validateView(itemView).isEmpty()).isTrue();
    }

    @Test
    void addingAnItemUpdatesTheStoredOrderTotal() {
        Order order = savedOrder("120");
        savedItem(order, product("Existing summary product", "100"), "20");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);
        assertField(view, "totalField", "120");

        addItem(view, product("Added summary product", "50"), "5");

        assertThat(view.getEditedEntity().getOrderItems()).hasSize(2);
        assertThat(view.getEditedEntity().getTotal()).isEqualByComparingTo("175");
        assertField(view, "totalField", "175");
    }

    @Test
    void addingAnItemUpdatesTheAmountOfAPercentageDiscount() {
        Order order = savedOrder("90");
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountValue(new BigDecimal("10"));
        saveWithoutReload(order);
        savedItem(order, product("Percentage discount product", "100"), "0");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        addItem(view, product("Added percentage discount product", "100"), "0");

        JmixBigDecimalField percent = viewTestSupport.getComponent(view, "discountPercentField");
        assertField(view, "subtotalField", "180");
        assertThat(percent.getValue()).isEqualByComparingTo("10");
        assertField(view, "discountValueField", "20");
        assertField(view, "totalField", "180");
    }

    @Test
    void addingAnItemUpdatesThePercentageOfAFixedDiscount() {
        Order order = savedOrder("75");
        order.setDiscountPercent(BigDecimal.ZERO);
        order.setDiscountValue(new BigDecimal("25"));
        saveWithoutReload(order);
        savedItem(order, product("Fixed discount product", "100"), "0");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        addItem(view, product("Added fixed discount product", "100"), "0");

        JmixBigDecimalField percent = viewTestSupport.getComponent(view, "discountPercentField");
        assertField(view, "subtotalField", "175");
        assertThat(percent.getValue()).isEqualByComparingTo("12.50");
        assertField(view, "discountValueField", "25");
        assertField(view, "totalField", "175");
    }

    @Test
    void percentageDiscountUpdatesSubtotalAndVat() {
        Order order = savedOrder("120");
        savedItem(order, product("Percentage summary product", "100"), "20");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        setClientValue(view, "discountPercentField", "10");

        assertSummary(view, "90", "18", "108");
        assertField(view, "discountValueField", "12");
    }

    @Test
    void fixedDiscountUsesTheLatestClientValueAndCanBeCleared() {
        Order order = savedOrder("120");
        savedItem(order, product("Fixed summary product", "100"), "20");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        setClientValue(view, "discountValueField", "12");
        assertSummary(view, "90", "18", "108");

        setClientValue(view, "discountValueField", "24");
        assertSummary(view, "80", "16", "96");
        assertField(view, "discountValueField", "24");

        setClientValue(view, "discountValueField", "");
        assertSummary(view, "100", "20", "120");
    }

    @Test
    void clearingOrZeroingPercentageDiscountRestoresTheSummary() {
        Order order = savedOrder("120");
        savedItem(order, product("Cleared discount product", "100"), "20");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        setClientValue(view, "discountPercentField", "10");
        setClientValue(view, "discountPercentField", "");
        assertSummary(view, "100", "20", "120");
        assertField(view, "discountValueField", "0");

        setClientValue(view, "discountPercentField", "10");
        setClientValue(view, "discountPercentField", "0");
        assertSummary(view, "100", "20", "120");
        assertField(view, "discountValueField", "0");
    }

    @Test
    void fixedDiscountKeepsItsExactAmountAfterReopeningAndAddingAnItem() {
        Order order = savedOrder("1000");
        savedItem(order, product("Exact fixed discount product", "1000"), "0");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        setClientValue(view, "discountValueField", "0.05");
        assertSummary(view, "999.95", "0", "999.95");

        setClientValue(view, "discountValueField", "0.06");
        assertSummary(view, "999.94", "0", "999.94");

        view = saveAndReopen(view);
        assertField(view, "discountValueField", "0.06");
        assertSummary(view, "999.94", "0", "999.94");

        addItem(view, product("Added exact fixed discount product", "1000"), "0");
        assertField(view, "discountValueField", "0.06");
        assertSummary(view, "1999.94", "0", "1999.94");
    }

    @Test
    void percentageDiscountKeepsItsRateAfterReopeningAndAddingAnItem() {
        Order order = savedOrder("120");
        savedItem(order, product("Saved percentage discount product", "100"), "20");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);

        setClientValue(view, "discountPercentField", "10");
        view = saveAndReopen(view);
        assertSummary(view, "90", "18", "108");

        addItem(view, product("Added saved percentage discount product", "50"), "10");

        JmixBigDecimalField percent = viewTestSupport.getComponent(view, "discountPercentField");
        assertThat(percent.getValue()).isEqualByComparingTo("10");
        assertField(view, "discountValueField", "18");
        assertSummary(view, "135", "27", "162");
    }

    @Test
    void removingAnItemValidatesAFixedDiscountAgainstTheRemainingAmount() {
        Order order = savedOrder("120");
        OrderItem removed = savedItem(order, product("Removed discounted product", "100"), "0");
        savedItem(order, product("Remaining discounted product", "20"), "0");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);
        setClientValue(view, "discountValueField", "25");
        TypedTextField<BigDecimal> discount = viewTestSupport.getComponent(view, "discountValueField");
        CollectionContainer<OrderItem> items = ViewControllerUtils.getViewData(view).getContainer("orderItemsDc");
        OrderItem loadedItem = items.getItems().stream()
                .filter(item -> item.getId().equals(removed.getId()))
                .findFirst().orElseThrow();

        items.getMutableItems().remove(loadedItem);

        assertField(view, "discountValueField", "25");
        assertThat(UiTestUtils.validateView(view).getAll())
                .extracting(ValidationErrors.Item::getComponent)
                .contains(discount);

        setClientValue(view, "discountValueField", "15");

        assertSummary(view, "5", "0", "5");
        assertThat(UiTestUtils.validateView(view).getAll()).isEmpty();
    }

    @Test
    void editingAnItemRefreshesTheOrderSummary() {
        Order order = savedOrder("175");
        OrderItem first = savedItem(order, product("Edited summary product", "100"), "20");
        savedItem(order, product("Other summary product", "50"), "5");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);
        DataGrid<OrderItem> grid = viewTestSupport.getComponent(view, "orderItemsGrid");
        grid.select(first);
        viewTestSupport.click(view, "editButton");
        OrderItemDetailView itemView = (OrderItemDetailView) openedDialogWindows().getCurrentDialog().orElse(null);
        assertThat(itemView).isNotNull();

        setVat(itemView, "30");
        viewTestSupport.click(itemView, "saveAndCloseButton");

        assertThat(openedDialogWindows().getDialogs()).doesNotContain(itemView);
        assertThat(view.getEditedEntity().getVat()).isEqualByComparingTo("35");
        assertField(view, "subtotalField", "150");
        assertField(view, "vatField", "35");
        assertField(view, "totalField", "185");
    }

    @Test
    void removingAnItemRefreshesTheOrderSummary() {
        Order order = savedOrder("175");
        savedItem(order, product("Remaining summary product", "100"), "20");
        OrderItem removed = savedItem(order, product("Removed summary product", "50"), "5");
        OrderDetailView view = viewTestSupport.navigateToDetailView(order, OrderDetailView.class);
        CollectionContainer<OrderItem> items = ViewControllerUtils.getViewData(view).getContainer("orderItemsDc");
        OrderItem loadedItem = items.getItems().stream()
                .filter(item -> item.getId().equals(removed.getId()))
                .findFirst().orElseThrow();

        items.getMutableItems().remove(loadedItem);

        assertThat(view.getEditedEntity().getOrderItems()).hasSize(1);
        assertThat(view.getEditedEntity().getSubTotal()).isEqualByComparingTo("100");
        assertThat(view.getEditedEntity().getVat()).isEqualByComparingTo("20");
        assertField(view, "subtotalField", "100");
        assertField(view, "vatField", "20");
        assertField(view, "totalField", "120");
    }

    private CategoryItem product(String name, String price) {
        return entities.categoryItem(name, UniqueValues.string(), null, new BigDecimal(price), UomType.PIECES);
    }

    private Order savedOrder(String total) {
        return entities.order(entities.client(), LocalDate.now(), OrderStatus.NEW, new BigDecimal(total));
    }

    private OrderItem savedItem(Order order, CategoryItem product, String vat) {
        return entities.createAndSaveEntity(OrderItem.class, item -> {
            item.setOrder(order);
            item.setCategoryItem(product);
            item.setQuantity(BigDecimal.ONE);
            item.setNetPrice(product.getPrice());
            item.setVat(new BigDecimal(vat));
            item.setGrossPrice(product.getPrice().add(new BigDecimal(vat)));
        });
    }

    private void addItem(OrderDetailView view, CategoryItem product, String vat) {
        addItem(view, product, "1", vat);
    }

    private void addItem(OrderDetailView view, CategoryItem product, String quantity, String vat) {
        OrderItemDetailView itemView = openItem(view, product, quantity, vat);
        viewTestSupport.click(itemView, "saveAndCloseButton");
        assertThat(openedDialogWindows().getDialogs()).doesNotContain(itemView);
    }

    private OrderItemDetailView openItem(OrderDetailView view, CategoryItem product, String quantity, String vat) {
        viewTestSupport.click(view, "createButton");
        OrderItemDetailView itemView = (OrderItemDetailView) openedDialogWindows().getCurrentDialog().orElse(null);
        assertThat(itemView).isNotNull();
        EntityComboBox<CategoryItem> productField = viewTestSupport.getComponent(itemView, "categoryItemField");
        productField.setValue(product);
        JmixBigDecimalField quantityField = viewTestSupport.getComponent(itemView, "quantityField");
        quantityField.setValue(new BigDecimal(quantity));
        setVat(itemView, vat);
        return itemView;
    }

    private void setVat(OrderItemDetailView view, String vat) {
        TypedTextField<BigDecimal> field = viewTestSupport.getComponent(view, "vatField");
        field.setTypedValue(new BigDecimal(vat));
    }

    private OpenedDialogWindows openedDialogWindows() {
        return applicationContext.getBean(OpenedDialogWindows.class);
    }

    private void setClientValue(OrderDetailView view, String id, String value) {
        Component field = viewTestSupport.getComponent(view, id);
        field.getElement().getNode().getFeature(ElementPropertyMap.class).updateFromClient("value", value);
    }

    private OrderDetailView saveAndReopen(OrderDetailView view) {
        Order order = view.getEditedEntity();
        assertThat(UiTestUtils.validateView(view).getAll()).isEmpty();
        viewTestSupport.click(view, "saveAndCloseButton");
        Order saved = dataManager.load(Order.class).id(order.getId()).one();
        assertThat(saved.getDiscountType()).isEqualTo(order.getDiscountType());
        return viewTestSupport.navigateToDetailView(saved, OrderDetailView.class);
    }

    private void assertSummary(OrderDetailView view, String subtotal, String vat, String total) {
        assertField(view, "subtotalField", subtotal);
        assertField(view, "vatField", vat);
        assertField(view, "totalField", total);
    }

    private void assertField(OrderDetailView view, String id, String expected) {
        TypedTextField<BigDecimal> field = viewTestSupport.getComponent(view, id);
        assertThat(field.getTypedValue()).as(id).isEqualByComparingTo(expected);
    }
}
