package com.company.crm.app.service.analytics;

import com.company.crm.model.order.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain policy: derives category allocation shares from order lines and applies exposure allocation.
 */
@Component("crm_CategoryAllocationPolicy")
public class CategoryAllocationPolicy {

    public static final String UNASSIGNED = "UNASSIGNED";

    public Map<String, CategoryAllocationShare> defineAllocationShares(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return Map.of(UNASSIGNED, new CategoryAllocationShare(UNASSIGNED, UNASSIGNED, BigDecimal.ONE));
        }

        Map<String, CategoryBasis> basisByCategory = new LinkedHashMap<>();
        for (OrderItem item : orderItems) {
            String code = UNASSIGNED;
            String name = UNASSIGNED;
            if (item.getCategoryItem() != null && item.getCategoryItem().getCategory() != null) {
                code = item.getCategoryItem().getCategory().getCode();
                name = item.getCategoryItem().getCategory().getName();
            }

            String categoryName = name;
            CategoryBasis basis = basisByCategory.computeIfAbsent(code, key -> new CategoryBasis(categoryName));
            basis.amount = basis.amount.add(safeMoney(item.getTotal()).max(BigDecimal.ZERO));
        }

        BigDecimal sumBasis = basisByCategory.values().stream()
                .map(CategoryBasis::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sumBasis.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of(UNASSIGNED, new CategoryAllocationShare(UNASSIGNED, UNASSIGNED, BigDecimal.ONE));
        }

        LinkedHashMap<String, CategoryAllocationShare> shares = new LinkedHashMap<>();
        for (Map.Entry<String, CategoryBasis> entry : basisByCategory.entrySet()) {
            BigDecimal percentage = entry.getValue().amount()
                    .divide(sumBasis, 8, RoundingMode.HALF_UP);
            shares.put(entry.getKey(), new CategoryAllocationShare(entry.getKey(), entry.getValue().name(), percentage));
        }
        return shares;
    }

    public LinkedHashMap<String, BigDecimal> allocateExposure(BigDecimal invoiceTotal, Map<String, CategoryAllocationShare> shares) {
        LinkedHashMap<String, BigDecimal> allocations = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(shares.keySet());
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            BigDecimal allocation;
            if (i == keys.size() - 1) {
                allocation = invoiceTotal.subtract(running);
            } else {
                allocation = invoiceTotal.multiply(shares.get(key).percentage())
                        .setScale(2, RoundingMode.HALF_UP);
                running = running.add(allocation);
            }
            allocations.put(key, allocation);
        }
        return allocations;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static class CategoryBasis {
        private final String name;
        private BigDecimal amount = BigDecimal.ZERO;

        private CategoryBasis(String name) {
            this.name = name;
        }

        private String name() {
            return name;
        }

        private BigDecimal amount() {
            return amount;
        }
    }

    public record CategoryAllocationShare(String code, String name, BigDecimal percentage) {
    }
}
