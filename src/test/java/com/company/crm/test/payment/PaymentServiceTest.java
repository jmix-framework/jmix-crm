package com.company.crm.test.payment;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.finance.PaymentService;
import com.company.crm.model.client.Client;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.order.OrderStatus;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTest extends AbstractServiceTest<PaymentService> {

    @Test
    void paymentsTotalSum_filtersByOrder() {
        Client client = entities.client();
        Order order1 = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        Order order2 = entities.order(client, LocalDate.now(), OrderStatus.DONE);

        Invoice invoice1 = entities.invoice(client, order1);
        Invoice invoice2 = entities.invoice(client, order2);

        savePayment(invoice1, new BigDecimal("50"));
        savePayment(invoice1, new BigDecimal("10"));
        savePayment(invoice2, new BigDecimal("20"));

        assertThat(service.getPaymentsTotalSum(order1)).isEqualByComparingTo("60");
        assertThat(service.getPaymentsTotalSum()).isEqualByComparingTo("80");
    }

    @Test
    void paymentsTotalsByClients_returnsTotalsWithLimit() {
        Client client1 = entities.client("Client One");
        Client client2 = entities.client("Client Two");

        Order order1 = entities.order(client1, LocalDate.now(), OrderStatus.DONE);
        Order order2 = entities.order(client2, LocalDate.now(), OrderStatus.DONE);

        Invoice invoice1 = entities.invoice(client1, order1);
        Invoice invoice2 = entities.invoice(client2, order2);

        savePayment(invoice1, new BigDecimal("100"));
        savePayment(invoice2, new BigDecimal("50"));

        var totals = service.getPaymentsTotalsByClients(1);

        assertThat(totals).hasSize(1);
        assertThat(totals.values().iterator().next()).isEqualByComparingTo("100");
    }

    @Test
    void biggestPayments_returnsHighestAmounts() {
        Client client = entities.client();
        Order order = entities.order(client, LocalDate.now(), OrderStatus.DONE);
        Invoice invoice = entities.invoice(client, order);

        savePayment(invoice, new BigDecimal("10"));
        savePayment(invoice, new BigDecimal("30"));
        savePayment(invoice, new BigDecimal("20"));

        var payments = service.getBiggestPayments(2);

        assertThat(payments).hasSize(2);
        assertThat(payments.getFirst().getAmount()).isEqualByComparingTo("30");
        assertThat(payments.get(1).getAmount()).isEqualByComparingTo("20");
    }

    private Payment savePayment(Invoice invoice, BigDecimal amount) {
        Payment payment = dataManager.create(Payment.class);
        payment.setInvoice(invoice);
        payment.setDate(LocalDate.now());
        payment.setAmount(amount);
        return saveWithoutReload(payment);
    }
}
