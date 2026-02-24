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
        // given
        Category firstCategory = new Category();
        firstCategory.setCode("CAT1");
        firstCategory.setName("Category 1");
        CategoryItem firstCategoryItem = new CategoryItem();
        firstCategoryItem.setCategory(firstCategory);
        OrderItem firstItem = new OrderItem();
        firstItem.setCategoryItem(firstCategoryItem);
        firstItem.setGrossPrice(new BigDecimal("500.00"));
        firstItem.setNetPrice(new BigDecimal("500.00"));
        firstItem.setQuantity(BigDecimal.ONE);

        Category secondCategory = new Category();
        secondCategory.setCode("CAT2");
        secondCategory.setName("Category 2");
        CategoryItem secondCategoryItem = new CategoryItem();
        secondCategoryItem.setCategory(secondCategory);
        OrderItem secondItem = new OrderItem();
        secondItem.setCategoryItem(secondCategoryItem);
        secondItem.setGrossPrice(new BigDecimal("500.00"));
        secondItem.setNetPrice(new BigDecimal("500.00"));
        secondItem.setQuantity(BigDecimal.ONE);

        List<OrderItem> items = List.of(
                firstItem,
                secondItem
        );

        // when
        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = policy.defineAllocationShares(items);

        // then
        assertThat(shares).hasSize(2);
        assertThat(shares.get("CAT1").percentage()).isEqualByComparingTo("0.50000000");
        assertThat(shares.get("CAT2").percentage()).isEqualByComparingTo("0.50000000");
        assertThat(shares.values().stream()
                .map(CategoryAllocationPolicy.CategoryAllocationShare::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("1.00000000");
    }

    @Test
    void defineAllocationShares_emptyItems_returnsUnassigned() {
        // when
        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = policy.defineAllocationShares(List.of());

        // then
        assertThat(shares).hasSize(1);
        assertThat(shares).containsKey(CategoryAllocationPolicy.UNASSIGNED);
        assertThat(shares.get(CategoryAllocationPolicy.UNASSIGNED).percentage()).isEqualByComparingTo("1");
    }

    @Test
    void allocateExposure_appliesRoundingResidualToLastCategory() {
        // given
        Map<String, CategoryAllocationPolicy.CategoryAllocationShare> shares = new LinkedHashMap<>();
        shares.put("A", new CategoryAllocationPolicy.CategoryAllocationShare("A", "A", new BigDecimal("0.33333333")));
        shares.put("B", new CategoryAllocationPolicy.CategoryAllocationShare("B", "B", new BigDecimal("0.33333333")));
        shares.put("C", new CategoryAllocationPolicy.CategoryAllocationShare("C", "C", new BigDecimal("0.33333334")));

        // when
        LinkedHashMap<String, BigDecimal> allocations = policy.allocateExposure(new BigDecimal("100.00"), shares);

        // then
        assertThat(allocations.get("A")).isEqualByComparingTo("33.33");
        assertThat(allocations.get("B")).isEqualByComparingTo("33.33");
        assertThat(allocations.get("C")).isEqualByComparingTo("33.34");
        assertThat(allocations.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

}
