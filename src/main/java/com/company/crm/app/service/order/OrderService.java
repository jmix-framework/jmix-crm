package com.company.crm.app.service.order;

import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getOrders(LocalDateRange dateRange) {
        return orderRepository.listByQuery("e.date >= ?1 and e.date <= ?2",
                dateRange.startDate(), dateRange.endDate());
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
}
