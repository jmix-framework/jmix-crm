package com.company.crm.app.service.order;

import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderRepository;
import com.company.crm.model.order.OrderStatus;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    public BigDecimal getPaid(Order order) {
        return paymentService.getPaymentsTotalSum(order);
    }

    public BigDecimal getLeftOverSum(Order order) {
        BigDecimal result = order.getTotal().subtract(getPaid(order));
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return result;
    }

    public Map<OrderStatus, BigDecimal> getOrdersAmountByStatus() {
        Map<OrderStatus, BigDecimal> result = new HashMap<>();
        List<KeyValueEntity> list = orderRepository.fluentValuesLoader(
                        "select e.status as status, count(e) as amount " +
                                "from Order_ e " +
                                "group by e.status")
                .properties("status", "amount")
                .list();
        list.forEach(e -> {
            OrderStatus status = OrderStatus.fromId(e.getValue("status"));
            BigDecimal amount = BigDecimal.valueOf(e.getValue("amount"));
            BigDecimal currentAmount = result.getOrDefault(status, BigDecimal.ZERO);
            result.put(status, currentAmount.add(amount));
        });
        return result;
    }

    public Map<OrderStatus, List<Order>> getOrdersByStatus() {
        Map<OrderStatus, List<Order>> ordersByStatus = new HashMap<>();
        orderRepository.findAll().forEach(order ->
                ordersByStatus.getOrDefault(order.getStatus(), new ArrayList<>()).add(order));
        return ordersByStatus;
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
