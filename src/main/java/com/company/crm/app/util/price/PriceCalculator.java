package com.company.crm.app.util.price;

import com.company.crm.model.invoice.Invoice;
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

        BigDecimal quantity = item.getQuantity();
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return unitPrice.multiply(quantity).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateGrossPrice(OrderItem item) {
        BigDecimal netPrice = calculateNetPrice(item);
        BigDecimal vat = zeroIfNull(item.getVat());

        if (vat.compareTo(BigDecimal.ZERO) <= 0) {
            return netPrice;
        }

        return netPrice.add(vat).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
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

    public static BigDecimal calculateVat(OrderItem item, BigDecimal vatPercent) {
        return zeroIfNull(item.getNetPrice())
                .multiply(vatPercent)
                .divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateVatPercent(OrderItem item) {
        BigDecimal grossPrice = zeroIfNull(item.getGrossPrice());
        if (grossPrice == null || grossPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return grossPrice
                .subtract(zeroIfNull(item.getNetPrice()))
                .divide(grossPrice, DEFAULT_SCALE, DEFAULT_ROUNDING)
                .multiply(BigDecimal.valueOf(100))
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateVat(Invoice invoice, BigDecimal vatPercent) {
        if (vatPercent == null || vatPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return zeroIfNull(invoice.getSubtotal())
                .multiply(vatPercent)
                .divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static void calculateInvoiceFieldsFromOrder(Order order, Invoice invoice, BigDecimal vatPercent) {
        BigDecimal unbilledTotal = order.getTotal().subtract(order.getInvoiced());
        if (unbilledTotal.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setSubtotal(unbilledTotal);
            invoice.setVat(calculateVat(invoice, vatPercent));
            invoice.setTotal(calculateTotal(invoice));
        }
    }

    public static BigDecimal calculateVatTotal(OrderItem item) {
        return calculateVat(item).multiply(zeroIfNull(item.getQuantity()));
    }

    public static BigDecimal calculateTotal(Invoice invoice) {
        return zeroIfNull(invoice.getSubtotal()).add(zeroIfNull(invoice.getVat()));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }

    private PriceCalculator() {
    }
}

