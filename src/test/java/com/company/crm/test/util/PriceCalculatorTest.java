package com.company.crm.test.util;

import com.company.crm.app.util.price.PriceCalculator;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderDiscountType;
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
        assertThat(order.getSubTotal()).isEqualByComparingTo("306");
        assertThat(order.getVat()).isEqualByComparingTo("63");

        order.setDiscountPercent(BigDecimal.ZERO);
        order.setDiscountValue(new BigDecimal("25"));
        assertThat(order.getTotal()).isEqualByComparingTo("385");
        assertThat(order.getSubTotal()).isEqualByComparingTo("319.27");
        assertThat(order.getVat()).isEqualByComparingTo("65.73");
    }

    @Test
    void emptyOrderHasZeroSummary() {
        Order order = new Order();
        assertThat(order.getSubTotal()).isEqualByComparingTo("0");
        assertThat(order.getVat()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("0");
    }

    @Test
    void amountDiscountPreservesCentsDespiteTheRoundedPercentage() {
        Order order = order(item("1000", "1", "0", "0"));
        order.setDiscountValue(new BigDecimal("0.05"));
        order.setDiscountPercent(new BigDecimal("0.01"));
        order.setDiscountType(OrderDiscountType.AMOUNT);

        assertSummary(order, "999.95", "0", "999.95");
    }

    @Test
    void percentageDiscountIgnoresAnAmountFromAnotherMode() {
        Order order = order(item("100", "1", "20", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountValue(new BigDecimal("99"));
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "90", "18", "108");

        order.setDiscountPercent(BigDecimal.ZERO);

        assertSummary(order, "100", "20", "120");
    }

    @Test
    void legacyDiscountUsesThePercentageBeforeTheAmount() {
        Order order = order(item("100", "1", "20", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountValue(new BigDecimal("25"));

        assertThat(order.getDiscountType()).isNull();
        assertThat(PriceCalculator.resolveDiscountType(order)).isEqualTo(OrderDiscountType.PERCENT);
        assertSummary(order, "90", "18", "108");

        order.setDiscountPercent(BigDecimal.ZERO);

        assertThat(order.getDiscountType()).isNull();
        assertThat(PriceCalculator.resolveDiscountType(order)).isEqualTo(OrderDiscountType.AMOUNT);
        assertSummary(order, "79.17", "15.83", "95");
    }

    @Test
    void amountDiscountRemainsFixedWhenItemsChange() {
        OrderItem first = item("100", "1", "0", "0");
        Order order = order(first);
        order.setDiscountValue(new BigDecimal("25"));
        order.setDiscountPercent(new BigDecimal("25"));
        order.setDiscountType(OrderDiscountType.AMOUNT);
        assertSummary(order, "75", "0", "75");

        order.setOrderItems(List.of(first, item("100", "1", "0", "0")));

        assertSummary(order, "175", "0", "175");
    }

    @Test
    void roundedSubtotalAndVatAddUpToTheRoundedTotal() {
        Order order = order(item("0.04", "1", "0.01", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "0.03", "0.01", "0.04");
    }

    @Test
    void fullAmountAndPercentageDiscountsHaveZeroSummary() {
        Order order = order(item("100", "1", "20", "0"));
        order.setDiscountValue(new BigDecimal("120"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountType(OrderDiscountType.AMOUNT);
        assertSummary(order, "0", "0", "0");

        order.setDiscountPercent(new BigDecimal("100"));
        order.setDiscountValue(BigDecimal.ZERO);
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "0", "0", "0");
    }

    @Test
    void zeroPricedItemsHaveZeroSummaryWithAPercentageDiscount() {
        Order order = order(item("0", "1", "0", "0"));
        order.setDiscountPercent(new BigDecimal("10"));
        order.setDiscountType(OrderDiscountType.PERCENT);

        assertSummary(order, "0", "0", "0");
    }

    private void assertSummary(Order order, String subtotal, String vat, String total) {
        assertThat(order.getSubTotal()).isEqualByComparingTo(subtotal);
        assertThat(order.getVat()).isEqualByComparingTo(vat);
        assertThat(order.getTotal()).isEqualByComparingTo(total);
        assertThat(order.getSubTotal().add(order.getVat())).isEqualByComparingTo(order.getTotal());
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
