package com.company.crm.tests.client;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.address.Address;
import com.company.crm.model.base.UuidEntity;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientServiceTest extends AbstractTest {

    @Autowired
    DataManager dataManager;

    @Autowired
    ClientService clientService;

    List<UuidEntity> toRemove = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (int i = toRemove.size() - 1; i >= 0; i--) {
            dataManager.remove(Id.of(toRemove.get(i)));
        }
        toRemove.clear();
    }

    @Test
    void ordersTotalSum_filtersBySingleStatus() {
        Client client = saveClient("sum-status-single");

        Order order1 = saveOrder(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal(10));
        dataManager.save(order1);

        Order order2 = saveOrder(client, LocalDate.now(), OrderStatus.NEW);
        order2.setTotal(new BigDecimal(100));
        dataManager.save(order2);

        BigDecimal total = clientService.getOrdersTotalSum(new OrderStatus[]{OrderStatus.DONE}, client);
        assertThat(total).isEqualByComparingTo("10");
    }

    @Test
    void ordersTotalSum_filtersByMultipleStatuses() {
        Client client = saveClient("sum-status-multi");

        Order order1 = saveOrder(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal(10));
        dataManager.save(order1);

        Order order2 = saveOrder(client, LocalDate.now(), OrderStatus.ACCEPTED);
        order2.setTotal(new BigDecimal(20));
        dataManager.save(order2);

        Order order3 = saveOrder(client, LocalDate.now(), OrderStatus.NEW);
        order3.setTotal(new BigDecimal(100));
        dataManager.save(order3);

        BigDecimal total = clientService.getOrdersTotalSum(new OrderStatus[]{OrderStatus.DONE, OrderStatus.ACCEPTED}, client);
        assertThat(total).isEqualByComparingTo("30");
    }

    @Test
    void ordersTotalSum_doesNotFilterWhenStatusesIsNull() {
        Client client = saveClient("sum-status-null");

        Order order1 = saveOrder(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal(10));
        dataManager.save(order1);

        Order order2 = saveOrder(client, LocalDate.now(), OrderStatus.NEW);
        order2.setTotal(new BigDecimal(100));
        dataManager.save(order2);

        BigDecimal total = clientService.getOrdersTotalSum(null, client);
        assertThat(total).isEqualByComparingTo("110");
    }

    @Test
    void salesCycleLength_isCalculatedFromOrderDateToLastPaymentDate_andAveraged() {
        Client client = saveClient("sc-client");
        Client otherClient = saveClient("sc-other");

        Order order1 = saveOrder(client, LocalDate.of(2025, 1, 1), OrderStatus.DONE);
        Order order2 = saveOrder(client, LocalDate.of(2025, 1, 3), OrderStatus.DONE);

        // order without payments must not affect the result
        saveOrder(client, LocalDate.of(2025, 1, 20), OrderStatus.DONE);

        // order with null date should be ignored
        Order nullDateOrder = saveOrder(client, null, OrderStatus.DONE);

        // other client must not affect client-filtered result
        Order otherClientOrder = saveOrder(otherClient, LocalDate.of(2025, 1, 1), OrderStatus.DONE);

        // order1: last payment on 2025-01-10 => 9 days
        Invoice inv1 = saveInvoice(client, order1);
        savePayment(inv1, LocalDate.of(2025, 1, 5));
        savePayment(inv1, LocalDate.of(2025, 1, 10));

        // order2: last payment on 2025-01-04 => 1 day
        Invoice inv2 = saveInvoice(client, order2);
        savePayment(inv2, LocalDate.of(2025, 1, 4));

        // null-date order has payments, but should still be ignored due to null order date
        Invoice invNull = saveInvoice(client, nullDateOrder);
        savePayment(invNull, LocalDate.of(2025, 1, 7));

        // other client order with payments
        Invoice invOther = saveInvoice(otherClient, otherClientOrder);
        savePayment(invOther, LocalDate.of(2025, 1, 2));

        var avg = clientService.getSalesCycleLength(null, client);
        assertThat(avg).isEqualByComparingTo(5);

        var avgFiltered = clientService.getSalesCycleLength(
                LocalDateRange.from(LocalDate.of(2025, 1, 3), LocalDate.of(2025, 1, 3)),
                client
        );
        assertThat(avgFiltered).isEqualByComparingTo(1);
    }

    @Test
    void purchaseFrequency_countsOrdersInInclusivePeriod_forSpecificClient() {
        Client client = saveClient("pf-client");
        Client otherClient = saveClient("pf-other-client");

        saveOrder(client, LocalDate.of(2025, 1, 1), OrderStatus.DONE);   // in (lower bound)
        saveOrder(client, LocalDate.of(2025, 1, 10), OrderStatus.DONE);  // in (upper bound)
        saveOrder(client, LocalDate.of(2024, 12, 31), OrderStatus.DONE); // out
        saveOrder(client, null, OrderStatus.DONE);                       // null date must be excluded when period is set

        saveOrder(otherClient, LocalDate.of(2025, 1, 5), OrderStatus.DONE); // other client

        var dateRange = LocalDateRange.from(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 10));
        assertThat(clientService.getCompletedOrdersAmount(dateRange, client)).isEqualByComparingTo("2");
    }

    @Test
    void purchaseFrequency_countsOrdersAllTime_whenDateRangeIsNull() {
        Client client = saveClient("pf-open-interval");

        saveOrder(client, LocalDate.of(2025, 1, 1), OrderStatus.DONE);
        saveOrder(client, LocalDate.of(2025, 1, 10), OrderStatus.DONE);
        saveOrder(client, LocalDate.of(2025, 2, 1), OrderStatus.DONE);
        saveOrder(client, null, OrderStatus.DONE);

        // with dateRange = null, all DONE orders are counted (including orders with null date)
        assertThat(clientService.getCompletedOrdersAmount(null, client)).isEqualByComparingTo("4");
    }

    private Client saveClient(String namePrefix) {
        Client client = dataManager.create(Client.class);
        client.setName(namePrefix + "-" + System.currentTimeMillis());
        client.setAddress(new Address());
        Client saved = dataManager.save(client);
        toRemove.add(saved);
        return saved;
    }

    private Order saveOrder(Client client, LocalDate date, OrderStatus status) {
        Order order = dataManager.create(Order.class);
        order.setClient(client);
        order.setDate(date);
        order.setStatus(status);

        Order saved = dataManager.save(order);
        toRemove.add(saved);
        return saved;
    }

    private Invoice saveInvoice(Client client, Order order) {
        Invoice invoice = dataManager.create(Invoice.class);
        invoice.setClient(client);
        invoice.setOrder(order);

        Invoice saved = dataManager.save(invoice);
        toRemove.add(saved);
        return saved;
    }

    private Payment savePayment(Invoice invoice, LocalDate date) {
        Payment payment = dataManager.create(Payment.class);
        payment.setInvoice(invoice);
        payment.setDate(date);
        payment.setAmount(BigDecimal.TEN);

        Payment saved = dataManager.save(payment);
        toRemove.add(saved);
        return saved;
    }
}
