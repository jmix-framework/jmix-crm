package com.company.crm.app.service.analytics;

import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.order.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryAllocationPolicyTest {

    private final CategoryAllocationPolicy policy = new CategoryAllocationPolicy();

    @Test
    void defineAllocationShares_twoCategories() {
        List<OrderItem> items = List.of(
                orderItem("CAT1", "Category 1", "500.00"),
                orderItem("CAT2", "Category 2", "500.00")
        );

        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = policy.defineAllocationShares(items);

        assertThat(shares).hasSize(2);
        assertThat(shares.get("CAT1").percentage()).isEqualByComparingTo("0.50000000");
        assertThat(shares.get("CAT2").percentage()).isEqualByComparingTo("0.50000000");
    }

    @Test
    void defineAllocationShares_emptyItems_returnsUnassigned() {
        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = policy.defineAllocationShares(List.of());

        assertThat(shares).hasSize(1);
        assertThat(shares).containsKey(CategoryAllocationPolicy.UNASSIGNED);
        assertThat(shares.get(CategoryAllocationPolicy.UNASSIGNED).percentage()).isEqualByComparingTo("1");
    }

    @Test
    void allocateExposure_appliesRoundingResidualToLastCategory() {
        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = new LinkedHashMap<>();
        shares.put("A", new CategoryAllocationPolicy.CategoryAllocationShare("A", "A", new BigDecimal("0.33333333")));
        shares.put("B", new CategoryAllocationPolicy.CategoryAllocationShare("B", "B", new BigDecimal("0.33333333")));
        shares.put("C", new CategoryAllocationPolicy.CategoryAllocationShare("C", "C", new BigDecimal("0.33333334")));

        LinkedHashMap<String, BigDecimal> allocations = policy.allocateExposure(new BigDecimal("100.00"), shares);

        assertThat(allocations.get("A")).isEqualByComparingTo("33.33");
        assertThat(allocations.get("B")).isEqualByComparingTo("33.33");
        assertThat(allocations.get("C")).isEqualByComparingTo("33.34");
        assertThat(allocations.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

    private OrderItem orderItem(String categoryCode, String categoryName, String total) {
        Category category = new Category();
        category.setCode(categoryCode);
        category.setName(categoryName);

        CategoryItem categoryItem = new CategoryItem();
        categoryItem.setCategory(category);

        OrderItem orderItem = new OrderItem();
        orderItem.setCategoryItem(categoryItem);
        orderItem.setGrossPrice(new BigDecimal(total));
        orderItem.setNetPrice(new BigDecimal(total));
        orderItem.setQuantity(BigDecimal.ONE);
        return orderItem;
    }
}
