package com.company.crm.app.service.order;

import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> loadOrders(LocalDate startDate, LocalDate endDate) {
        return orderRepository.listByQuery("e.date >= ?1 and e.date <= ?2", startDate, endDate);
    }
}
