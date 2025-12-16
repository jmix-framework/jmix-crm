package com.company.crm.app.util.price;

import com.company.crm.model.order.Order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PriceCalculator {

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

        return total;
    }

    private PriceCalculator() {
    }
}
