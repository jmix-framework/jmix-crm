package com.company.crm.test.util;

import com.company.crm.app.util.price.PriceCalculator;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JmixIncorrectCreateEntity") // Pure calculations do not require a persistence context.
class PriceCalculatorTest {

    @Test
    void sumsAllLineAmountsWithoutMultiplyingQuantityTwice() {
        OrderItem first = item("100", "2", "0", "0");
        OrderItem second = item("50", "3", "0", "0");
        Order order = order(first, second);

        assertThat(first.getNetPrice()).isEqualByComparingTo("200");
        assertThat(second.getNetPrice()).isEqualByComparingTo("150");
        assertThat(order.getSubTotal()).isEqualByComparingTo("350");
        assertThat(order.getVat()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("350");
    }

    @Test
    void sumsLineVatAndDiscountsOnce() {
        OrderItem first = item("100", "2", "40", "10");
        OrderItem second = item("50", "3", "30", "0");
        Order order = order(first, second);

        assertThat(first.getTotal()).isEqualByComparingTo("230");
        assertThat(second.getTotal()).isEqualByComparingTo("180");
        assertThat(order.getSubTotal()).isEqualByComparingTo("340");
        assertThat(order.getVat()).isEqualByComparingTo("70");
        assertThat(order.getTotal()).isEqualByComparingTo("410");
    }

    @Test
    void appliesOrderDiscountAfterSummingLineAmounts() {
        Order order = order(item("100", "2", "40", "10"), item("50", "3", "30", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        assertThat(order.getTotal()).isEqualByComparingTo("369");

        order.setDiscountPercent(BigDecimal.ZERO);
        order.setDiscountValue(new BigDecimal("25"));
        assertThat(order.getTotal()).isEqualByComparingTo("385");
    }

    @Test
    void emptyOrderHasZeroSummary() {
        Order order = new Order();
        assertThat(order.getSubTotal()).isEqualByComparingTo("0");
        assertThat(order.getVat()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("0");
    }

    private OrderItem item(String unitPrice, String quantity, String vat, String discount) {
        CategoryItem product = new CategoryItem();
        product.setPrice(new BigDecimal(unitPrice));
        OrderItem item = new OrderItem();
        item.setCategoryItem(product);
        item.setQuantity(new BigDecimal(quantity));
        item.setVat(new BigDecimal(vat));
        item.setDiscount(new BigDecimal(discount));
        PriceCalculator.recalculatePricing(item);
        return item;
    }

    private Order order(OrderItem... items) {
        Order order = new Order();
        order.setOrderItems(List.of(items));
        return order;
    }
}
