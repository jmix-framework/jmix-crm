package com.company.crm.test.client;

import com.company.crm.AbstractTest;
import com.company.crm.app.service.client.ClientService;
import com.company.crm.app.util.date.range.LocalDateRange;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ClientServiceTest extends AbstractTest {

    @Autowired
    private ClientService clientService;

    @Test
    void ordersTotalSum_filtersBySingleStatus() {
        Client client = entities.client();

        Order order1 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal(10));
        saveWithoutReload(order1);

        Order order2 = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        order2.setTotal(new BigDecimal(100));
        saveWithoutReload(order2);

        BigDecimal total = clientService.getOrdersTotalSum(new OrderStatus[]{OrderStatus.DONE}, client);
        assertThat(total).isEqualByComparingTo("10");
    }

    @Test
    void ordersTotalSum_filtersByMultipleStatuses() {
        Client client = entities.client();

        Order order1 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal(10));
        saveWithoutReload(order1);

        Order order2 = entities.order(client, LocalDate.now(), OrderStatus.ACCEPTED);
        order2.setTotal(new BigDecimal(20));
        saveWithoutReload(order2);

        Order order3 = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        order3.setTotal(new BigDecimal(100));
        saveWithoutReload(order3);

        BigDecimal total = clientService.getOrdersTotalSum(new OrderStatus[]{OrderStatus.DONE, OrderStatus.ACCEPTED}, client);
        assertThat(total).isEqualByComparingTo("30");
    }

    @Test
    void ordersTotalSum_doesNotFilterWhenStatusesIsNull() {
        Client client = entities.client();

        Order order1 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        order1.setTotal(new BigDecimal(10));
        saveWithoutReload(order1);

        Order order2 = entities.order(client, LocalDate.now(), OrderStatus.NEW);
        order2.setTotal(new BigDecimal(100));
        saveWithoutReload(order2);

        BigDecimal total = clientService.getOrdersTotalSum(null, client);
        assertThat(total).isEqualByComparingTo("110");
    }

    @Test
    void salesCycleLength_isCalculatedFromOrderDateToLastPaymentDate_andAveraged() {
        Client client = entities.client();
        Client otherClient = entities.client();

        Order order1 = entities.order(client, LocalDate.of(2026, 1, 1), OrderStatus.DONE);
        Order order2 = entities.order(client, LocalDate.of(2026, 1, 3), OrderStatus.DONE);

        // order without payments must not affect the result
        entities.order(client, LocalDate.of(2026, 1, 20), OrderStatus.DONE);

        // order with null date should be ignored
        Order nullDateOrder = entities.order(client, null, OrderStatus.DONE);

        // other client must not affect client-filtered result
        Order otherClientOrder = entities.order(otherClient, LocalDate.of(2026, 1, 1), OrderStatus.DONE);

        // order1: last payment on 2026-01-10 => 9 days
        Invoice inv1 = entities.invoice(client, order1);
        entities.payment(inv1, LocalDate.of(2026, 1, 5));
        entities.payment(inv1, LocalDate.of(2026, 1, 10));

        // order2: last payment on 2026-01-04 => 1 day
        Invoice inv2 = entities.invoice(client, order2);
        entities.payment(inv2, LocalDate.of(2026, 1, 4));

        // null-date order has payments, but should still be ignored due to null order date
        Invoice invNull = entities.invoice(client, nullDateOrder);
        entities.payment(invNull, LocalDate.of(2026, 1, 7));

        // other client order with payments
        Invoice invOther = entities.invoice(otherClient, otherClientOrder);
        entities.payment(invOther, LocalDate.of(2026, 1, 2));

        var avg = clientService.getSalesCycleLength(null, client);
        assertThat(avg).isEqualByComparingTo(5);

        var avgFiltered = clientService.getSalesCycleLength(
                LocalDateRange.from(LocalDate.of(2026, 1, 3), LocalDate.of(2026, 1, 3)),
                client
        );
        assertThat(avgFiltered).isEqualByComparingTo(1);
    }

    @Test
    void purchaseFrequency_countsOrdersInInclusivePeriod_forSpecificClient() {
        Client client = entities.client();
        Client otherClient = entities.client();

        entities.order(client, LocalDate.of(2026, 1, 1), OrderStatus.DONE);   // in (lower bound)
        entities.order(client, LocalDate.of(2026, 1, 10), OrderStatus.DONE);  // in (upper bound)
        entities.order(client, LocalDate.of(2024, 12, 31), OrderStatus.DONE); // out
        entities.order(client, null, OrderStatus.DONE);                       // null date must be excluded when period is set

        entities.order(otherClient, LocalDate.of(2026, 1, 5), OrderStatus.DONE); // other client

        var dateRange = LocalDateRange.from(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10));
        assertThat(clientService.getCompletedOrdersAmount(dateRange, client)).isEqualTo(2L);
    }

    @Test
    void purchaseFrequency_countsOrdersAllTime_whenDateRangeIsNull() {
        Client client = entities.client();

        entities.order(client, LocalDate.of(2026, 1, 1), OrderStatus.DONE);
        entities.order(client, LocalDate.of(2026, 1, 10), OrderStatus.DONE);
        entities.order(client, LocalDate.of(2026, 2, 1), OrderStatus.DONE);
        entities.order(client, null, OrderStatus.DONE);

        // with dateRange = null, all DONE orders are counted (including orders with null date)
        assertThat(clientService.getCompletedOrdersAmount(null, client)).isEqualTo(4L);
    }
    @Test
    void getInvoicesTotalSum_calculatesCorrectSum() {
        Client client = entities.client("Sum Client");
        entities.createAndSaveEntity(Invoice.class, i -> {
            i.setClient(client);
            i.setTotal(new BigDecimal("100"));
        });
        entities.createAndSaveEntity(Invoice.class, i -> {
            i.setClient(client);
            i.setTotal(new BigDecimal("50"));
        });

        assertThat(clientService.getInvoicesTotalSum(client)).isEqualByComparingTo("150");
    }

    @Test
    void getOutstandingBalance_calculatesDifference() {
        Client client = entities.client("Balance Client");
        entities.createAndSaveEntity(Invoice.class, i -> {
            i.setClient(client);
            i.setTotal(new BigDecimal("100"));
        });
        entities.createAndSaveEntity(Payment.class, (Payment p) -> {
            p.setInvoice(entities.createAndSaveEntity(Invoice.class, (Invoice i) -> i.setClient(client)));
            p.setAmount(new BigDecimal("40"));
        });

        // Outstanding balance = 100 (first invoice) + 0 (second invoice total is null/0 by default) - 40 (payment)
        assertThat(clientService.getOutstandingBalance(client)).isEqualByComparingTo("60");
    }

    @Test
    void getBestBuyers_returnsSortedMap() {
        Client c1 = entities.client("Client 1");
        Client c2 = entities.client("Client 2");

        entities.createAndSaveEntity(Order.class, o -> { o.setClient(c1); o.setTotal(new BigDecimal("100")); });
        entities.createAndSaveEntity(Order.class, o -> { o.setClient(c2); o.setTotal(new BigDecimal("200")); });

        var bestBuyers = clientService.getBestBuyers(10);
        assertThat(bestBuyers.keySet()).containsExactly(c2, c1);
    }
}
