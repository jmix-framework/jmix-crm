package com.company.crm.app.service.order;

import com.company.crm.model.client.Client;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderItem;
import com.company.crm.model.order.OrderItemRepository;
import com.company.crm.model.order.OrderRepository;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlans;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final FetchPlans fetchPlans;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, FetchPlans fetchPlans) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.fetchPlans = fetchPlans;
    }

    public List<Order> loadOrders(LocalDate startDate, LocalDate endDate) {
        return orderRepository.listByQuery("e.date >= ?1 and e.date <= ?2", startDate, endDate);
    }

    public Map<OrderItem, BigDecimal> getBestOrderItems(int amount) {
        if (amount <= 0) {
            return new HashMap<>();
        }

        Map<OrderItem, BigDecimal> orderItem2Amount = new HashMap<>();

        loadOrderItems().forEach(orderItem ->
                orderItem2Amount.merge(orderItem,
                        orderItem2Amount.getOrDefault(orderItem, orderItem.getQuantity()), BigDecimal::add));

        return orderItem2Amount.entrySet().stream()
                .sorted(comparingByValue(Comparator.reverseOrder()))
                .limit(amount)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private List<OrderItem> loadOrderItems() {
        return orderItemRepository.findAll(
                fetchPlans.builder(OrderItem.class)
                        .addFetchPlan(FetchPlan.BASE)
                        .add("categoryItem", FetchPlan.BASE)
                        .build());
    }
}
