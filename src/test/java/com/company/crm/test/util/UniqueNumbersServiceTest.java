package com.company.crm.test.util;

import com.company.crm.AbstractServiceTest;
import com.company.crm.app.service.util.UniqueNumbersService;
import com.company.crm.model.invoice.Invoice;
import com.company.crm.model.order.Order;
import com.company.crm.model.payment.Payment;
import org.junit.jupiter.api.Test;

import static com.company.crm.app.service.util.UniqueNumbersService.INVOICE_NUMBER_PREFIX;
import static com.company.crm.app.service.util.UniqueNumbersService.ORDER_NUMBER_PREFIX;
import static com.company.crm.app.service.util.UniqueNumbersService.PAYMENT_NUMBER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

class UniqueNumbersServiceTest extends AbstractServiceTest<UniqueNumbersService> {

    @Test
    void nextNumber_usesPrefixAndIncrements() {
        String first = service.getNextOrderNumber();
        String second = service.getNextOrderNumber();

        assertThat(first).startsWith(ORDER_NUMBER_PREFIX);
        assertThat(second).startsWith(ORDER_NUMBER_PREFIX);
        assertThat(extractNumber(second)).isGreaterThan(extractNumber(first));
    }

    @Test
    void getNextNumber_supportsKnownEntities() {
        String order = service.getNextNumber(Order.class);
        String invoice = service.getNextNumber(Invoice.class);
        String payment = service.getNextNumber(Payment.class);

        assertThat(order).startsWith(ORDER_NUMBER_PREFIX);
        assertThat(invoice).startsWith(INVOICE_NUMBER_PREFIX);
        assertThat(payment).startsWith(PAYMENT_NUMBER_PREFIX);
    }

    @Test
    void getNextPurchaseOrderNumber_usesPrefix() {
        String poNumber = service.getNextPurchaseOrderNumber();
        assertThat(poNumber).startsWith(UniqueNumbersService.PURCHASE_ORDER_NUMBER_PREFIX);
    }

    private long extractNumber(String value) {
        return Long.parseLong(value.substring(value.indexOf('-') + 1));
    }
}
