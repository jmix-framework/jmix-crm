package com.company.crm.test.order;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.order.OrderService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OrderServiceTest extends AbstractServiceTest<OrderService> {

    @Test
    void paidAndLeftOverSum_usePaymentsTotal() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order.setTotal(new BigDecimal("100"));
        saveWithoutReload(order);

        Invoice invoice = entities.invoice(client, order);

        Payment payment1 = dataManager.create(Payment.class);
        payment1.setInvoice(invoice);
        payment1.setDate(LocalDate.now());
        payment1.setAmount(new BigDecimal("30"));
        saveWithoutReload(payment1);

        Payment payment2 = dataManager.create(Payment.class);
        payment2.setInvoice(invoice);
        payment2.setDate(LocalDate.now());
        payment2.setAmount(new BigDecimal("30"));
        saveWithoutReload(payment2);

        assertThat(service.getPaid(order)).isEqualByComparingTo("60");
        assertThat(service.getLeftOverSum(order)).isEqualByComparingTo("40");

        Payment payment3 = dataManager.create(Payment.class);
        payment3.setInvoice(invoice);
        payment3.setDate(LocalDate.now());
        payment3.setAmount(new BigDecimal("50"));
        saveWithoutReload(payment3);

        assertThat(service.getPaid(order)).isEqualByComparingTo("110");
        assertThat(service.getLeftOverSum(order)).isEqualByComparingTo("0");
    }

    @Test
    void ordersAmountByStatus_groupsResults() {
        Client client = entities.client();
        saveWithoutReload(entities.order(client, LocalDate.now(), OrderStatus.NEW));
        saveWithoutReload(entities.order(client, LocalDate.now(), OrderStatus.DONE));
        saveWithoutReload(entities.order(client, LocalDate.now(), OrderStatus.DONE));

        var result = service.getOrdersAmountByStatus();

        assertThat(result.get(OrderStatus.NEW)).isEqualByComparingTo("1");
        assertThat(result.get(OrderStatus.DONE)).isEqualByComparingTo("2");
    }

    @Test
    void ordersByStatus_groupsOrders() {
        Client client = entities.client();
        Order order1 = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        Order order2 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        Order order3 = entities.order(client, LocalDate.now(), OrderStatus.DONE);

        var result = service.getOrdersByStatus();

        assertThat(result.get(OrderStatus.NEW)).containsExactly(order1);
        assertThat(result.get(OrderStatus.DONE)).containsExactlyInAnyOrder(order2, order3);
    }

    @Test
    void ordersTotalAndAverage_returnAggregates() {
        Client client = entities.client();
        Order order1 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal("10"));
        saveWithoutReload(order1);

        Order order2 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order2.setTotal(new BigDecimal("20"));
        saveWithoutReload(order2);

        Order order3 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order3.setTotal(new BigDecimal("30"));
        saveWithoutReload(order3);

        assertThat(service.getOrdersTotalSum()).isEqualByComparingTo("60");
        assertThat(service.getOrdersAverageBill()).isEqualByComparingTo("20");
    }

    @Test
    void orders_filtersByDateRange() {
        Client client = entities.client();
        saveWithoutReload(entities.order(client, LocalDate.of(2026, 1, 1), OrderStatus.NEW));
        saveWithoutReload(entities.order(client, LocalDate.of(2026, 1, 10), OrderStatus.NEW));
        saveWithoutReload(entities.order(client, LocalDate.of(2026, 2, 1), OrderStatus.NEW));

        var range = LocalDateRange.from(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertThat(service.getOrders(range)).hasSize(2);
    }
}
