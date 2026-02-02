package com.company.crm.test.order;

import com.company.crm.AbstractUiTest;
import com.company.crm.model.catalog.category.Category;
import com.company.crm.model.catalog.item.CategoryItem;
import com.company.crm.model.catalog.item.UomType;
import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.view.order.OrderDetailView;
import com.company.crm.view.order.OrderListView;
import com.company.crm.view.order.item.OrderItemDetailView;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OrderViewsTest extends AbstractUiTest {

    @Test
    void opensOrderListView() {
        var view = viewTestSupport.navigateTo(OrderListView.class);
        assertThat(view).isInstanceOf(OrderListView.class);
    }

    @Test
    void opensOrderDetailView() {
        var view = viewTestSupport.navigateToNewEntityDetail(Order.class, OrderDetailView.class);
        assertThat(view).isInstanceOf(OrderDetailView.class);
    }

    @Test
    void opensOrderItemDetailView() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.NEW);

        Category category = dataManager.create(Category.class);
        category.setName("Category");
        category.setCode("CAT-1");
        saveWithoutReload(category);

        CategoryItem categoryItem = dataManager.create(CategoryItem.class);
        categoryItem.setName("Item");
        categoryItem.setCode("ITEM-1");
        categoryItem.setCategory(category);
        categoryItem.setPrice(new BigDecimal("100"));
        categoryItem.setUom(UomType.PIECES);
        saveWithoutReload(categoryItem);

        OrderItem orderItem = dataManager.create(OrderItem.class);
        orderItem.setOrder(order);
        orderItem.setCategoryItem(categoryItem);
        orderItem.setQuantity(new BigDecimal("1"));
        orderItem.setNetPrice(new BigDecimal("100"));
        orderItem.setGrossPrice(new BigDecimal("120"));
        orderItem.setVat(new BigDecimal("20"));
        saveWithoutReload(orderItem);

        var view = viewTestSupport.navigateToDetailView(OrderItem.class, orderItem, OrderItemDetailView.class);
        assertThat(view).isInstanceOf(OrderItemDetailView.class);
    }
}
