package com.company.crm.view.order.item;

import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.vaadin.flow.spring.annotation.UIScope;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@UIScope
@Component
public class OrderItemPreservedState {
    private OrderItem orderItem;

    @Nullable
    public OrderItem getOrderItem() {
        return orderItem;
    }

    @Nullable
    public Order getOrder() {
        OrderItem item = getOrderItem();
        if (item == null) {
            return null;
        } else {
            return item.getOrder();
        }
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public boolean isEmpty() {
        return orderItem == null;
    }

    public void clear() {
        orderItem = null;
    }
}
