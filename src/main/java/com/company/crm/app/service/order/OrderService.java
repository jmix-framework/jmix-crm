package com.company.crm.app.service.order;

import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getOrders(LocalDate startDate, LocalDate endDate) {
        return orderRepository.listByQuery("e.date >= ?1 and e.date <= ?2", startDate, endDate);
    }
    
    /**
     * Calculates and returns the total sum of all orders.
     *
     * @return the total value of all orders as a {@code BigDecimal}.
     */
    public BigDecimal getOrdersTotalSum() {
        return orderRepository.fluentValueLoader(
                        "select sum(e.total) as total " +
                                "from Order_ e " +
                                "order by total desc", BigDecimal.class)
                .optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Calculates and returns the average bill for all orders.
     *
     * @return the average bill value of all orders as a {@code BigDecimal}.
     */
    public BigDecimal getOrdersAverageBill() {
        return orderRepository.fluentValueLoader(
                        "select avg(e.total) as average " +
                                "from Order_ e", BigDecimal.class)
                .optional().orElse(BigDecimal.ZERO);
    }

    /**
     * Retrieves the top order items based on their quantity, optionally limited to a specific number of items.
     *
     * @param amount an optional integer specifying the maximum number of items to return.
     *               If null or not greater than 0, no limit is applied.
     * @return a map where the keys are the top {@code OrderItem} objects and the values are their respective total quantities as {@code BigDecimal}.
     */
    public Map<OrderItem, BigDecimal> getBestOrderItems(@Nullable Integer amount) {
        Map<OrderItem, BigDecimal> orderItem2Amount = new LinkedHashMap<>();

        orderRepository.fluentValuesLoader(
                        "select e.categoryItem as categoryItem, sum(e.quantity) as total " +
                                "from OrderItem e " +
                                "group by e.categoryItem " +
                                "order by total desc " +
                                ((amount != null && amount > 0) ? ("limit " + amount) : ""))
                .properties("categoryItem", "total")
                .list().forEach(keyValue -> {
                    OrderItem orderItem = keyValue.getValue("categoryItem");
                    BigDecimal total = keyValue.getValue("total");
                    orderItem2Amount.merge(orderItem, total, BigDecimal::add);
                });

        return orderItem2Amount;
    }

}
