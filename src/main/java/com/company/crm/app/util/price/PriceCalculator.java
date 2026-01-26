package com.company.crm.app.util.price;

import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public final class PriceCalculator {

    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    public static void recalculatePricing(OrderItem item) {
        item.setNetPrice(calculateNetPrice(item));
        item.setGrossPrice(calculateGrossPrice(item));
    }

    public static BigDecimal calculateNetPrice(OrderItem item) {
        BigDecimal unitPrice = item.getUnitPrice();
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return unitPrice.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateGrossPrice(OrderItem item) {
        BigDecimal net = calculateNetPrice(item);
        BigDecimal vatRate = zeroIfNull(item.getVatAmount());

        if (!Boolean.TRUE.equals(item.getVatIncluded()) || vatRate.compareTo(BigDecimal.ZERO) <= 0) {
            return net;
        }

        BigDecimal vatFactor = vatRate
                .divide(BigDecimal.valueOf(100), 6, DEFAULT_ROUNDING);

        return net
                .multiply(BigDecimal.ONE.add(vatFactor))
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateTotal(OrderItem item) {
        return zeroIfNull(item.getGrossPrice())
                .multiply(zeroIfNull(item.getQuantity()))
                .subtract(zeroIfNull(item.getDiscount()))
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateNetTotal(OrderItem item) {
        return zeroIfNull(item.getNetPrice())
                .multiply(zeroIfNull(item.getQuantity()))
                .subtract(zeroIfNull(item.getDiscount()))
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Subtotal = sum of net totals after discounts.
     */
    public static BigDecimal calculateSubtotal(Order order) {
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return orderItems.stream()
                .map(PriceCalculator::calculateNetTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Total = sum of gross totals.
     * Also must equal Subtotal + VAT.
     */
    public static BigDecimal calculateTotal(Order order) {
        BigDecimal total = order.getItemsTotal();
        BigDecimal discountValue = order.getDiscountValue();
        BigDecimal discountPercent = order.getDiscountPercent();

        if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(total.multiply(discountPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        } else if (discountValue.compareTo(BigDecimal.ZERO) > 0) {
            total = total.subtract(discountValue);
        }

        return total.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * VAT = sum of VAT for all items.
     */
    public static BigDecimal calculateVat(Order order) {
        List<OrderItem> orderItems = order.getOrderItems();
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return orderItems.stream()
                .map(PriceCalculator::calculateVatTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }


    public static BigDecimal calculateVat(OrderItem item) {
        return zeroIfNull(zeroIfNull(item.getGrossPrice()))
                .subtract(zeroIfNull(item.getNetPrice()))
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }


    public static BigDecimal calculateVatTotal(OrderItem item) {
        return calculateVat(item).multiply(zeroIfNull(item.getQuantity()));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    private PriceCalculator() {
    }
}

